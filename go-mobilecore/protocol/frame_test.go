package protocol

import (
	"bytes"
	"errors"
	"io"
	"testing"
)

func TestPlainWireFormat(t *testing.T) {
	got, err := EncodeFrame(0x2a, []byte{0x10, 0x20, 0x30}, nil)
	if err != nil {
		t.Fatal(err)
	}
	want := []byte{0x2a, 0, 3, 0x10, 0x20, 0x30}
	if !bytes.Equal(got, want) {
		t.Fatalf("%x != %x", got, want)
	}
	kind, data, err := DecodeFrame(bytes.NewReader(got), nil, 100)
	if err != nil || kind != 0x2a || !bytes.Equal(data, want[3:]) {
		t.Fatalf("kind=%x data=%x err=%v", kind, data, err)
	}
}
func TestCipherOffsetsAndMultipleFrames(t *testing.T) {
	writer := NewXORState([]byte{0xaa, 0x31, 0xff, 0x02})
	reader := NewXORState([]byte{0xaa, 0x31, 0xff, 0x02})
	var wire bytes.Buffer
	for _, p := range [][]byte{{1, 2, 3, 4}, {5}, {}, {6, 7, 8}} {
		enc, err := EncodeFrame(7, p, writer)
		if err != nil {
			t.Fatal(err)
		}
		wire.Write(enc)
	}
	for _, p := range [][]byte{{1, 2, 3, 4}, {5}, {}, {6, 7, 8}} {
		kind, decoded, err := DecodeFrame(&wire, reader, 65535)
		if err != nil || kind != 7 || !bytes.Equal(decoded, p) {
			t.Fatalf("got %d %x %v", kind, decoded, err)
		}
	}
}
func TestInvalidFrames(t *testing.T) {
	_, err := EncodeFrame(0, make([]byte, 65536), nil)
	if !errors.Is(err, ErrPayloadTooLarge) {
		t.Fatal(err)
	}
	_, _, err = DecodeFrame(bytes.NewReader([]byte{0, 0, 3, 1, 2, 3}), nil, 2)
	if !errors.Is(err, ErrFrameLimit) {
		t.Fatal(err)
	}
	_, _, err = DecodeFrame(bytes.NewReader([]byte{0, 0, 3, 1}), nil, 10)
	if !errors.Is(err, io.ErrUnexpectedEOF) {
		t.Fatal(err)
	}
}
func FuzzFrameRoundTrip(f *testing.F) {
	f.Add(byte(4), []byte{0, 1, 255}, []byte{5, 6, 7})
	f.Add(byte(0), []byte{}, []byte{})
	f.Fuzz(func(t *testing.T, kind byte, payload, key []byte) {
		if len(payload) > MaxPayload || len(key) > 10000 {
			t.Skip()
		}
		encoded, err := EncodeFrame(kind, payload, NewXORState(key))
		if err != nil {
			t.Fatal(err)
		}
		gotKind, gotPayload, err := DecodeFrame(bytes.NewReader(encoded), NewXORState(key), MaxPayload)
		if err != nil || gotKind != kind || !bytes.Equal(gotPayload, payload) {
			t.Fatalf("broken roundtrip")
		}
	})
}
