package structured

import (
	"strings"
	"unicode"
)

// parseLogfmt は logfmt の 1 行を解析します。
// "=" を伴わない単語 (値なしキー) は記録しません。平文の継続行に含まれる
// "time" などの単語を、タイムスタンプやレベルのキーと誤認しないためです。
func parseLogfmt(line string) Record {
	r := Record{}
	for {
		line = strings.TrimLeftFunc(line, unicode.IsSpace)
		if line == "" {
			return r
		}

		end := strings.IndexFunc(line, func(ch rune) bool {
			return ch == '=' || ch == '"' || unicode.IsSpace(ch)
		})
		if end < 0 {
			// 行末の値なしキー
			return r
		}
		if end == 0 || line[end] == '"' {
			return nil
		}
		key := line[:end]
		line = line[end:]
		if line[0] != '=' {
			// 値なしキー
			continue
		}

		line = line[1:]
		if strings.HasPrefix(line, `"`) {
			value, rest, ok := quotedValue(line[1:])
			if !ok {
				return nil
			}
			r[key], line = value, rest
			continue
		}
		// 引用符なしの値に " や = が現れたら不正な行 (継続行) とする
		end = strings.IndexFunc(line, func(ch rune) bool { return ch == '"' || ch == '=' || unicode.IsSpace(ch) })
		if end < 0 {
			r[key] = line
			return r
		}
		if line[end] == '"' || line[end] == '=' {
			return nil
		}
		r[key], line = line[:end], line[end:]
	}
}

// quotedValue は開始引用符の直後を受け取り、エスケープを戻した値と残りの入力を返します。
func quotedValue(s string) (string, string, bool) {
	var value strings.Builder
	for {
		i := strings.IndexAny(s, `"\`)
		if i < 0 {
			return "", "", false
		}
		value.WriteString(s[:i])
		ch := s[i]
		s = s[i+1:]
		if ch == '"' {
			// 閉じ引用符の直後は行末または空白に限る。
			return value.String(), s, s == "" || strings.IndexFunc(s, unicode.IsSpace) == 0
		}
		if s == "" {
			return "", "", false
		}
		switch s[0] {
		case '"', '\\':
			value.WriteByte(s[0])
		case 'n':
			value.WriteByte('\n')
		case 't':
			value.WriteByte('\t')
		case 'r':
			value.WriteByte('\r')
		default:
			return "", "", false
		}
		s = s[1:]
	}
}
