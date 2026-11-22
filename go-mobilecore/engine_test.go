package mobilecore

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net"
	"strings"
	"testing"
	"time"
)

func TestCapabilitiesAreTruthful(t *testing.T) {
	e := NewEngine()
	var caps map[string]bool
	if err := json.Unmarshal([]byte(e.CapabilitiesJSON()), &caps); err != nil {
		t.Fatal(err)
	}
	if caps["gameAutomation"] || !caps["networkProbe"] {
		t.Fatalf("unexpected caps: %v", caps)
	}
	if err := e.Start(); err != ErrGameProtocolUnavailable {
		t.Fatalf("Start must fail closed: %v", err)
	}
}

func TestAccountValidation(t *testing.T) {
	e := NewEngine()
	for _, raw := range []string{`{}`, `[{"id":"1","username":"a","city":"unknown"}]`, `[{"id":"1","username":"a","city":"hoanmy"},{"id":"1","username":"b","city":"dieuky"}]`} {
		if err := e.SetAccountsJSON(raw); err == nil {
			t.Fatalf("expected error for %s", raw)
		}
	}
	if err := e.SetAccountsJSON(`[{"id":"1","username":"tester","city":"hoanmy","enabled":true}]`); err != nil {
		t.Fatal(err)
	}
	var state struct {
		Accounts int `json:"accounts"`
	}
	_ = json.Unmarshal([]byte(e.StateJSON()), &state)
	if state.Accounts != 1 {
		t.Fatal(state)
	}
}

func TestDirectTCPProbe(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	go func() {
		c, e := listener.Accept()
		if e == nil {
			_ = c.Close()
		}
	}()
	e := NewEngine()
	var res struct {
		OK       bool `json:"ok"`
		ViaProxy bool `json:"viaProxy"`
	}
	_ = json.Unmarshal([]byte(e.ProbeJSON(listener.Addr().String(), "")), &res)
	if !res.OK || res.ViaProxy {
		t.Fatal(res)
	}
}

func TestSOCKS5Probe(t *testing.T) {
	listener, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatal(err)
	}
	defer listener.Close()
	done := make(chan struct{})
	go func() {
		defer close(done)
		c, e := listener.Accept()
		if e != nil {
			return
		}
		defer c.Close()
		hdr := make([]byte, 3)
		_, _ = io.ReadFull(c, hdr)
		_, _ = c.Write([]byte{5, 0})
		req := make([]byte, 4)
		_, _ = io.ReadFull(c, req)
		if req[3] == 3 {
			size := []byte{0}
			_, _ = io.ReadFull(c, size)
			_, _ = io.CopyN(io.Discard, c, int64(size[0])+2)
		} else if req[3] == 1 {
			_, _ = io.CopyN(io.Discard, c, 6)
		}
		_, _ = c.Write([]byte{5, 0, 0, 1, 127, 0, 0, 1, 0, 80})
	}()
	e := NewEngine()
	raw := e.ProbeJSON("avatar.example:19128", "socks5://"+listener.Addr().String())
	var res struct {
		OK       bool `json:"ok"`
		ViaProxy bool `json:"viaProxy"`
	}
	if err := json.Unmarshal([]byte(raw), &res); err != nil {
		t.Fatal(err)
	}
	if !res.OK || !res.ViaProxy {
		t.Fatal(raw)
	}
	<-done
}

type testWorker struct{ entered chan struct{} }

func (w *testWorker) Run(ctx context.Context, _ Account) error {
	select {
	case w.entered <- struct{}{}:
	default:
	}
	<-ctx.Done()
	return ctx.Err()
}

func TestWorkerStartStopLifecycle(t *testing.T) {
	e := NewEngine()
	if err := e.SetAccountsJSON(`[{"id":"1","username":"tester","city":"hoanmy","enabled":true}]`); err != nil {
		t.Fatal(err)
	}
	w := &testWorker{entered: make(chan struct{}, 1)}
	e.worker = w
	if err := e.Start(); err != nil {
		t.Fatal(err)
	}
	select {
	case <-w.entered:
	case <-time.After(time.Second):
		t.Fatal("worker never started")
	}
	e.Stop()
	var state struct {
		Status string `json:"status"`
	}
	_ = json.Unmarshal([]byte(e.StateJSON()), &state)
	if state.Status != "idle" {
		t.Fatal(state)
	}
}

func TestRejectsCredentialsInMetadata(t *testing.T) {
	e := NewEngine()
	if err := e.SetAccountsJSON(`[{"id":"1","username":"a","password":"secret","city":"hoanmy"}]`); err == nil {
		t.Fatal("credentials must not enter metadata JSON")
	}
}

type panicWorker struct{}

func (panicWorker) Run(context.Context, Account) error { panic("supersecret-password") }

func TestPanicRedacted(t *testing.T) {
	e := NewEngine()
	e.worker = panicWorker{}
	err := e.invokeWorker(context.Background(), Account{ID: "a"})
	if err == nil || strings.Contains(err.Error(), "supersecret") {
		t.Fatal("worker secret leaked")
	}
}

func TestMoreThan64ActiveAccountsRejected(t *testing.T) {
	e := NewEngine()
	e.worker = &testWorker{entered: make(chan struct{}, 1)}
	var accounts []Account
	for i := 0; i < 65; i++ {
		accounts = append(accounts, Account{ID: fmt.Sprint(i), Username: "u", City: "hoanmy", Enabled: true})
	}
	payload, _ := json.Marshal(accounts)
	if err := e.SetAccountsJSON(string(payload)); err != nil {
		t.Fatal(err)
	}
	if err := e.Start(); err == nil {
		e.Stop()
		t.Fatal("expected max-worker limit")
	}
}
