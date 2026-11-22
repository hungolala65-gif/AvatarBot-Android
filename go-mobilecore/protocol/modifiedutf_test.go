package protocol

import (
	"bytes"
	"errors"
	"strings"
	"testing"
)

func TestJavaModifiedUTFEncoding(t *testing.T) {
	cases := []struct {
		input    string
		expected []byte
	}{
		{"", []byte{0, 0}},
		{"A", []byte{0, 1, 0x41}},
		{"a\x00b", []byte{0, 4, 0x61, 0xc0, 0x80, 0x62}},
		{"é", []byte{0, 2, 0xc3, 0xa9}},
		{"😀", []byte{0, 6, 0xed, 0xa0, 0xbd, 0xed, 0xb8, 0x80}},
	}
	for _, tc := range cases {
		got, err := EncodeModifiedUTF(tc.input)
		if err != nil || !bytes.Equal(got, tc.expected) {
			t.Fatalf("%q got=%x expected=%x err=%v", tc.input, got, tc.expected, err)
		}
		value, err := DecodeModifiedUTF(bytes.NewReader(got))
		if err != nil || value != tc.input {
			t.Fatalf("%q got=%q err=%v", tc.input, value, err)
		}
	}
}
func TestInvalidJavaModifiedUTF(t *testing.T) {
	for _, b := range [][]byte{
		{0, 1, 0}, {0, 1, 0xff}, {0, 2, 0xc2, 0x00}, {0, 3, 0xe0, 0x81, 0x81}, {0, 2, 0xc1, 0x81},
	} {
		_, err := DecodeModifiedUTF(bytes.NewReader(b))
		if !errors.Is(err, ErrInvalidModifiedUTF) {
			t.Fatalf("accepted malformed %x: %v", b, err)
		}
	}
	_, err := EncodeModifiedUTF(strings.Repeat("中", 22000))
	if !errors.Is(err, ErrPayloadTooLarge) {
		t.Fatal(err)
	}
}
