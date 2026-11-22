package protocol

import (
	"encoding/binary"
	"errors"
	"io"
	"unicode/utf16"
)

var ErrInvalidModifiedUTF = errors.New("invalid Java modified UTF-8")

// EncodeModifiedUTF produces Java DataOutputStream.writeUTF compatible bytes:
// two-byte big-endian *encoded byte count* and the modified UTF-8 payload.
// Go's UTF-8 representation is not directly compatible, notably for NUL
// and supplementary Unicode code points.
func EncodeModifiedUTF(value string) ([]byte, error) {
	runes := []rune(value)
	codes := utf16.Encode(runes)
	// Output cannot exceed Java's unsigned 16-bit byte-length prefix.
	data := make([]byte, 0, len(codes)*3)
	for _, u := range codes {
		switch {
		case u != 0 && u <= 0x7f:
			data = append(data, byte(u))
		case u <= 0x7ff:
			data = append(data, byte(0xc0|u>>6), byte(0x80|u&0x3f))
		default:
			data = append(data, byte(0xe0|u>>12), byte(0x80|(u>>6)&0x3f), byte(0x80|u&0x3f))
		}
		if len(data) > MaxPayload {
			return nil, ErrPayloadTooLarge
		}
	}
	packet := make([]byte, 2+len(data))
	binary.BigEndian.PutUint16(packet[:2], uint16(len(data)))
	copy(packet[2:], data)
	return packet, nil
}

// DecodeModifiedUTF decodes exactly the readUTF wire representation; it does
// not apply the game cipher or read an outer frame header.
func DecodeModifiedUTF(reader io.Reader) (string, error) {
	var hdr [2]byte
	if _, err := io.ReadFull(reader, hdr[:]); err != nil {
		return "", err
	}
	length := int(binary.BigEndian.Uint16(hdr[:]))
	data := make([]byte, length)
	if _, err := io.ReadFull(reader, data); err != nil {
		return "", err
	}
	codes := make([]uint16, 0, length)
	for i := 0; i < len(data); {
		b := data[i]
		switch {
		case b >= 1 && b <= 0x7f:
			codes = append(codes, uint16(b))
			i++
		case b&0xe0 == 0xc0:
			if i+1 >= len(data) || data[i+1]&0xc0 != 0x80 {
				return "", ErrInvalidModifiedUTF
			}
			v := uint16(b&0x1f)<<6 | uint16(data[i+1]&0x3f)
			if v < 0x80 && v != 0 {
				return "", ErrInvalidModifiedUTF
			}
			codes = append(codes, v)
			i += 2
		case b&0xf0 == 0xe0:
			if i+2 >= len(data) || data[i+1]&0xc0 != 0x80 || data[i+2]&0xc0 != 0x80 {
				return "", ErrInvalidModifiedUTF
			}
			v := uint16(b&0x0f)<<12 | uint16(data[i+1]&0x3f)<<6 | uint16(data[i+2]&0x3f)
			if v < 0x800 {
				return "", ErrInvalidModifiedUTF
			}
			codes = append(codes, v)
			i += 3
		default:
			return "", ErrInvalidModifiedUTF
		}
	}
	return string(utf16.Decode(codes)), nil
}
