package timeparse

import (
	"testing"
	"time"
)

func TestParse_Generic(t *testing.T) {
	tests := []struct {
		name string
		in   string
		want time.Time
	}{
		{"yyyy-MM-dd HH:mm:ss.SSS", "2016-11-06 12:17:53.985", time.Date(2016, 11, 6, 12, 17, 53, 985e6, time.Local)},
		{"yyyy/MM/dd HH:mm:ss.SSS", "2016/11/06 12:17:53.985", time.Date(2016, 11, 6, 12, 17, 53, 985e6, time.Local)},
		{"yyyy-MM-dd HH:mm:ss", "2016-11-06 12:17:53", time.Date(2016, 11, 6, 12, 17, 53, 0, time.Local)},
		{"yyyy-MM-dd HH:mm", "2016-11-06 12:17", time.Date(2016, 11, 6, 12, 17, 0, 0, time.Local)},
		{"yyyy-MM-dd", "2016-11-06", time.Date(2016, 11, 6, 0, 0, 0, 0, time.Local)},
		{"yy-MM-dd", "16-11-06", time.Date(2016, 11, 6, 0, 0, 0, 0, time.Local)},
		{"yy/MM/dd HH:mm:ss", "16/11/06 12:17:53", time.Date(2016, 11, 6, 12, 17, 53, 0, time.Local)},
		{"カンマ区切りミリ秒", "2016-11-06 12:17:53,985", time.Date(2016, 11, 6, 12, 17, 53, 985e6, time.Local)},
		{"Tセパレータ+ミリ秒なし", "2016-11-06T12:17:53", time.Date(2016, 11, 6, 12, 17, 53, 0, time.Local)},
		{"yyyyMMdd", "20161106", time.Date(2016, 11, 6, 0, 0, 0, 0, time.Local)},
		{"yyyyMMdd HH:mm:ss", "20161106 12:17:53", time.Date(2016, 11, 6, 12, 17, 53, 0, time.Local)},
		{"ミリ秒2桁は右0埋め", "2016-11-06 12:17:53.98", time.Date(2016, 11, 6, 12, 17, 53, 980e6, time.Local)},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := Parse(tt.in)
			if err != nil {
				t.Fatalf("Parse(%q) error = %v", tt.in, err)
			}
			if !got.Equal(tt.want) {
				t.Errorf("Parse(%q) = %v, want %v", tt.in, got, tt.want)
			}
		})
	}
}

func TestParse_ISO8601(t *testing.T) {
	jst := time.FixedZone("+0900", 9*60*60)
	tests := []struct {
		name string
		in   string
		want time.Time
	}{
		{"ミリ秒あり", "2016-11-06T12:17:53.985+0900", time.Date(2016, 11, 6, 12, 17, 53, 985e6, jst)},
		{"ミリ秒カンマ区切り", "2016-11-06T12:17:53,985+0900", time.Date(2016, 11, 6, 12, 17, 53, 985e6, jst)},
		{"ミリ秒なし", "2016-11-06T12:17:53+0900", time.Date(2016, 11, 6, 12, 17, 53, 0, jst)},
		{"末尾コロン", "2016-11-06T12:17:53.985+0900:", time.Date(2016, 11, 6, 12, 17, 53, 985e6, jst)},
		{"コロン区切りタイムゾーン", "2016-11-06T12:17:53.985+09:00", time.Date(2016, 11, 6, 12, 17, 53, 985e6, jst)},
		{"UTC", "2016-11-06T12:17:53.985Z", time.Date(2016, 11, 6, 12, 17, 53, 985e6, time.UTC)},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			got, err := Parse(tt.in)
			if err != nil {
				t.Fatalf("Parse(%q) error = %v", tt.in, err)
			}
			if !got.Equal(tt.want) {
				t.Errorf("Parse(%q) = %v, want %v", tt.in, got, tt.want)
			}
		})
	}
}

func TestParse_ApacheAccessLog(t *testing.T) {
	jst := time.FixedZone("+0900", 9*60*60)
	got, err := Parse("[10/Oct/2000:13:55:36 +0900]")
	if err != nil {
		t.Fatalf("Parse error = %v", err)
	}
	want := time.Date(2000, 10, 10, 13, 55, 36, 0, jst)
	if !got.Equal(want) {
		t.Errorf("Parse = %v, want %v", got, want)
	}
}

func TestParse_Error(t *testing.T) {
	tests := []struct {
		name string
		in   string
	}{
		{"空文字", ""},
		{"変換できない文字列", "unmatched date format"},
		{"存在しない月", "9999-13-01 00:00:00.000"},
		{"存在しない日", "2016-02-30"},
		{"時刻範囲外", "2016-11-06 25:00:00"},
		{"8文字未満", "2016-1"},
		{"トークン過多", "2016-11-06 12:17:53.985.111"},
		{"数値でない", "aaaa-bb-cc"},
		{"IPアドレス", "127.0.0.1"},
	}
	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got, err := Parse(tt.in); err == nil {
				t.Errorf("Parse(%q) = %v, want error", tt.in, got)
			}
		})
	}
}
