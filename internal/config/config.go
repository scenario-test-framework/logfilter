// Package config はシステム設定 (JSON) の読み込み・マージ・バリデーションを提供します。
// 設定ファイルのキーは旧Java実装と互換です。
package config

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strings"

	"github.com/scenario-test-framework/logfilter/internal/charset"
	"github.com/scenario-test-framework/logfilter/internal/timeparse"
)

// Config はシステム設定。
type Config struct {
	// 入力ファイルパス
	InputFilePath string `json:"inputFilePath"`
	// 入力ファイル文字コード
	InputCharset string `json:"inputCharset"`
	// 出力ファイルパス
	OutputFilePath string `json:"outputFilePath"`
	// 出力ファイル文字コード (未設定の場合、入力文字コードと同一)
	OutputCharset string `json:"outputCharset"`
	// TimeFilter設定値: 抽出開始時刻
	TimeFilterValueFrom string `json:"timeFilterValueFrom"`
	// TimeFilter設定値: 抽出終了時刻
	TimeFilterValueTo string `json:"timeFilterValueTo"`
	// LogLevelFilter設定値
	LogLevelFilterValueList []string `json:"logLevelFilterValueList"`
	// StringContentFilter設定値
	StringContentFilterValue string `json:"stringContentFilterValue"`
	// RegexFilter設定値
	RegexFilterValue string `json:"regexFilterValue"`
}

// Default はデフォルト設定を返します (旧 log_filter.json 相当)。
func Default() *Config {
	return &Config{
		InputCharset: "UTF-8",
	}
}

// Load は設定ファイル (JSON, UTF-8) を読み込みます。
func Load(path string) (*Config, error) {
	data, err := os.ReadFile(path)
	if err != nil {
		return nil, fmt.Errorf("設定ファイルを読み込めません。 設定値=%s: %w", path, err)
	}
	cfg := &Config{}
	if err := json.Unmarshal(data, cfg); err != nil {
		return nil, fmt.Errorf("設定ファイルをパースできません。 設定値=%s: %w", path, err)
	}
	return cfg, nil
}

// ApplyDefault はデフォルト設定を反映します (未設定項目のみ)。
func (c *Config) ApplyDefault(d *Config) {
	if c.InputCharset == "" {
		c.InputCharset = d.InputCharset
	}
}

// Validate は設定の妥当性を検証します。
// 旧Java実装と同様に、検出したすべての違反をまとめて返します。
func (c *Config) Validate() error {
	var violations []string

	// 入力ファイルパス
	if c.InputFilePath == "" {
		violations = append(violations, "inputFilePath:必須項目です。")
	} else if info, err := os.Stat(c.InputFilePath); err != nil {
		violations = append(violations, fmt.Sprintf("inputFilePath:存在しないパスです。 設定値=%s", c.InputFilePath))
	} else if info.IsDir() {
		violations = append(violations, fmt.Sprintf("inputFilePath:ファイルではありません。 設定値=%s", c.InputFilePath))
	}

	// 出力ファイルパス
	if c.OutputFilePath == "" {
		violations = append(violations, "outputFilePath:必須項目です。")
	}

	// 文字コード
	if c.InputCharset == "" {
		violations = append(violations, "inputCharset:必須項目です。")
	} else if _, err := charset.Lookup(c.InputCharset); err != nil {
		violations = append(violations, fmt.Sprintf("inputCharset:サポートされていない文字コードです。 設定値=%s", c.InputCharset))
	}
	if c.OutputCharset != "" {
		if _, err := charset.Lookup(c.OutputCharset); err != nil {
			violations = append(violations, fmt.Sprintf("outputCharset:サポートされていない文字コードです。 設定値=%s", c.OutputCharset))
		}
	}

	// TimeFilter
	fromSet, toSet := c.TimeFilterValueFrom != "", c.TimeFilterValueTo != ""
	if fromSet {
		if _, err := timeparse.Parse(c.TimeFilterValueFrom); err != nil {
			violations = append(violations, fmt.Sprintf("timeFilterValueFrom:日時に変換できません。 設定値=%s", c.TimeFilterValueFrom))
			fromSet = false
		}
	}
	if toSet {
		if _, err := timeparse.Parse(c.TimeFilterValueTo); err != nil {
			violations = append(violations, fmt.Sprintf("timeFilterValueTo:日時に変換できません。 設定値=%s", c.TimeFilterValueTo))
			toSet = false
		}
	}
	// From/To の前後関係
	if fromSet && toSet {
		from, _ := timeparse.Parse(c.TimeFilterValueFrom)
		to, _ := timeparse.Parse(c.TimeFilterValueTo)
		if from.After(to) {
			violations = append(violations, "timeFilter:TimeFilterのFromは、Toより前の日時を設定して下さい。")
		}
	}

	if len(violations) > 0 {
		return errors.New("妥当性チェックエラーが発生しました。\n" + strings.Join(violations, "\n"))
	}
	return nil
}
