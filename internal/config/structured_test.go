package config

import (
	"os"
	"path/filepath"
	"strings"
	"testing"
)

func TestLoadStructuredSettings(t *testing.T) {
	path := filepath.Join(t.TempDir(), "config.json")
	if err := os.WriteFile(path, []byte(`{"logFormat":"json","timestampKey":"log.ts","timestampFormat":"unixus","logLevelKey":"log.level"}`), 0o600); err != nil {
		t.Fatal(err)
	}
	cfg, err := Load(path)
	if err != nil {
		t.Fatal(err)
	}
	cfg.ApplyDefault(Default())
	if cfg.LogFormat != "json" || cfg.TimestampKey != "log.ts" || cfg.TimestampFormat != "unixus" || cfg.LogLevelKey != "log.level" {
		t.Errorf("Load = %+v", cfg)
	}
}

func TestValidateStructuredSettings(t *testing.T) {
	for _, tt := range []struct {
		format, timestampFormat, timestampKey, levelKey string
		invalidKeys                                     []string
	}{
		{"JSON", "days", "", "", []string{"logFormat:", "設定値=JSON", "timestampFormat:", "設定値=days"}},
		{"text", "auto", "ts", "lvl", []string{"timestampFormat:", "設定値=auto", "timestampKey:", "設定値=ts", "logLevelKey:", "設定値=lvl"}},
		{"", "", "ts", "", []string{"timestampKey:", "設定値=ts"}},
		{"", "", "", "", nil}, {"text", "", "", "", nil},
		{"json", "auto", "", "", nil}, {"logfmt", "unix", "clock", "level", nil},
		{"json", "unixms", "", "", nil}, {"json", "unixus", "", "", nil}, {"json", "unixns", "", "", nil},
	} {
		cfg := Default()
		cfg.InputFilePath = filepath.Join("..", "..", "testdata", "sample.log")
		cfg.OutputFilePath = filepath.Join(t.TempDir(), "out")
		cfg.LogFormat, cfg.TimestampFormat, cfg.TimestampKey, cfg.LogLevelKey = tt.format, tt.timestampFormat, tt.timestampKey, tt.levelKey
		err := cfg.Validate()
		if len(tt.invalidKeys) == 0 && err != nil {
			t.Errorf("Validate(%+v) = %v", cfg, err)
		}
		for _, want := range tt.invalidKeys {
			if err == nil || !strings.Contains(err.Error(), want) {
				t.Errorf("Validate(%+v) = %v, want %q", cfg, err, want)
			}
		}
	}
}
