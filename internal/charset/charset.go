// Package charset は文字コード名から x/text の encoding.Encoding を解決します。
// 日本語主要どころ (UTF-8 / Shift_JIS / EUC-JP / ISO-2022-JP / UTF-16) と
// その別名 ("utf8", "sjis", "MS932" など) に対応します。
package charset

import (
	"fmt"
	"strings"

	"golang.org/x/text/encoding"
	"golang.org/x/text/encoding/htmlindex"
)

// Lookup は文字コード名を encoding.Encoding に解決します。
// 解決できない場合は error を返します。
func Lookup(name string) (encoding.Encoding, error) {
	normalized := strings.ToLower(strings.TrimSpace(name))
	if normalized == "" {
		return nil, fmt.Errorf("文字コードが指定されていません。")
	}
	if enc, err := htmlindex.Get(normalized); err == nil {
		return enc, nil
	}
	// Java流の "EUC_JP" などアンダースコア表記を "-" に置き換えて再試行
	if enc, err := htmlindex.Get(strings.ReplaceAll(normalized, "_", "-")); err == nil {
		return enc, nil
	}
	return nil, fmt.Errorf("サポートされていない文字コードです。 設定値=%s", name)
}
