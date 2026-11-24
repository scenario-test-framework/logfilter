// Package structured は JSON Lines / logfmt のイベント先頭行を解析します。
package structured

import (
	"encoding/json"
	"fmt"
	"strings"
	"time"
)

// Record は構造化ログ1行分のフィールドです。数値は元の10進表現を保持します。
type Record map[string]any

// Parse は行全体を指定形式で解析します。継続行として扱う場合は nil を返します。
func Parse(format, line string) Record {
	switch format {
	case "json":
		line = strings.TrimSpace(line)
		decoder := json.NewDecoder(strings.NewReader(line))
		decoder.UseNumber()
		var r Record
		if err := decoder.Decode(&r); err != nil || decoder.InputOffset() != int64(len(line)) {
			return nil
		}
		return r
	case "logfmt":
		return parseLogfmt(line)
	}
	return nil
}

// lookup はドットを含むキー名の完全一致を優先し、次に短い接頭辞から各階層を探索します。
func (r Record) lookup(path string) (any, bool) {
	if value, ok := r[path]; ok {
		return value, true
	}
	for i, ch := range path {
		if ch != '.' {
			continue
		}
		child, ok := r[path[:i]].(map[string]any)
		if !ok {
			continue
		}
		if value, found := Record(child).lookup(path[i+1:]); found {
			return value, true
		}
	}
	return nil, false
}

// field は指定キー、または最初に存在する候補キーの値を返します。
func (r Record) field(key string, candidates ...string) (string, any, bool) {
	if key != "" {
		v, ok := r.lookup(key)
		return key, v, ok
	}
	for _, k := range candidates {
		if v, ok := r[k]; ok {
			return k, v, true
		}
	}
	return "", nil, false
}

// Timestamp はイベントの日時を返します。キーが無い継続行と、値が不正な行を区別します。
func (r Record) Timestamp(key, format string) (time.Time, bool, error) {
	key, value, found := r.field(key, "time", "timestamp", "ts", "@timestamp")
	if !found {
		return time.Time{}, false, nil
	}
	if ts, ok := parseTimestamp(value, format); ok {
		return ts, true, nil
	}
	display, ok := value.(string)
	if !ok {
		data, _ := json.Marshal(value)
		display = string(data)
	}
	return time.Time{}, true, fmt.Errorf("タイムスタンプを日時に変換できません。 キー=%s 設定値=%s", key, display)
}

// Level は先頭行のレベルを返します。文字列・数値以外の場合は本文での判定に戻します。
func (r Record) Level(key string) (string, bool) {
	_, value, _ := r.field(key, "level", "severity", "lvl")
	switch v := value.(type) {
	case string:
		return v, true
	case json.Number:
		return v.String(), true
	}
	return "", false
}
