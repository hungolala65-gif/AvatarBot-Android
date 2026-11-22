// Package protocol implements the recovered three-byte wire framing of the
// supplied AvatarBot PC binary. It does NOT contain handshake/login opcodes.
package protocol

import (
	"encoding/binary"
	"errors"
	"io"
)

const HeaderBytes = 3
const MaxPayload = 65535

var ErrPayloadTooLarge = errors.New("frame payload exceeds 16-bit limit")
var ErrFrameLimit = errors.New("frame exceeds configured length limit")

// XORState stores independent inbound/outbound positions in a shared XOR key.
// Decompiled PC routines refer to separate read and write byte positions.
// SetKey must be called only after a trusted protocol handshake verifies the
// actual key; never assume a hardcoded cipher for production connections.
type XORState struct {
	key         []byte
	readOffset  int
	writeOffset int
}

func NewXORState(key []byte) *XORState { return &XORState{key: append([]byte(nil), key...)} }
func (s *XORState) xor(data []byte, inbound bool) {
	if s == nil || len(s.key) == 0 {
		return
	}
	index := s.writeOffset
	if inbound {
		index = s.readOffset
	}
	for i := range data {
		data[i] ^= s.key[index]
		index++
		if index == len(s.key) {
			index = 0
		}
	}
	if inbound {
		s.readOffset = index
	} else {
		s.writeOffset = index
	}
}

// EncodeFrame builds TYPE (1 byte) + BODY LENGTH (2 bytes, big endian) + BODY.
// This shape is corroborated by static disassembly of internal/proto.EncodeFrame
// from the provided Linux/amd64 binary; encrypted-handshake correctness is not.
func EncodeFrame(kind byte, payload []byte, cipher *XORState) ([]byte, error) {
	if len(payload) > MaxPayload {
		return nil, ErrPayloadTooLarge
	}
	out := make([]byte, HeaderBytes+len(payload))
	out[0] = kind
	binary.BigEndian.PutUint16(out[1:3], uint16(len(payload)))
	copy(out[3:], payload)
	cipher.xor(out, false)
	return out, nil
}

// DecodeFrame validates framing and enforces the caller's maximum body size.
// Never call concurrently on the same cipher or stream; per-session locking
// belongs in the future authenticated game session adapter.
func DecodeFrame(reader io.Reader, cipher *XORState, limit int) (byte, []byte, error) {
	if limit <= 0 || limit > MaxPayload {
		limit = MaxPayload
	}
	var header [HeaderBytes]byte
	if _, err := io.ReadFull(reader, header[:]); err != nil {
		return 0, nil, err
	}
	cipher.xor(header[:], true)
	kind := header[0]
	bodyLength := int(binary.BigEndian.Uint16(header[1:]))
	if bodyLength > limit {
		return 0, nil, ErrFrameLimit
	}
	body := make([]byte, bodyLength)
	if _, err := io.ReadFull(reader, body); err != nil {
		return 0, nil, err
	}
	cipher.xor(body, true)
	return kind, body, nil
}
