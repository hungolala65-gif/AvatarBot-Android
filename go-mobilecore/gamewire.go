package mobilecore

import (
	"context"
	"errors"
	"io"
	"net"
	"sync"
	"time"

	"avatarbot/mobilecore/protocol"
)

// gameWire is a connection layer awaiting the original game login/handshake.
// It does not send anything until a caller explicitly sends a verified frame.
// Keep unexported: exposing arbitrary game packets to UI is undesirable.
type gameWire struct {
	conn      net.Conn
	outgoing  *protocol.XORState
	incoming  *protocol.XORState
	receiveMu sync.Mutex
	sendMu    sync.Mutex
	closeOnce sync.Once
	shutdown  chan struct{}
}

func newGameWire(ctx context.Context, endpoint, proxyURI string) (*gameWire, error) {
	if err := ctx.Err(); err != nil {
		return nil, err
	}
	conn, err := dialEndpoint(endpoint, proxyURI, 6*time.Second)
	if err != nil {
		return nil, err
	}
	if err := ctx.Err(); err != nil {
		conn.Close()
		return nil, err
	}
	w := &gameWire{conn: conn, shutdown: make(chan struct{})}
	go func() {
		select {
		case <-ctx.Done():
			w.close()
		case <-w.shutdown:
		}
	}()
	return w, nil
}

// enableCipher must be called only after verifying the original handshake.
// The key is deliberately not persisted or logged by the wire layer.
func (w *gameWire) enableCipher(key []byte) error {
	if len(key) == 0 {
		return errors.New("empty game cipher key")
	}
	w.sendMu.Lock()
	defer w.sendMu.Unlock()
	w.receiveMu.Lock()
	defer w.receiveMu.Unlock()
	w.outgoing = protocol.NewXORState(key)
	w.incoming = protocol.NewXORState(key)
	return nil
}
func (w *gameWire) send(kind byte, payload []byte) error {
	w.sendMu.Lock()
	defer w.sendMu.Unlock()
	framed, err := protocol.EncodeFrame(kind, payload, w.outgoing)
	if err != nil {
		return err
	}
	if err := w.conn.SetWriteDeadline(time.Now().Add(10 * time.Second)); err != nil {
		return err
	}
	defer w.conn.SetWriteDeadline(time.Time{})
	for len(framed) > 0 {
		n, err := w.conn.Write(framed)
		if err != nil {
			return err
		}
		if n == 0 {
			return io.ErrShortWrite
		}
		framed = framed[n:]
	}
	return nil
}
func (w *gameWire) receive() (byte, []byte, error) {
	w.receiveMu.Lock()
	defer w.receiveMu.Unlock()
	// IO ends when session context is canceled (watcher closes conn).
	return protocol.DecodeFrame(w.conn, w.incoming, protocol.MaxPayload)
}
func (w *gameWire) close() {
	w.closeOnce.Do(func() {
		if w.shutdown != nil {
			close(w.shutdown)
		}
		_ = w.conn.Close()
	})
}
