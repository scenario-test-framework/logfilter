package charset

import "testing"

func TestLookup(t *testing.T) {
	tests := []string{
		"UTF-8", "utf8",
		"Shift_JIS", "sjis", "MS932", "Windows-31J",
		"EUC-JP", "euc_jp",
		"ISO-2022-JP",
		"UTF-16BE", "UTF-16LE",
	}
	for _, name := range tests {
		t.Run(name, func(t *testing.T) {
			if _, err := Lookup(name); err != nil {
				t.Errorf("Lookup(%q) error = %v", name, err)
			}
		})
	}
}

func TestLookup_Error(t *testing.T) {
	for _, name := range []string{"", "NotExist", "NotExist2"} {
		t.Run(name, func(t *testing.T) {
			if _, err := Lookup(name); err == nil {
				t.Errorf("Lookup(%q) should return error", name)
			}
		})
	}
}
