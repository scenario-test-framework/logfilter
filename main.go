// logfilter は、大容量のログファイルから 期間・ログレベル・文字列・正規表現 を指定して
// 必要なログイベントを抽出するCUIツールです。
//
// 旧Java実装 (me.suwash.tools.logfilter) と互換のCLIインターフェースを提供します。
package main

import (
	"bufio"
	"errors"
	"flag"
	"fmt"
	"log"
	"os"
	"strings"

	"github.com/scenario-test-framework/logfilter/internal/config"
	"github.com/scenario-test-framework/logfilter/internal/engine"
)

// CUIのリターンコード
const (
	exitSuccess = 0
	exitWarn    = 3
	exitError   = 6
)

const usageText = `logfilter [OPTION] inputFilePath [outputFilePath]

OPTION
  -h, --help
    このメッセージを表示します。

  -f, --force
    出力ファイルパスが存在する場合の確認を表示しません。

  -cf, --configFile
    /path/to/configFile
    設定ファイル (JSON) のパスを指定します。


  ----- 以降の設定を指定すると、設定ファイルより優先されます。 -----

  -ic, --inputCharset
    入力ファイルの文字コードを指定します。
    デフォルトは、UTF-8です。

  -oc, --outputCharset
    出力ファイルの文字コードを指定します。
    デフォルトは、入力ファイルと同じ文字コードです。

  -tf, --timeFilterFrom
    クォートで括った、指定のフォーマットで、抽出を開始する時刻を指定します。
    timeFilterTo と一緒に設定すると、AND条件で動作します。

    利用可能なタイムスタンプフォーマット
     'yyyy/MM/dd hh:mm:ss.SSS', 'yyyy/MM/dd hh:mm:ss', 'yyyy/MM/dd hh:mm', 'yyyy/MM/dd'
     'yyyy-MM-dd hh:mm:ss.SSS', 'yyyy-MM-dd hh:mm:ss', 'yyyy-MM-dd hh:mm', 'yyyy-MM-dd'

  -tt, --timeFilterTo
    クォートで括った、指定のフォーマットで抽出を終了する時刻を指定します。
    timeFilterFrom と一緒に設定すると、AND条件で動作します。

  -l, --logLevelFilter
    例：'WARN,ERROR'
    抽出するログレベルを、クォートで括った、カンマ区切りで列挙します。

  -s, --stringContentFilter
    例：'SOME_CONTENT'
    抽出する文字列をクォートで括って指定します。

  -r, --regexFilter
    例：'ERROR|Exception'
    抽出する正規表現をクォートで括って指定します。
`

// options はコマンドラインオプション。
type options struct {
	configFilePath      string
	forceOverwrite      bool
	inputCharset        string
	outputCharset       string
	timeFilterFrom      string
	timeFilterTo        string
	logLevelFilter      string
	stringContentFilter string
	regexFilter         string
}

func main() {
	log.SetFlags(log.LstdFlags)
	log.SetOutput(os.Stderr)
	os.Exit(run(os.Args[1:]))
}

func run(args []string) int {
	// ----------------------------------------
	// 引数のパース
	// ----------------------------------------
	if len(args) == 0 {
		fmt.Print(usageText)
		return exitSuccess
	}

	opt := &options{}
	fs := flag.NewFlagSet("logfilter", flag.ContinueOnError)
	fs.SetOutput(os.Stderr)
	fs.Usage = func() { fmt.Fprint(os.Stderr, usageText) }
	registerFlags(fs, opt)

	if err := fs.Parse(args); err != nil {
		if errors.Is(err, flag.ErrHelp) {
			return exitSuccess
		}
		return exitError
	}

	// 引数の数
	params := fs.Args()
	if len(params) < 1 || 2 < len(params) {
		log.Print("ERROR 引数の指定内容に誤りがあります。")
		fs.Usage()
		return exitError
	}

	// ----------------------------------------
	// 設定の読み込みと上書き
	// ----------------------------------------
	cfg, err := buildConfig(opt, params)
	if err != nil {
		log.Printf("ERROR %v", err)
		return exitError
	}

	// 強制上書きフラグの確認
	if !opt.forceOverwrite && cfg.OutputFilePath != "" {
		if _, err := os.Stat(cfg.OutputFilePath); err == nil {
			fmt.Printf("output file: %s is already exists. overwrite it ? y|n\n", cfg.OutputFilePath)
			if !confirmed(os.Stdin) {
				log.Print("INFO  canceled.")
				return exitSuccess
			}
		}
	}

	// ----------------------------------------
	// バリデーション
	// ----------------------------------------
	if err := cfg.Validate(); err != nil {
		log.Printf("ERROR %v", err)
		return exitError
	}

	// ----------------------------------------
	// フィルタリングの実行
	// ----------------------------------------
	printConfig(cfg)
	result, err := engine.Run(cfg)
	if err != nil {
		log.Printf("ERROR %v", err)
		log.Print("ERROR 処理が異常終了しました。")
		return exitError
	}

	// ----------------------------------------
	// 結果判定
	// ----------------------------------------
	log.Print("INFO  --------------------------------------------------")
	log.Print("INFO  ・入出力行数")
	log.Printf("INFO      ・入力：%d", result.InputRows)
	log.Printf("INFO      ・出力：%d", result.OutputRows)
	if result.OutputRows == 0 {
		log.Print("WARN  フィルタにマッチするログイベントは存在しませんでした。")
		log.Print("WARN  処理が警告終了しました。")
		return exitWarn
	}
	log.Print("INFO  処理が終了しました。")
	return exitSuccess
}

// registerFlags はコマンドラインオプションを登録します (短名・長名の両対応)。
func registerFlags(fs *flag.FlagSet, opt *options) {
	fs.StringVar(&opt.configFilePath, "cf", "", "設定ファイルパス")
	fs.StringVar(&opt.configFilePath, "configFile", "", "設定ファイルパス")
	fs.BoolVar(&opt.forceOverwrite, "f", false, "強制上書き")
	fs.BoolVar(&opt.forceOverwrite, "force", false, "強制上書き")
	fs.StringVar(&opt.inputCharset, "ic", "", "入力文字コード")
	fs.StringVar(&opt.inputCharset, "inputCharset", "", "入力文字コード")
	fs.StringVar(&opt.outputCharset, "oc", "", "出力文字コード")
	fs.StringVar(&opt.outputCharset, "outputCharset", "", "出力文字コード")
	fs.StringVar(&opt.timeFilterFrom, "tf", "", "TimeFilter設定値: From")
	fs.StringVar(&opt.timeFilterFrom, "timeFilterFrom", "", "TimeFilter設定値: From")
	fs.StringVar(&opt.timeFilterTo, "tt", "", "TimeFilter設定値: To")
	fs.StringVar(&opt.timeFilterTo, "timeFilterTo", "", "TimeFilter設定値: To")
	fs.StringVar(&opt.logLevelFilter, "l", "", "LogLevelFilter設定値")
	fs.StringVar(&opt.logLevelFilter, "logLevelFilter", "", "LogLevelFilter設定値")
	fs.StringVar(&opt.stringContentFilter, "s", "", "StringContentFilter設定値")
	fs.StringVar(&opt.stringContentFilter, "stringContentFilter", "", "StringContentFilter設定値")
	fs.StringVar(&opt.regexFilter, "r", "", "RegexFilter設定値")
	fs.StringVar(&opt.regexFilter, "regexFilter", "", "RegexFilter設定値")
}

// buildConfig はデフォルト設定・設定ファイル・コマンドラインオプションをマージした設定を返します。
func buildConfig(opt *options, params []string) (*config.Config, error) {
	// デフォルト設定
	cfg := config.Default()

	// 設定ファイルパスが指定されている場合、カスタム設定を読み込み
	if opt.configFilePath != "" {
		loaded, err := config.Load(escapeQuote(opt.configFilePath))
		if err != nil {
			return nil, err
		}
		loaded.ApplyDefault(cfg)
		cfg = loaded
	}

	// コマンドラインオプションで上書き
	if opt.inputCharset != "" {
		cfg.InputCharset = escapeQuote(opt.inputCharset)
	}
	if opt.outputCharset != "" {
		cfg.OutputCharset = escapeQuote(opt.outputCharset)
	}
	if opt.timeFilterFrom != "" {
		cfg.TimeFilterValueFrom = escapeQuote(opt.timeFilterFrom)
	}
	if opt.timeFilterTo != "" {
		cfg.TimeFilterValueTo = escapeQuote(opt.timeFilterTo)
	}
	if opt.logLevelFilter != "" {
		cfg.LogLevelFilterValueList = strings.Split(escapeQuote(opt.logLevelFilter), ",")
	}
	if opt.stringContentFilter != "" {
		cfg.StringContentFilterValue = escapeQuote(opt.stringContentFilter)
	}
	if opt.regexFilter != "" {
		cfg.RegexFilterValue = escapeQuote(opt.regexFilter)
	}

	// 入力ファイルパス
	cfg.InputFilePath = escapeQuote(params[0])
	// 出力ファイルパス
	if len(params) == 2 {
		cfg.OutputFilePath = escapeQuote(params[1])
	}

	return cfg, nil
}

// confirmed は標準入力から y|n を読み取り、y の場合に true を返します。
func confirmed(r *os.File) bool {
	scanner := bufio.NewScanner(r)
	if !scanner.Scan() {
		return false
	}
	return strings.EqualFold(strings.TrimSpace(scanner.Text()), "y")
}

// escapeQuote はクォート、ダブルクォートで括られた文字列から括り文字を除去します。
func escapeQuote(s string) string {
	if len(s) >= 2 {
		if (s[0] == '\'' && s[len(s)-1] == '\'') || (s[0] == '"' && s[len(s)-1] == '"') {
			return s[1 : len(s)-1]
		}
	}
	return s
}

// printConfig は実行する設定内容をログに出力します。
func printConfig(cfg *config.Config) {
	log.Print("INFO  ・入力ファイル")
	log.Printf("INFO      ・パス      : %s", cfg.InputFilePath)
	log.Printf("INFO      ・文字コード: %s", cfg.InputCharset)
	log.Print("INFO  ・出力ファイル")
	log.Printf("INFO      ・パス      : %s", cfg.OutputFilePath)
	log.Printf("INFO      ・文字コード: %s", cfg.OutputCharset)
	log.Print("INFO  ・フィルタ設定")
	log.Print("INFO      ・TimeFilter")
	log.Printf("INFO          ・From : %s", cfg.TimeFilterValueFrom)
	log.Printf("INFO          ・To   : %s", cfg.TimeFilterValueTo)
	log.Print("INFO      ・LogLevelFilter")
	log.Printf("INFO          ・Value: %v", cfg.LogLevelFilterValueList)
	log.Print("INFO      ・StringContentFilter")
	log.Printf("INFO          ・Value: %s", cfg.StringContentFilterValue)
	log.Print("INFO      ・RegexFilter")
	log.Printf("INFO          ・Value: %s", cfg.RegexFilterValue)
}
