package main

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func testdata(name string) string {
	return filepath.Join("testdata", name)
}

func TestRun_NoArgs_ShowsUsage(t *testing.T) {
	if got := run(nil); got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
}

func TestRun_TooManyArgs(t *testing.T) {
	if got := run([]string{"a", "b", "c"}); got != exitError {
		t.Errorf("run() = %d, want %d", got, exitError)
	}
}

func TestRun_Success(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"-l", "ERROR", testdata("sample.log"), out})
	if got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
	data, err := os.ReadFile(out)
	if err != nil {
		t.Fatalf("出力ファイルを読み込めません: %v", err)
	}
	if !strings.Contains(string(data), "ERROR") {
		t.Error("ERRORイベントが出力されていない")
	}
}

func TestRun_Warn_WhenUnmatched(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"-s", "NO_MATCH_CONTENT", testdata("sample.log"), out})
	if got != exitWarn {
		t.Errorf("run() = %d, want %d", got, exitWarn)
	}
}

func TestRun_Error_UnsupportedFormat(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{testdata("unsupported.log"), out})
	if got != exitError {
		t.Errorf("run() = %d, want %d", got, exitError)
	}
}

func TestRun_Error_ValidationFailure(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"-ic", "NotExist", testdata("sample.log"), out})
	if got != exitError {
		t.Errorf("run() = %d, want %d", got, exitError)
	}
}

func TestRun_Error_OutputPathMissing(t *testing.T) {
	// 出力パスは位置引数でも設定ファイルでも未指定 → バリデーションエラー
	got := run([]string{testdata("sample.log")})
	if got != exitError {
		t.Errorf("run() = %d, want %d", got, exitError)
	}
}

func TestRun_WithConfigFile(t *testing.T) {
	// ignoreDebug.json: inputCharset=utf8, logLevelFilterValueList=[INFO,WARN,ERROR]
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"-cf", testdata("ignoreDebug.json"), testdata("sample.log"), out})
	if got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
	data, err := os.ReadFile(out)
	if err != nil {
		t.Fatalf("出力ファイルを読み込めません: %v", err)
	}
	if strings.Contains(string(data), "DEBUG") {
		t.Error("DEBUGイベントが除外されていない")
	}
}

func TestRun_CommandOptionOverridesConfigFile(t *testing.T) {
	// 設定ファイルの [INFO,WARN,ERROR] をコマンドオプションの [ERROR] で上書き
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"-cf", testdata("ignoreDebug.json"), "-l", "ERROR", testdata("sample.log"), out})
	if got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
	data, err := os.ReadFile(out)
	if err != nil {
		t.Fatalf("出力ファイルを読み込めません: %v", err)
	}
	if strings.Contains(string(data), "INFO") || strings.Contains(string(data), "WARN") {
		t.Error("設定ファイルのログレベルが上書きされていない")
	}
}

func TestRun_ForceOverwrite(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	if err := os.WriteFile(out, []byte("old\n"), 0o644); err != nil {
		t.Fatal(err)
	}
	// -f 指定時は確認プロンプトなしで上書き
	got := run([]string{"-f", "-l", "ERROR", testdata("sample.log"), out})
	if got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
	data, _ := os.ReadFile(out)
	if strings.Contains(string(data), "old") {
		t.Error("出力ファイルが上書きされていない")
	}
}

func TestRun_LongOptions(t *testing.T) {
	out := filepath.Join(t.TempDir(), "out.log")
	got := run([]string{"--logLevelFilter", "ERROR", "--force", testdata("sample.log"), out})
	if got != exitSuccess {
		t.Errorf("run() = %d, want %d", got, exitSuccess)
	}
}

func TestEscapeQuote(t *testing.T) {
	tests := []struct {
		in   string
		want string
	}{
		{"'quoted'", "quoted"},
		{`"quoted"`, "quoted"},
		{"plain", "plain"},
		{"'unbalanced", "'unbalanced"},
		{"", ""},
		{"'", "'"},
	}
	for _, tt := range tests {
		if got := escapeQuote(tt.in); got != tt.want {
			t.Errorf("escapeQuote(%q) = %q, want %q", tt.in, got, tt.want)
		}
	}
}
