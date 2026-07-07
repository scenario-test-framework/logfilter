package config

import (
	"path/filepath"
	"reflect"
	"strings"
	"testing"
)

func TestLoad(t *testing.T) {
	cfg, err := Load(filepath.Join("..", "..", "testdata", "ignoreDebug.json"))
	if err != nil {
		t.Fatalf("Load error = %v", err)
	}
	if cfg.InputCharset != "utf8" {
		t.Errorf("InputCharset = %q, want %q", cfg.InputCharset, "utf8")
	}
	want := []string{"INFO", "WARN", "ERROR"}
	if !reflect.DeepEqual(cfg.LogLevelFilterValueList, want) {
		t.Errorf("LogLevelFilterValueList = %v, want %v", cfg.LogLevelFilterValueList, want)
	}
}

func TestLoad_Error(t *testing.T) {
	if _, err := Load("/path/to/notexist.json"); err == nil {
		t.Error("Load should return error for nonexistent file")
	}
}

func TestApplyDefault(t *testing.T) {
	cfg := &Config{}
	cfg.ApplyDefault(Default())
	if cfg.InputCharset != "UTF-8" {
		t.Errorf("InputCharset = %q, want %q", cfg.InputCharset, "UTF-8")
	}

	// 設定済みの場合は上書きしない
	cfg = &Config{InputCharset: "sjis"}
	cfg.ApplyDefault(Default())
	if cfg.InputCharset != "sjis" {
		t.Errorf("InputCharset = %q, want %q", cfg.InputCharset, "sjis")
	}
}

func TestValidate_AllViolations(t *testing.T) {
	// 旧Javaテスト (LogFilterServiceTest#test_validate) の移植
	cfg := &Config{
		InputFilePath:       "/path/to/input",
		InputCharset:        "NotExist",
		OutputCharset:       "NotExist2",
		TimeFilterValueFrom: "unmatched date format",
		TimeFilterValueTo:   "9999-13-01 00:00:00.000",
	}
	err := cfg.Validate()
	if err == nil {
		t.Fatal("Validate should return error")
	}
	msg := err.Error()
	for _, want := range []string{
		"inputFilePath:存在しないパスです。",
		"outputFilePath:必須項目です。",
		"inputCharset:サポートされていない文字コードです。",
		"outputCharset:サポートされていない文字コードです。",
		"timeFilterValueFrom:日時に変換できません。",
		"timeFilterValueTo:日時に変換できません。",
	} {
		if !strings.Contains(msg, want) {
			t.Errorf("Validate message should contain %q\ngot: %s", want, msg)
		}
	}
}

func TestValidate_InvalidTimeRange(t *testing.T) {
	cfg := &Config{
		InputFilePath:       filepath.Join("..", "..", "testdata", "sample.log"),
		InputCharset:        "UTF-8",
		OutputFilePath:      "/tmp/out.log",
		TimeFilterValueFrom: "2016-11-07",
		TimeFilterValueTo:   "2016-11-06",
	}
	err := cfg.Validate()
	if err == nil || !strings.Contains(err.Error(), "TimeFilterのFromは、Toより前の日時を設定して下さい。") {
		t.Errorf("Validate should report invalid time range, got: %v", err)
	}
}

func TestValidate_OK(t *testing.T) {
	cfg := &Config{
		InputFilePath:       filepath.Join("..", "..", "testdata", "sample.log"),
		InputCharset:        "UTF-8",
		OutputFilePath:      "/tmp/out.log",
		TimeFilterValueFrom: "2016-11-06",
		TimeFilterValueTo:   "2016-11-07",
	}
	if err := cfg.Validate(); err != nil {
		t.Errorf("Validate error = %v", err)
	}
}
