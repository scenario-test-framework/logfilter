package structured

import (
	"encoding/json"
	"testing"
	"time"
)

func TestTimestampConversion(t *testing.T) {
	for _, tt := range []struct {
		value  any
		format string
		want   time.Time
	}{
		{"2024-01-01 09:00:00", "", time.Date(2024, 1, 1, 9, 0, 0, 0, time.Local)},
		{"20240101", "auto", time.Date(2024, 1, 1, 0, 0, 0, 0, time.Local)},
		{json.Number("20240101"), "auto", time.Unix(20240101, 0)},
		{"20240101", "unix", time.Unix(20240101, 0)},
		{"1704067200.123456789", "auto", time.Unix(1704067200, 123456789)},
		{json.Number("99999999999.5"), "auto", time.Unix(99999999999, 500000000)},
		{json.Number("-100000000000"), "auto", time.Unix(-100000000, 0)},
		{json.Number("99999999999999"), "auto", time.Unix(99999999999, 999000000)},
		{json.Number("100000000000000"), "auto", time.Unix(100000000, 0)},
		{json.Number("99999999999999999"), "auto", time.Unix(99999999999, 999999000)},
		{json.Number("100000000000000000"), "auto", time.Unix(100000000, 0)},
		{json.Number("1704067200123456789"), "auto", time.Unix(1704067200, 123456789)},
		{json.Number("1.704067200123456789e18"), "auto", time.Unix(1704067200, 123456789)},
		{"-0.0000000019", "unix", time.Unix(-1, 999999999)},
		{"00001234.567891", "unixms", time.Unix(1, 234567891)},
		{json.Number("-1000000.123"), "unixus", time.Unix(-1, -123)},
		{"1704067200123456789.9", "unixns", time.Unix(1704067200, 123456789)},
		// 19 桁のナノ秒文字列を既存の日時変換 (固定位置) で 1704 年と誤解釈しない
		{"1704010100000000000", "auto", time.Unix(1704010100, 0)},
		{"1704067200", "auto", time.Unix(1704067200, 0)},
		// 8 桁でも暦日として不正なら Unix 秒として扱う
		{"20240230", "auto", time.Unix(20240230, 0)},
		{"253402300799", "unix", time.Unix(253402300799, 0)},
		{"-62135596800", "unix", time.Unix(-62135596800, 0)},
	} {
		got, ok, err := (Record{"time": tt.value}).Timestamp("", tt.format)
		if err != nil || !ok || !got.Equal(tt.want) {
			t.Errorf("Timestamp(%v, %q) = (%v, %t, %v), want %v", tt.value, tt.format, got, ok, err, tt.want)
		}
	}
}

func TestInvalidTimestamps(t *testing.T) {
	for _, tt := range []struct {
		value  any
		format string
	}{
		{"invalid", "auto"}, {nil, "auto"}, {true, "auto"}, {map[string]any{}, "auto"}, {[]any{}, "auto"},
		{"2024-01-01", "unix"}, {"1e3", "unixms"}, {"+1", "unixus"}, {"1.", "unixns"}, {".5", "unix"},
		{"999999999999999999999999", "unix"},
		// time.Time で比較できない範囲 (0001〜9999 年の外)
		{json.Number("9223372036854775807"), "unix"}, {"253402300800", "unix"}, {"-62135596801", "unix"},
	} {
		_, _, err := (Record{"time": tt.value, "timestamp": json.Number("1")}).Timestamp("", tt.format)
		if err == nil {
			t.Errorf("Timestamp(%v, %q) succeeded", tt.value, tt.format)
		}
	}
}
