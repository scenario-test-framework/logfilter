package engine

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
	"time"

	"golang.org/x/text/encoding/japanese"

	"github.com/scenario-test-framework/logfilter/internal/config"
)

// TestMain はタイムゾーン依存のテスト (apache access log の +0900 と
// ローカルタイムのフィルタ値の比較) を安定させるため、JSTに固定します。
func TestMain(m *testing.M) {
	loc, err := time.LoadLocation("Asia/Tokyo")
	if err != nil {
		panic(err)
	}
	time.Local = loc
	os.Exit(m.Run())
}

func testdata(name string) string {
	return filepath.Join("..", "..", "testdata", name)
}

func baseConfig(t *testing.T, inputFile string) *config.Config {
	t.Helper()
	return &config.Config{
		InputFilePath:  testdata(inputFile),
		InputCharset:   "UTF-8",
		OutputFilePath: filepath.Join(t.TempDir(), "out.log"),
	}
}

func readOutput(t *testing.T, path string) string {
	t.Helper()
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("出力ファイルを読み込めません: %v", err)
	}
	return string(data)
}

// 旧Javaテスト (LogFilterServiceTest#test_execute) の移植。
func TestRun_ApacheAccessLog_TimeFilterFrom(t *testing.T) {
	cfg := baseConfig(t, "apache_access.log")
	cfg.TimeFilterValueFrom = "2000-10-10 13:57:00.000"

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.InputRows != 5 {
		t.Errorf("InputRows = %d, want 5", result.InputRows)
	}
	if result.OutputRows != 3 {
		t.Errorf("OutputRows = %d, want 3", result.OutputRows)
	}

	out := readOutput(t, cfg.OutputFilePath)
	if strings.Contains(out, "13:55:36") || strings.Contains(out, "13:56:36") {
		t.Errorf("From より前のイベントが出力されている:\n%s", out)
	}
	for _, want := range []string{"13:57:36", "13:58:36", "13:59:36"} {
		if !strings.Contains(out, want) {
			t.Errorf("出力に %s のイベントが含まれない:\n%s", want, out)
		}
	}
}

// 旧Javaテスト (LogFilterServiceTest#test_execute_unsupported) の移植。
func TestRun_UnsupportedFormat(t *testing.T) {
	cfg := baseConfig(t, "unsupported.log")

	_, err := Run(cfg)
	if err == nil || !strings.Contains(err.Error(), "タイムスタンプを検出できませんでした") {
		t.Errorf("サポート外フォーマットのエラーが発生すること, got: %v", err)
	}
}

func TestRun_LogLevelFilter_MultiLineEvent(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.LogLevelFilterValueList = []string{"ERROR"}

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.InputRows != 59 {
		t.Errorf("InputRows = %d, want 59", result.InputRows)
	}
	// ERRORイベントは2件。それぞれヘッダ行 + 継続行4行 + 空行 = 6行。
	if result.OutputRows != 12 {
		t.Errorf("OutputRows = %d, want 12", result.OutputRows)
	}

	out := readOutput(t, cfg.OutputFilePath)
	// マルチラインの継続行が出力されること
	if !strings.Contains(out, "設定値=unmatched date format") {
		t.Errorf("継続行が出力されない:\n%s", out)
	}
	// ERROR以外のイベントが出力されないこと
	if strings.Contains(out, "DEBUG") || strings.Contains(out, "WARN") {
		t.Errorf("ERROR以外のイベントが出力されている:\n%s", out)
	}
}

func TestRun_LogLevelFilter_BracketStyle(t *testing.T) {
	dir := t.TempDir()
	input := filepath.Join(dir, "bracket.log")
	content := "2016-11-06 12:00:00.000 [INFO ] information\n" +
		"2016-11-06 12:00:01.000 [ERROR] some error\n"
	if err := os.WriteFile(input, []byte(content), 0o644); err != nil {
		t.Fatal(err)
	}

	cfg := &config.Config{
		InputFilePath:           input,
		InputCharset:            "UTF-8",
		OutputFilePath:          filepath.Join(dir, "out.log"),
		LogLevelFilterValueList: []string{"ERROR"},
	}
	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.OutputRows != 1 {
		t.Errorf("OutputRows = %d, want 1", result.OutputRows)
	}
	if out := readOutput(t, cfg.OutputFilePath); !strings.Contains(out, "[ERROR]") {
		t.Errorf("[ERROR]イベントが出力されない:\n%s", out)
	}
}

func TestRun_TimeFilter_FromTo(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.TimeFilterValueFrom = "2016-11-06 12:17:53.982"
	cfg.TimeFilterValueTo = "2016-11-06 12:17:53.984"

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	// 12:17:53.982 のイベント2件 (ERRORイベント6行 + DEBUGイベント1行)。
	// To (12:17:53.984) は範囲に含まない。
	if result.OutputRows != 7 {
		t.Errorf("OutputRows = %d, want 7", result.OutputRows)
	}
	out := readOutput(t, cfg.OutputFilePath)
	if strings.Contains(out, "12:17:53.984") {
		t.Errorf("To以降のイベントが出力されている:\n%s", out)
	}
}

func TestRun_TimeFilter_FromEqualsTo(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.TimeFilterValueFrom = "2016-11-06 12:17:53.984"
	cfg.TimeFilterValueTo = "2016-11-06 12:17:53.984"

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	// 完全一致のイベントのみ (12:17:53.984 の1行)
	if result.OutputRows != 1 {
		t.Errorf("OutputRows = %d, want 1", result.OutputRows)
	}
}

func TestRun_StringContentFilter_Unmatched(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.StringContentFilterValue = "NO_MATCH_CONTENT"

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.OutputRows != 0 {
		t.Errorf("OutputRows = %d, want 0", result.OutputRows)
	}
}

func TestRun_RegexFilter(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.RegexFilterValue = `WARN\s+\d+`

	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.OutputRows != 1 {
		t.Errorf("OutputRows = %d, want 1", result.OutputRows)
	}
	if out := readOutput(t, cfg.OutputFilePath); !strings.Contains(out, "警告メッセージ") {
		t.Errorf("WARNイベントが出力されない:\n%s", out)
	}
}

func TestRun_InvalidRegex(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.RegexFilterValue = "("

	if _, err := Run(cfg); err == nil {
		t.Error("不正な正規表現でエラーが発生すること")
	}
}

func TestRun_InvalidTimeRange(t *testing.T) {
	cfg := baseConfig(t, "sample.log")
	cfg.TimeFilterValueFrom = "2016-11-07"
	cfg.TimeFilterValueTo = "2016-11-06"

	if _, err := Run(cfg); err == nil || !strings.Contains(err.Error(), "TimeFilterのFromは、Toより前の日時を設定して下さい。") {
		t.Errorf("From > To でエラーが発生すること, got: %v", err)
	}
}

func TestRun_CharsetConversion_SjisToUtf8(t *testing.T) {
	dir := t.TempDir()
	utf8Content := "2016-11-06 12:00:00.000 ERROR 日本語のエラーメッセージ\n" +
		"2016-11-06 12:00:01.000 INFO 日本語の情報メッセージ\n"
	sjisContent, err := japanese.ShiftJIS.NewEncoder().String(utf8Content)
	if err != nil {
		t.Fatal(err)
	}
	input := filepath.Join(dir, "sjis.log")
	if err := os.WriteFile(input, []byte(sjisContent), 0o644); err != nil {
		t.Fatal(err)
	}

	cfg := &config.Config{
		InputFilePath:            input,
		InputCharset:             "sjis",
		OutputFilePath:           filepath.Join(dir, "out.log"),
		OutputCharset:            "UTF-8",
		StringContentFilterValue: "エラー",
	}
	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.OutputRows != 1 {
		t.Errorf("OutputRows = %d, want 1", result.OutputRows)
	}
	want := "2016-11-06 12:00:00.000 ERROR 日本語のエラーメッセージ\n"
	if out := readOutput(t, cfg.OutputFilePath); out != want {
		t.Errorf("出力 = %q, want %q", out, want)
	}
}

func TestRun_CharsetConversion_OutputDefaultsToInput(t *testing.T) {
	dir := t.TempDir()
	utf8Content := "2016-11-06 12:00:00.000 ERROR 日本語のエラーメッセージ\n"
	sjisContent, err := japanese.ShiftJIS.NewEncoder().String(utf8Content)
	if err != nil {
		t.Fatal(err)
	}
	input := filepath.Join(dir, "sjis.log")
	if err := os.WriteFile(input, []byte(sjisContent), 0o644); err != nil {
		t.Fatal(err)
	}

	// OutputCharset 未設定 → 入力と同じ Shift_JIS で出力
	cfg := &config.Config{
		InputFilePath:  input,
		InputCharset:   "sjis",
		OutputFilePath: filepath.Join(dir, "out.log"),
	}
	if _, err := Run(cfg); err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if out := readOutput(t, cfg.OutputFilePath); out != sjisContent {
		t.Errorf("出力がShift_JISで書き出されていない")
	}
}

func TestRun_OverwritesExistingOutput(t *testing.T) {
	cfg := baseConfig(t, "apache_access.log")
	if err := os.WriteFile(cfg.OutputFilePath, []byte("old content\n"), 0o644); err != nil {
		t.Fatal(err)
	}

	if _, err := Run(cfg); err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if out := readOutput(t, cfg.OutputFilePath); strings.Contains(out, "old content") {
		t.Error("既存の出力ファイルが削除されていない")
	}
}

func TestRun_CreatesOutputDir(t *testing.T) {
	cfg := baseConfig(t, "apache_access.log")
	cfg.OutputFilePath = filepath.Join(t.TempDir(), "nested", "dir", "out.log")

	if _, err := Run(cfg); err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if _, err := os.Stat(cfg.OutputFilePath); err != nil {
		t.Errorf("出力ファイルが作成されていない: %v", err)
	}
}
