// Package timeparse は、旧Java実装 (me.suwash.util.DateUtils#toDate) と互換の
// 日時文字列パースを提供します。
//
// 対応フォーマット:
//   - ISO-8601形式 (yyyy-MM-dd'T'HH:mm:ss[.SSS|,SSS]±ZZZZ、末尾":"許容)
//   - apache access log timestamp形式 ([dd/MMM/yyyy:HH:mm:ss ±ZZZZ])
//   - 汎用日時フォーマット (yyyy/MM/dd, yy-MM-dd, yyyyMMdd など + 時刻)
package timeparse

import (
	"fmt"
	"strconv"
	"strings"
	"time"
)

// dateStringLength は日付文字列の最小文字数 (YYYYMMDD)。
const dateStringLength = 8

// Parse は日時文字列を time.Time に変換します。
// 変換できない場合は error を返します。
func Parse(s string) (time.Time, error) {
	if t, ok := parseCommonFormat(s); ok {
		return t, nil
	}
	return parseGenericFormat(s)
}

// isoLayouts は ISO-8601 変換で試行するレイアウト。
// Java実装の "yyyy-MM-dd'T'HH:mm:ss.SSSX" → ",SSSX" → "X(ミリ秒なし)" の順を維持しつつ、
// タイムゾーン表記のゆれ (+09 / +0900 / +09:00 / Z) を許容します。
var isoLayouts = []string{
	"2006-01-02T15:04:05.000Z0700",
	"2006-01-02T15:04:05.000Z07:00",
	"2006-01-02T15:04:05.000Z07",
	"2006-01-02T15:04:05Z0700",
	"2006-01-02T15:04:05Z07:00",
	"2006-01-02T15:04:05Z07",
}

// parseCommonFormat は ISO-8601 / apache access log 形式の変換を試行します。
func parseCommonFormat(s string) (time.Time, bool) {
	if s == "" {
		return time.Time{}, false
	}

	// ISO-8601: スペースなし、"-"あり、"T"あり、":"あり
	if !strings.Contains(s, " ") && strings.Contains(s, "-") && strings.Contains(s, "T") && strings.Contains(s, ":") {
		target := s
		// yyyy-MM-dd'T'HH:mm:ss.SSS+0900: 形式 → 末尾の : を除去
		target = strings.TrimSuffix(target, ":")
		// ミリ秒の "," 区切りは "." に正規化 (Javaは ,SSS レイアウトで試行)
		target = strings.Replace(target, ",", ".", 1)
		for _, layout := range isoLayouts {
			if t, err := time.Parse(layout, target); err == nil {
				return t, true
			}
		}
	}

	// apache access log: [ ]括り、スペースあり、"/"あり、":"あり
	if len(s) >= 2 && s[0] == '[' && s[len(s)-1] == ']' &&
		strings.Contains(s, " ") && strings.Contains(s, "/") && strings.Contains(s, ":") {
		target := s[1 : len(s)-1]
		if t, err := time.Parse("02/Jan/2006:15:04:05 -0700", target); err == nil {
			return t, true
		}
	}

	return time.Time{}, false
}

// parseGenericFormat は汎用日時フォーマットを解析します。
// Java実装の format() + toCalendarMain() (Calendar lenient=false) 相当です。
func parseGenericFormat(s string) (time.Time, error) {
	target := strings.TrimSpace(s)
	if len(target) < dateStringLength {
		return time.Time{}, unsupportedErr(s)
	}

	var year, month, day, hour, minute, sec, millis string
	hour, minute, sec, millis = "00", "00", "00", "000"

	if !strings.ContainsAny(target, "/-") {
		if len(target) == dateStringLength {
			// yyyyMMdd形式
			year, month, day = target[0:4], target[4:6], target[6:8]
			return buildTime(s, year, month, day, hour, minute, sec, millis)
		}
		// yyyyMMdd HH:mm:ss形式 (区切り位置固定)
		if len(target) < 17 {
			return time.Time{}, unsupportedErr(s)
		}
		year, month, day = target[0:4], target[4:6], target[6:8]
		hour, minute, sec = target[9:11], target[12:14], target[15:17]
		return buildTime(s, year, month, day, hour, minute, sec, millis)
	}

	// "_/-:.,T " 区切りで 年/月/日/時/分/秒/ミリ秒 に分解
	tokens := strings.FieldsFunc(target, func(r rune) bool {
		return strings.ContainsRune("_/-:.,T ", r)
	})
	if len(tokens) > 7 || len(tokens) == 0 {
		return time.Time{}, unsupportedErr(s)
	}
	for i, token := range tokens {
		switch i {
		case 0: // 年: 左"20"埋めで4桁化 (yy対応)
			year = fill(token, true, 4, "20")
		case 1:
			month = fill(token, true, 2, "0")
		case 2:
			day = fill(token, true, 2, "0")
		case 3:
			hour = fill(token, true, 2, "0")
		case 4:
			minute = fill(token, true, 2, "0")
		case 5:
			sec = fill(token, true, 2, "0")
		case 6: // ミリ秒: 右"0"埋めで3桁化
			millis = fill(token, false, 3, "0")
		}
	}
	return buildTime(s, year, month, day, hour, minute, sec, millis)
}

// buildTime は各要素文字列を検証して time.Time (ローカルタイム) を構築します。
// Go の time.Date は範囲外値を正規化してしまうため、Java Calendar (lenient=false) 相当の
// 範囲チェックを明示的に行います。
func buildTime(orig, year, month, day, hour, minute, sec, millis string) (time.Time, error) {
	intVal := func(v string) (int, bool) {
		n, err := strconv.Atoi(v)
		return n, err == nil
	}
	y, okY := intVal(year)
	mo, okMo := intVal(month)
	d, okD := intVal(day)
	h, okH := intVal(hour)
	mi, okMi := intVal(minute)
	se, okSe := intVal(sec)
	ms, okMs := intVal(millis)
	if !okY || !okMo || !okD || !okH || !okMi || !okSe || !okMs {
		return time.Time{}, unsupportedErr(orig)
	}
	if mo < 1 || mo > 12 || d < 1 || h > 23 || mi > 59 || se > 59 ||
		h < 0 || mi < 0 || se < 0 || ms < 0 || ms > 999 {
		return time.Time{}, unsupportedErr(orig)
	}
	if d > daysIn(y, mo) {
		return time.Time{}, unsupportedErr(orig)
	}
	return time.Date(y, time.Month(mo), d, h, mi, se, ms*int(time.Millisecond), time.Local), nil
}

// daysIn は指定年月の日数を返します。
func daysIn(year, month int) int {
	return time.Date(year, time.Month(month)+1, 0, 0, 0, 0, 0, time.UTC).Day()
}

// fill は Java実装の fillString 移植です。
// 対象文字列が指定桁数になるまで、補充文字列を左または右に挿入します。
func fill(s string, left bool, length int, add string) string {
	buf := s
	for length > len(buf) {
		if left {
			cur := add
			if sum := len(buf) + len(cur); sum > length {
				cur = cur[:len(cur)-(sum-length)]
			}
			buf = cur + buf
		} else {
			buf += add
		}
	}
	if len(buf) > length {
		return buf[:length]
	}
	return buf
}

func unsupportedErr(s string) error {
	return fmt.Errorf("日時に変換できません。 設定値=%s", s)
}
