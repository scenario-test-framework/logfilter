package engine

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestStructuredFixture(t *testing.T) {
	for _, tt := range []struct{ format, file string }{{"json", "structured.jsonl"}, {"logfmt", "structured.logfmt"}} {
		t.Run(tt.format, func(t *testing.T) {
			cfg := baseConfig(t, tt.file)
			cfg.LogFormat = tt.format
			lines := strings.SplitAfter(readOutput(t, cfg.InputFilePath), "\n")
			cfg.LogLevelFilterValueList = []string{"WARN", "ERROR"}
			check := func(want string) {
				t.Helper()
				result, err := Run(cfg)
				if err != nil {
					t.Fatal(err)
				}
				if got := readOutput(t, cfg.OutputFilePath); got != want {
					t.Errorf("出力 = %q, want %q", got, want)
				}
				if result.InputRows != 5 || result.OutputRows != int64(strings.Count(want, "\n")) {
					t.Errorf("行数 = %+v", result)
				}
			}
			check(strings.Join(lines[1:5], ""))
			cfg.TimeFilterValueFrom = "2024-01-01 09:00:01"
			cfg.TimeFilterValueTo = "2024-01-01 09:00:02"
			check(strings.Join(lines[1:4], ""))
			cfg.TimeFilterValueTo = cfg.TimeFilterValueFrom
			cfg.StringContentFilterValue = "retry"
			cfg.RegexFilterValue = `checkout\.go:42\n`
			check(strings.Join(lines[1:4], ""))
			cfg.LogLevelFilterValueList = []string{"ERR"}
			check("")
		})
	}
}

func TestStructuredCustomFields(t *testing.T) {
	for _, tt := range []struct{ format, input string }{
		{"json", `{"time":"broken","log":{"clock":1000,"level":40},"log.clock":2000,"level":"INFO"}`},
		{"logfmt", `time=broken log.clock=2000 log.level=40 level=INFO`},
	} {
		cfg := baseConfig(t, "")
		cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
		if err := os.WriteFile(cfg.InputFilePath, []byte(tt.input), 0o600); err != nil {
			t.Fatal(err)
		}
		cfg.LogFormat, cfg.TimestampKey, cfg.TimestampFormat, cfg.LogLevelKey = tt.format, "log.clock", "unixms", "log.level"
		cfg.TimeFilterValueFrom = "1970-01-01T00:00:02Z"
		cfg.TimeFilterValueTo = cfg.TimeFilterValueFrom
		cfg.LogLevelFilterValueList = []string{"40"}
		result, err := Run(cfg)
		if err != nil || result.OutputRows != 1 {
			t.Errorf("%s: result = %+v, err = %v", tt.format, result, err)
		}
	}
}

func TestStructuredLevelFallbackOnContinuation(t *testing.T) {
	cfg := baseConfig(t, "")
	cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
	cfg.LogFormat = "json"
	cfg.LogLevelFilterValueList = []string{"ERROR"}
	ignored := "{\"time\":0,\"level\":\"\"}\n[ERROR] ignored\n"
	want := "{\"time\":1,\"level\":null,\"severity\":\"INFO\"}\n[ERROR] kept\n"
	if err := os.WriteFile(cfg.InputFilePath, []byte(ignored+want), 0o600); err != nil {
		t.Fatal(err)
	}
	if _, err := Run(cfg); err != nil {
		t.Fatal(err)
	}
	if got := readOutput(t, cfg.OutputFilePath); got != want {
		t.Errorf("出力 = %q, want %q", got, want)
	}
}

// 構造化形式でも StringContentFilter / RegexFilter がイベント全文 (継続行を含む) に効くこと。
// 時刻・レベルの条件を同じく満たす 2 イベントのうち、継続行の内容だけで片方が除外されることを確かめる。
func TestStructuredContentAndRegexFilters(t *testing.T) {
	for _, tt := range []struct{ format, a, b string }{
		{"json", `{"time":1,"level":"ERROR"}`, `{"time":2,"level":"ERROR"}`},
		{"logfmt", `time=1 level=ERROR`, `time=2 level=ERROR`},
	} {
		eventA := tt.a + "\n    at alpha.go:1\n"
		eventB := tt.b + "\n    at beta.go:2\n"
		for _, filter := range []string{"string", "regex"} {
			cfg := baseConfig(t, "")
			cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
			cfg.LogFormat = tt.format
			cfg.LogLevelFilterValueList = []string{"ERROR"}
			if filter == "string" {
				cfg.StringContentFilterValue = "beta.go"
			} else {
				cfg.RegexFilterValue = `beta\.go:\d`
			}
			if err := os.WriteFile(cfg.InputFilePath, []byte(eventA+eventB), 0o600); err != nil {
				t.Fatal(err)
			}
			if _, err := Run(cfg); err != nil {
				t.Fatal(err)
			}
			if got := readOutput(t, cfg.OutputFilePath); got != eventB {
				t.Errorf("%s/%s: 出力 = %q, want %q", tt.format, filter, got, eventB)
			}
		}
	}
}

// BOM は構造化形式の 1 行目に限り解析時だけ除くこと。出力は元の行 (BOM 付き) のまま、
// 2 行目以降は BOM を含む元の文字列で判定する (行頭の BOM でキー名がずれ、継続行になる) こと。
func TestStructuredBOM(t *testing.T) {
	for _, tt := range []struct{ name, format, input, want string }{
		{"logfmt 先頭キー", "logfmt",
			"\ufefflevel=INFO time=0 msg=\"previous ERROR reported\"\ntime=1 level=ERROR msg=x\n",
			"time=1 level=ERROR msg=x\n"},
		{"logfmt 出力保持", "logfmt",
			"\ufefftime=0 level=ERROR msg=x\ntime=1 level=INFO\n",
			"\ufefftime=0 level=ERROR msg=x\n"},
		{"json 出力保持", "json",
			"\ufeff{\"time\":0,\"level\":\"ERROR\"}\n{\"time\":1,\"level\":\"INFO\"}\n",
			"\ufeff{\"time\":0,\"level\":\"ERROR\"}\n"},
		{"json 2 行目の BOM は継続行", "json",
			"{\"time\":0,\"level\":\"ERROR\"}\n\ufeff{\"time\":1,\"level\":\"INFO\"}\n",
			"{\"time\":0,\"level\":\"ERROR\"}\n\ufeff{\"time\":1,\"level\":\"INFO\"}\n"},
		{"logfmt 2 行目の BOM は継続行", "logfmt",
			"time=0 level=ERROR\n\ufefftime=1 level=INFO\n",
			"time=0 level=ERROR\n\ufefftime=1 level=INFO\n"},
	} {
		cfg := baseConfig(t, "")
		cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
		cfg.LogFormat = tt.format
		cfg.LogLevelFilterValueList = []string{"ERROR"}
		if err := os.WriteFile(cfg.InputFilePath, []byte(tt.input), 0o600); err != nil {
			t.Fatal(err)
		}
		if _, err := Run(cfg); err != nil {
			t.Fatalf("%s: %v", tt.name, err)
		}
		if got := readOutput(t, cfg.OutputFilePath); got != tt.want {
			t.Errorf("%s: 出力 = %q, want %q", tt.name, got, tt.want)
		}
	}
}

// logfmt の継続行 (平文) に time などの単語が単独で現れても、イベント開始やエラーにしないこと。
func TestStructuredLogfmtPlainContinuationWithKeyWords(t *testing.T) {
	cfg := baseConfig(t, "")
	cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
	cfg.LogFormat = "logfmt"
	input := "time=2016-11-06T12:17:53+09:00 level=error msg=fail\n" +
		"retry after the time is synced\n" +
		"ts level timestamp\n"
	if err := os.WriteFile(cfg.InputFilePath, []byte(input), 0o600); err != nil {
		t.Fatal(err)
	}
	result, err := Run(cfg)
	if err != nil {
		t.Fatalf("Run error = %v", err)
	}
	if result.OutputRows != 3 {
		t.Errorf("OutputRows = %d, want 3", result.OutputRows)
	}
	if got := readOutput(t, cfg.OutputFilePath); got != input {
		t.Errorf("出力 = %q, want %q", got, input)
	}
}

func TestStructuredInputErrors(t *testing.T) {
	for _, tt := range []struct {
		format, input string
		want          []string
	}{
		{"json", "[]\n", []string{"ログイベントからタイムスタンプを検出できませんでした。対象文字列：[]"}},
		{"logfmt", "level=ERROR\n", []string{"ログイベントからタイムスタンプを検出できませんでした。対象文字列：level=ERROR"}},
		{"json", "{\"time\":1}\ntrace\n{\"time\":\"oops\",\"ts\":1}\n", []string{"3 行目", "キー=time", "設定値=oops"}},
		{"json", "{\"ts\":null}\n", []string{"1 行目", "キー=ts", "設定値=null"}},
		{"logfmt", "time=1\ntime=broken\n", []string{"2 行目", "キー=time", "設定値=broken"}},
	} {
		cfg := baseConfig(t, "")
		cfg.InputFilePath = filepath.Join(t.TempDir(), "in")
		cfg.LogFormat = tt.format
		if err := os.WriteFile(cfg.InputFilePath, []byte(tt.input), 0o600); err != nil {
			t.Fatal(err)
		}
		_, err := Run(cfg)
		for _, want := range tt.want {
			if err == nil || !strings.Contains(err.Error(), want) {
				t.Errorf("Run(%q) error = %v, want %q", tt.input, err, want)
			}
		}
	}
}
