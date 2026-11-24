package structured

import (
	"encoding/json"
	"reflect"
	"testing"
	"time"
)

func TestLogfmtRecord(t *testing.T) {
	got := Parse("logfmt", "  "+`log.ts=123 msg="日本語 \"引用\" \\ \n\t" empty= bare`+"\t"+`next=ok`)
	want := Record{"log.ts": "123", "msg": "日本語 \"引用\" \\ \n\t", "empty": "", "next": "ok"}
	if !reflect.DeepEqual(got, want) {
		t.Errorf("Parse = %#v, want %#v", got, want)
	}
	if got := Parse("logfmt", `msg="a\rb" eq="a=b"`); got["msg"] != "a\rb" || got["eq"] != "a=b" {
		t.Errorf("Parse(escape/quoted =) = %#v", got)
	}
	for _, line := range []string{`time=1 msg="unclosed`, `time=1 msg="ok"tail`, `=value`, `bad"key=1`, `time=1 broken=a=b`} {
		if got := Parse("logfmt", line); got != nil {
			t.Errorf("Parse(%q) = %#v, want nil", line, got)
		}
	}
}

func TestJSONRecord(t *testing.T) {
	got := Parse("json", " \t"+`{"ts":1704067200123456789}`+"  ")
	if got["ts"] != json.Number("1704067200123456789") {
		t.Errorf("ts = %#v", got["ts"])
	}
	for _, line := range []string{`{"ts":1} tail`, `{"ts":1} {}`, `[]`, `null`, `"time"`, `{"ts":`} {
		if got := Parse("json", line); got != nil {
			t.Errorf("Parse(%q) = %#v, want nil", line, got)
		}
	}
}

func TestLookup(t *testing.T) {
	record := Parse("json", `{"log.ts":1,"log":{"ts":2,"inner.ts":3,"inner":{"ts":4}},"log.inner":{"ts":5,"id":6},"empty":null}`)
	for _, tt := range []struct {
		key   string
		want  any
		found bool
	}{
		{"log.ts", json.Number("1"), true},
		{"log.inner.ts", json.Number("3"), true},
		{"log.inner.id", json.Number("6"), true},
		{"empty", nil, true},
		{"missing", nil, false},
		{"log.ts.x", nil, false},
	} {
		if got, ok := record.lookup(tt.key); got != tt.want || ok != tt.found {
			t.Errorf("lookup(%q) = (%v, %t), want (%v, %t)", tt.key, got, ok, tt.want, tt.found)
		}
	}
}

func TestTimestampAndLevelKeys(t *testing.T) {
	record := Parse("json", `{"time":1,"timestamp":2,"ts":3,"@timestamp":4,"level":"INFO","severity":"ERROR","lvl":40,"log":{"ts":5,"level":41}}`)
	for _, tt := range []struct {
		key     string
		seconds int64
	}{{"", 1}, {"timestamp", 2}, {"log.ts", 5}} {
		got, ok, err := record.Timestamp(tt.key, "")
		if err != nil || !ok || !got.Equal(time.Unix(tt.seconds, 0)) {
			t.Errorf("Timestamp(%q) = %v, %t, %v", tt.key, got, ok, err)
		}
	}
	if _, found, err := record.Timestamp("absent", "auto"); err != nil || found {
		t.Errorf("Timestamp(absent) found = %t, err = %v", found, err)
	}
	for _, tt := range []struct{ key, want string }{{"", "INFO"}, {"lvl", "40"}, {"log.level", "41"}} {
		if got, ok := record.Level(tt.key); !ok || got != tt.want {
			t.Errorf("Level(%q) = %q, %t", tt.key, got, ok)
		}
	}
	// 既定候補の順序: 先頭の候補を順に除いても、残った中で決められた順に選ばれること
	for _, tt := range []struct {
		json    string
		seconds int64
	}{
		{`{"@timestamp":4,"ts":3,"timestamp":2}`, 2},
		{`{"@timestamp":4,"ts":3}`, 3},
		{`{"@timestamp":4}`, 4},
	} {
		got, ok, err := Parse("json", tt.json).Timestamp("", "")
		if err != nil || !ok || !got.Equal(time.Unix(tt.seconds, 0)) {
			t.Errorf("Timestamp(%s) = %v, %t, %v, want %d", tt.json, got, ok, err, tt.seconds)
		}
	}
	for _, tt := range []struct{ json, want string }{
		{`{"lvl":"WARN","severity":"ERROR"}`, "ERROR"},
		{`{"lvl":"WARN"}`, "WARN"},
	} {
		if got, ok := Parse("json", tt.json).Level(""); !ok || got != tt.want {
			t.Errorf("Level(%s) = %q, %t, want %q", tt.json, got, ok, tt.want)
		}
	}
	for _, value := range []string{"null", "true", "[]", "{}"} {
		r := Parse("json", `{"severity":"ERROR","level":`+value+"}")
		if _, ok := r.Level(""); ok {
			t.Errorf("Level should fall back for %s", value)
		}
	}
}
