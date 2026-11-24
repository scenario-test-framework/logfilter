// Package engine はログファイルのフィルタリング処理本体を提供します。
//
// タイムスタンプで始まる行を「ログイベント」の開始とみなし、
// 継続行 (スタックトレースなど) を含むイベント単位で
// TimeFilter / LogLevelFilter / StringContentFilter / RegexFilter (すべてAND) を適用します。
package engine

import (
	"bufio"
	"fmt"
	"io"
	"log"
	"os"
	"path/filepath"
	"regexp"
	"slices"
	"strings"
	"time"

	"golang.org/x/text/transform"

	"github.com/scenario-test-framework/logfilter/internal/charset"
	"github.com/scenario-test-framework/logfilter/internal/config"
	"github.com/scenario-test-framework/logfilter/internal/structured"
	"github.com/scenario-test-framework/logfilter/internal/timeparse"
)

// reportLineCount は進捗ログを出力する行数間隔。
const reportLineCount = 100000

// maxTimestampField はタイムスタンプ検出に利用する最大フィールド番号。
const maxTimestampField = 5

// Result はフィルタリング処理の実行結果。
type Result struct {
	// 入力ファイルの行数
	InputRows int64
	// 出力ファイルの行数
	OutputRows int64
}

// filters はパース済みのフィルタ設定。
type filters struct {
	cfg   *config.Config
	from  *time.Time
	to    *time.Time
	regex *regexp.Regexp
}

// eventHeader はイベントの先頭行から読み取った情報です。
type eventHeader struct {
	timestamp time.Time
	record    structured.Record
}

// Run は設定に従ってフィルタリングを実行します。
// 設定は事前に config.Validate 済みであることを前提とします。
func Run(cfg *config.Config) (Result, error) {
	var result Result

	// ----------------------------------------
	// フィルタ設定のパース
	// ----------------------------------------
	f := filters{cfg: cfg}
	if cfg.TimeFilterValueFrom != "" {
		t, err := timeparse.Parse(cfg.TimeFilterValueFrom)
		if err != nil {
			return result, fmt.Errorf("timeFilterValueFrom: %w", err)
		}
		f.from = &t
	}
	if cfg.TimeFilterValueTo != "" {
		t, err := timeparse.Parse(cfg.TimeFilterValueTo)
		if err != nil {
			return result, fmt.Errorf("timeFilterValueTo: %w", err)
		}
		f.to = &t
	}
	if f.from != nil && f.to != nil && f.from.After(*f.to) {
		return result, fmt.Errorf("TimeFilterのFromは、Toより前の日時を設定して下さい。 From=%s To=%s", cfg.TimeFilterValueFrom, cfg.TimeFilterValueTo)
	}
	if cfg.RegexFilterValue != "" {
		re, err := regexp.Compile(cfg.RegexFilterValue)
		if err != nil {
			return result, fmt.Errorf("正規表現をコンパイルできません。 設定値=%s: %w", cfg.RegexFilterValue, err)
		}
		f.regex = re
	}

	// ----------------------------------------
	// 文字コードの解決
	// ----------------------------------------
	inEnc, err := charset.Lookup(cfg.InputCharset)
	if err != nil {
		return result, err
	}
	outCharset := cfg.OutputCharset
	if outCharset == "" {
		// 未設定の場合、入力文字コードと同一値を利用
		outCharset = cfg.InputCharset
	}
	outEnc, err := charset.Lookup(outCharset)
	if err != nil {
		return result, err
	}

	// ----------------------------------------
	// 出力先の準備 (既存ファイル削除 + 親ディレクトリ作成)
	// ----------------------------------------
	if _, err := os.Stat(cfg.OutputFilePath); err == nil {
		if err := os.Remove(cfg.OutputFilePath); err != nil {
			return result, fmt.Errorf("ファイルを削除できません。 対象=%s: %w", cfg.OutputFilePath, err)
		}
	}
	outDir := filepath.Dir(cfg.OutputFilePath)
	if err := os.MkdirAll(outDir, 0o755); err != nil {
		return result, fmt.Errorf("ディレクトリを作成できません。 対象=%s: %w", outDir, err)
	}

	// ----------------------------------------
	// reader / writer の作成
	// ----------------------------------------
	inFile, err := os.Open(cfg.InputFilePath)
	if err != nil {
		return result, fmt.Errorf("ファイルを読み込めません。 対象=%s: %w", cfg.InputFilePath, err)
	}
	defer inFile.Close()
	reader := bufio.NewReader(transform.NewReader(inFile, inEnc.NewDecoder()))

	outFile, err := os.Create(cfg.OutputFilePath)
	if err != nil {
		return result, fmt.Errorf("ファイルに書き込めません。 対象=%s: %w", cfg.OutputFilePath, err)
	}
	transformWriter := transform.NewWriter(outFile, outEnc.NewEncoder())
	writer := bufio.NewWriter(transformWriter)

	// ----------------------------------------
	// フィルタリング
	// ----------------------------------------
	runErr := filterLines(reader, writer, &f, &result)

	// stream のクローズ (エラー時も必ず閉じる)
	if err := writer.Flush(); err != nil && runErr == nil {
		runErr = fmt.Errorf("ファイルに書き込めません。 対象=%s: %w", cfg.OutputFilePath, err)
	}
	if err := transformWriter.Close(); err != nil && runErr == nil {
		runErr = fmt.Errorf("ファイルに書き込めません。 対象=%s: %w", cfg.OutputFilePath, err)
	}
	if err := outFile.Close(); err != nil && runErr == nil {
		runErr = fmt.Errorf("ファイルに書き込めません。 対象=%s: %w", cfg.OutputFilePath, err)
	}

	return result, runErr
}

// filterLines は入力を行単位で読み込み、イベント単位でフィルタを適用して出力します。
func filterLines(reader *bufio.Reader, writer *bufio.Writer, f *filters, result *Result) error {
	var event []string
	var header eventHeader

	// イベントを評価し、マッチする場合に出力する
	flush := func() error {
		if len(event) == 0 {
			return nil
		}
		write, err := f.match(event, header)
		if err != nil {
			return err
		}
		if write {
			for _, l := range event {
				if _, err := writer.WriteString(l + "\n"); err != nil {
					return fmt.Errorf("ファイルに書き込めません。: %w", err)
				}
			}
			result.OutputRows += int64(len(event))
		}
		return nil
	}

	for {
		line, readErr := reader.ReadString('\n')
		if readErr != nil && readErr != io.EOF {
			return fmt.Errorf("%d 行目の読み込みでエラーが発生しました。: %w", result.InputRows+1, readErr)
		}
		if line == "" && readErr == io.EOF {
			break
		}
		line = strings.TrimSuffix(line, "\n")
		line = strings.TrimSuffix(line, "\r")
		result.InputRows++

		if result.InputRows%reportLineCount == 0 {
			log.Printf("INFO  %d rows finished.", result.InputRows)
		}

		next, starts, err := f.parseHeader(line, result.InputRows == 1)
		if err != nil {
			return fmt.Errorf("%d 行目の%w", result.InputRows, err)
		}
		if !starts && result.InputRows == 1 {
			return fmt.Errorf("ログイベントからタイムスタンプを検出できませんでした。対象文字列：%s", line)
		}

		if starts {
			// ログイベントが切り替わる行の場合、蓄積したイベントを評価して出力
			if err := flush(); err != nil {
				return err
			}
			event = event[:0]
			header = next
		}
		event = append(event, line)

		if readErr == io.EOF {
			break
		}
	}

	// 最後のログイベントを評価して出力
	return flush()
}

// parseHeader はイベントの先頭行を認識し、構造化ログでは解析済みの日時とフィールドを保持します。
// first は入力の 1 行目か否か。構造化ログでは 1 行目に限り、解析用の文字列から UTF-8 の BOM を除きます
// (出力には元の行を使う)。
func (f *filters) parseHeader(line string, first bool) (eventHeader, bool, error) {
	if f.cfg.LogFormat == "" || f.cfg.LogFormat == "text" {
		_, err := lineTimestamp(line)
		return eventHeader{}, err == nil, nil
	}
	if first {
		line = strings.TrimPrefix(line, "\ufeff")
	}
	record := structured.Parse(f.cfg.LogFormat, line)
	ts, found, err := record.Timestamp(f.cfg.TimestampKey, f.cfg.TimestampFormat)
	return eventHeader{timestamp: ts, record: record}, found, err
}

// match は指定されたフィルタを適用して、出力するべきイベントか否かを返します。
func (f *filters) match(event []string, header eventHeader) (bool, error) {
	eventValue := strings.Join(event, "\n") + "\n"

	// TimeFilter
	if f.from != nil || f.to != nil {
		ts := header.timestamp
		if header.record == nil {
			// テキスト形式では旧版と同様に、イベント全文から比較用の日時を取得する。
			var err error
			ts, err = lineTimestamp(eventValue)
			if err != nil {
				return false, fmt.Errorf("ログイベントからタイムスタンプを検出できませんでした。対象文字列：%s", event[0])
			}
		}
		switch {
		case f.from != nil && f.to == nil:
			// Fromのみ指定: From <= ts
			if ts.Before(*f.from) {
				return false, nil
			}
		case f.from == nil && f.to != nil:
			// Toのみ指定: ts < To
			if !ts.Before(*f.to) {
				return false, nil
			}
		default:
			if f.from.Equal(*f.to) {
				// From = To の場合、完全一致を確認
				if !ts.Equal(*f.from) {
					return false, nil
				}
			} else {
				// From < To の場合、From <= ts < To を確認
				if ts.Before(*f.from) || !ts.Before(*f.to) {
					return false, nil
				}
			}
		}
	}

	// LogLevelFilter
	if len(f.cfg.LogLevelFilterValueList) > 0 && !f.matchLevel(eventValue, header.record) {
		return false, nil
	}

	// StringContentFilter
	if f.cfg.StringContentFilterValue != "" && !strings.Contains(eventValue, f.cfg.StringContentFilterValue) {
		return false, nil
	}

	// RegexFilter
	if f.regex != nil && !f.regex.MatchString(eventValue) {
		return false, nil
	}

	return true, nil
}

// matchLevel は構造化フィールドを優先し、値が取得できない場合はイベント全文を検索します。
func (f *filters) matchLevel(eventValue string, record structured.Record) bool {
	levels := f.cfg.LogLevelFilterValueList
	if record != nil {
		if value, ok := record.Level(f.cfg.LogLevelKey); ok {
			return slices.ContainsFunc(levels, func(level string) bool { return strings.EqualFold(value, level) })
		}
	}
	for _, level := range levels {
		if strings.Contains(eventValue, " "+level+" ") || strings.Contains(eventValue, "["+level) {
			return true
		}
	}
	return false
}

// lineTimestamp は行頭 1〜5 フィールド (半角スペース区切り) から
// 「隣接2フィールド連結 → 単一フィールド」の順で日時変換を試行します。
func lineTimestamp(line string) (time.Time, error) {
	parts := strings.Split(line, " ")
	// フィールドは「後続に区切り文字がある」場合のみ取得できる (旧実装互換)
	field := func(num int) string {
		if num <= len(parts)-1 {
			return parts[num-1]
		}
		return ""
	}

	for cur := 1; cur <= maxTimestampField; cur++ {
		curValue := field(cur)
		if curValue == "" {
			// フィールドが取得できない場合、エラー
			return time.Time{}, fmt.Errorf("ログイベントからタイムスタンプを検出できませんでした。対象文字列：%s", line)
		}

		// 現在フィールド + 次フィールド で変換
		if cur < maxTimestampField {
			if t, err := timeparse.Parse(curValue + " " + field(cur+1)); err == nil {
				return t, nil
			}
		}

		// 現在フィールドのみで変換
		if t, err := timeparse.Parse(curValue); err == nil {
			return t, nil
		}
	}

	return time.Time{}, fmt.Errorf("ログイベントからタイムスタンプを検出できませんでした。対象文字列：%s", line)
}
