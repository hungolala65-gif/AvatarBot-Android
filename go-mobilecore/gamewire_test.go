package mobilecore

import (
	"bytes"
	"context"
	"errors"
	"net"
	"testing"
	"time"
)

func TestGameWirePlainConnection(t *testing.T) {
	left, right := net.Pipe()
	defer left.Close()
	defer right.Close()
	w := &gameWire{conn: left, shutdown: make(chan struct{})}
	done := make(chan error, 1)
	go func() { done <- w.send(11, []byte{1, 2, 3}) }()
	var raw [6]byte
	_ = right.SetReadDeadline(time.Now().Add(2 * time.Second))
	_, err := right.Read(raw[:])
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(raw[:], []byte{11, 0, 3, 1, 2, 3}) {
		t.Fatalf("%x", raw)
	}
	if err := <-done; err != nil {
		t.Fatal(err)
	}
	go func() { _, _ = right.Write([]byte{21, 0, 2, 8, 9}) }()
	kind, body, err := w.receive()
	if err != nil || kind != 21 || !bytes.Equal(body, []byte{8, 9}) {
		t.Fatalf("%d %x %v", kind, body, err)
	}
	w.close()
}
func TestWireCancellation(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		conn, e := listener.Accept()
		if e == nil {
			defer conn.Close()
			<-time.After(time.Second)
		}
	}()
	ctx, cancel := context.WithCancel(context.Background())
	w, err := newGameWire(ctx, listener.Addr().String(), "")
	if err != nil {
		t.Fatal(err)
	}
	done := make(chan error, 1)
	go func() { _, _, e := w.receive(); done <- e }()
	cancel()
	select {
	case err := <-done:
		if err == nil {
			t.Fatal("expected read to stop")
		}
	case <-time.After(2 * time.Second):
		t.Fatal("read did not cancel")
	}
	w.close() // idempotent
	ctx2, cancel2 := context.WithCancel(context.Background())
	cancel2()
	_, err = newGameWire(ctx2, listener.Addr().String(), "")
	if !errors.Is(err, context.Canceled) {
		t.Fatal(err)
	}
}
