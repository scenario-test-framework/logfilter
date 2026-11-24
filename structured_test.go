package main

import (
	"os"
	"path/filepath"
	"testing"
)

func TestStructuredOptionOverrides(t *testing.T) {
	dir := t.TempDir()
	path, input := filepath.Join(dir, "config.json"), filepath.Join(dir, "in")
	if err := os.WriteFile(path, []byte(`{"logFormat":"json","timestampKey":"missing","timestampFormat":"unix","logLevelKey":"missing"}`), 0o600); err != nil {
		t.Fatal(err)
	}
	want := "clock=1000 rank=warn\n"
	if err := os.WriteFile(input, []byte(want+"clock=2000 rank=warn\n"), 0o600); err != nil {
		t.Fatal(err)
	}
	for _, flags := range [][]string{
		{"-lf", "'logfmt'", "-tk", "'clock'", "-tfmt", "'unixms'", "-lk", "'rank'"},
		{"--logFormat", "logfmt", "--timestampKey", "clock", "--timestampFormat", "unixms", "--logLevelKey", "rank"},
	} {
		out := filepath.Join(t.TempDir(), "out")
		args := append([]string{"-cf", path}, flags...)
		got := run(append(args, "-l", "WARN", "-tt", "1970-01-01T00:00:02Z", input, out))
		if got != exitSuccess {
			t.Fatalf("run(%v) = %d", flags, got)
		}
		if data, err := os.ReadFile(out); err != nil || string(data) != want {
			t.Errorf("出力 = %q, err = %v, want %q", data, err, want)
		}
	}
}

func TestStructuredExitCodes(t *testing.T) {
	for _, args := range [][]string{
		{"-tk", "time", testdata("sample.log")},
		{testdata("structured.jsonl")},
		{"-lf", "json", "-tfmt", "unix", testdata("structured.jsonl")},
	} {
		if got := run(append(args, filepath.Join(t.TempDir(), "out"))); got != exitError {
			t.Errorf("run(%v) = %d, want %d", args, got, exitError)
		}
	}
}
