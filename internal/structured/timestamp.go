package structured

import (
	"encoding/json"
	"math/big"
	"regexp"
	"time"

	"github.com/scenario-test-framework/logfilter/internal/timeparse"
)

var decimalString = regexp.MustCompile(`^-?[0-9]+(\.[0-9]+)?$`)

func parseTimestamp(value any, format string) (time.Time, bool) {
	var digits string
	switch v := value.(type) {
	case string:
		isDecimal := decimalString.MatchString(v)
		// 数字だけの文字列は Unix 時刻として扱う。ただし 8 桁の整数は既存の日時変換で yyyyMMdd と読む。
		// (既存の日時変換は 17 桁以上の数字列を固定位置で日時と読むため、ナノ秒の文字列を誤解釈させない)
		if (format == "" || format == "auto") && (!isDecimal || isCalendarDate(v)) {
			if ts, err := timeparse.Parse(v); err == nil {
				return ts, true
			}
		}
		if !isDecimal {
			return time.Time{}, false
		}
		digits = v
	case json.Number:
		digits = v.String()
	default:
		return time.Time{}, false
	}

	// 指数表記を含む10進数も分数のまま扱い、ナノ秒までの桁を正確に残す。
	number, ok := new(big.Rat).SetString(digits)
	if !ok {
		return time.Time{}, false
	}
	var unit time.Duration
	switch format {
	case "", "auto":
		whole := new(big.Int).Quo(number.Num(), number.Denom())
		switch {
		case whole.CmpAbs(big.NewInt(1e11)) < 0:
			unit = time.Second
		case whole.CmpAbs(big.NewInt(1e14)) < 0:
			unit = time.Millisecond
		case whole.CmpAbs(big.NewInt(1e17)) < 0:
			unit = time.Microsecond
		default:
			unit = time.Nanosecond
		}
	case "unix":
		unit = time.Second
	case "unixms":
		unit = time.Millisecond
	case "unixus":
		unit = time.Microsecond
	case "unixns":
		unit = time.Nanosecond
	default:
		return time.Time{}, false
	}

	// 1ナノ秒未満をゼロ方向に切り捨て、負の時刻も非負のナノ秒部分に分解する。
	nanos := new(big.Int).Mul(number.Num(), big.NewInt(int64(unit)))
	nanos.Quo(nanos, number.Denom())
	var seconds, fraction big.Int
	seconds.DivMod(nanos, big.NewInt(int64(time.Second)), &fraction)
	// time.Time で正しく比較できる範囲 (0001〜9999 年) に限る。範囲外は変換エラーとする。
	if !seconds.IsInt64() || seconds.Int64() < minUnixSeconds || seconds.Int64() > maxUnixSeconds {
		return time.Time{}, false
	}
	return time.Unix(seconds.Int64(), fraction.Int64()), true
}

// Unix 時刻として受け付ける秒の範囲 (0001-01-01T00:00:00Z 〜 9999-12-31T23:59:59Z)。
const (
	minUnixSeconds = -62135596800
	maxUnixSeconds = 253402300799
)

// isCalendarDate は yyyyMMdd 形式とみなす 8 桁の数字列か否かを返します。
func isCalendarDate(s string) bool {
	if len(s) != 8 {
		return false
	}
	for _, ch := range s {
		if ch < '0' || ch > '9' {
			return false
		}
	}
	return true
}
