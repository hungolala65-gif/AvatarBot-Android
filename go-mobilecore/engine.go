// Package mobilecore is a newly written Android-bindable AvatarBot foundation.
// It is NOT the PC binary's original (unavailable) Go source code.
package mobilecore

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"strings"
	"sync"
	"time"
)

var ErrGameProtocolUnavailable = errors.New("game protocol not implemented: original session/proto/farmer modules are required")

type Account struct {
	ID       string      `json:"id"`
	Username string      `json:"username"`
	City     string      `json:"city"`
	Enabled  bool        `json:"enabled"`
	Options  FarmOptions `json:"options"`
}

// worker must be implemented from source-compatible game session/protocol code.
// The provided binary-only ZIP contains no implementation of this interface.
type worker interface {
	Run(ctx context.Context, account Account) error
}

type Engine struct {
	lifecycle sync.Mutex
	mu        sync.Mutex
	accounts  []Account
	logs      []string
	updated   time.Time
	worker    worker
	running   bool
	cancel    context.CancelFunc
	wg        sync.WaitGroup
}

func NewEngine() *Engine {
	e := &Engine{updated: time.Now(), worker: newGameWorker()}
	e.logLocked("Go Mobile Core khởi tạo; game protocol chưa tích hợp")
	return e
}

// SetAccountsJSON only accepts non-secret account metadata, and must be called
// while stopped. Future real game login requires a separate reviewed API.
func (e *Engine) SetAccountsJSON(raw string) error {
	if len(raw) > 2*1024*1024 {
		return errors.New("account config too large")
	}
	var parsed []Account
	dec := json.NewDecoder(strings.NewReader(raw))
	dec.DisallowUnknownFields() // reject accidental password/token fields
	if err := dec.Decode(&parsed); err != nil {
		return fmt.Errorf("invalid account JSON: %w", err)
	}
	var extra any
	if err := dec.Decode(&extra); err != io.EOF {
		return errors.New("unexpected trailing JSON")
	}
	if parsed == nil {
		return errors.New("expected JSON array")
	}
	if len(parsed) > 1000 {
		return errors.New("too many accounts")
	}
	seen := map[string]bool{}
	for _, a := range parsed {
		if strings.TrimSpace(a.ID) == "" || strings.TrimSpace(a.Username) == "" {
			return errors.New("account ID and username are required")
		}
		if len(a.ID) > 128 || len(a.Username) > 256 {
			return errors.New("account field too long")
		}
		if seen[a.ID] {
			return errors.New("duplicate account ID")
		}
		seen[a.ID] = true
		if a.City != "hoanmy" && a.City != "dieuky" {
			return errors.New("unsupported city")
		}
	}
	e.mu.Lock()
	defer e.mu.Unlock()
	if e.running {
		return errors.New("stop running bot before changing accounts")
	}
	e.accounts = append([]Account(nil), parsed...)
	e.updated = time.Now()
	e.logLocked(fmt.Sprintf("Đã nạp cấu hình %d tài khoản (không gồm mật khẩu)", len(parsed)))
	return nil
}

func (e *Engine) Start() error {
	e.lifecycle.Lock()
	defer e.lifecycle.Unlock()
	e.mu.Lock()
	if e.running {
		e.mu.Unlock()
		return nil
	}
	if e.worker == nil {
		e.logLocked("Không thể khởi chạy auto: chưa có session/protocol game")
		e.mu.Unlock()
		return ErrGameProtocolUnavailable
	}
	active := make([]Account, 0, len(e.accounts))
	for _, a := range e.accounts {
		if a.Enabled {
			active = append(active, a)
		}
	}
	if len(active) == 0 {
		e.mu.Unlock()
		return errors.New("no enabled accounts")
	}
	// A phone should not create hundreds of simultaneous TCP sessions by accident.
	if len(active) > 64 {
		e.mu.Unlock()
		return errors.New("too many active accounts (max 64 on Android)")
	}
	ctx, cancel := context.WithCancel(context.Background())
	e.cancel = cancel
	e.running = true
	e.logLocked(fmt.Sprintf("Đã bắt đầu %d worker", len(active)))
	e.wg.Add(len(active))
	e.mu.Unlock()
	for _, a := range active {
		go e.workerLoop(ctx, a)
	}
	return nil
}

func (e *Engine) workerLoop(ctx context.Context, a Account) {
	defer e.wg.Done()
	backoff := 3 * time.Second
	for {
		if ctx.Err() != nil {
			return
		}
		began := time.Now()
		err := e.invokeWorker(ctx, a)
		if ctx.Err() != nil {
			return
		}
		e.mu.Lock()
		if err == nil {
			e.logLocked("Worker kết thúc; chuẩn bị kết nối lại")
		} else {
			e.logLocked("Worker gặp lỗi; sẽ thử kết nối lại")
		}
		e.mu.Unlock()
		// Reset backoff after a stable session; cap retry sleep to 60 seconds.
		if time.Since(began) > 2*time.Minute {
			backoff = 3 * time.Second
		}
		select {
		case <-ctx.Done():
			return
		case <-time.After(backoff):
		}
		if backoff < 60*time.Second {
			backoff *= 2
			if backoff > 60*time.Second {
				backoff = 60 * time.Second
			}
		}
	}
}

// A broken adapter must not crash the entire Android application process.
// Never expose panic strings; they may include passwords or tokens.
func (e *Engine) invokeWorker(ctx context.Context, a Account) (err error) {
	defer func() {
		if recover() != nil {
			err = errors.New("worker failure")
		}
	}()
	return e.worker.Run(ctx, a)
}

func (e *Engine) Stop() {
	e.lifecycle.Lock()
	defer e.lifecycle.Unlock()
	e.mu.Lock()
	if !e.running {
		e.mu.Unlock()
		return
	}
	cancel := e.cancel
	e.mu.Unlock()
	cancel()
	e.wg.Wait() // worker.Run must respect ctx; see INTEGRATION.md
	e.mu.Lock()
	e.running = false
	e.cancel = nil
	e.logLocked("Đã dừng toàn bộ worker")
	e.mu.Unlock()
}

func (e *Engine) CapabilitiesJSON() string {
	e.mu.Lock()
	hasProtocol := e.worker != nil
	e.mu.Unlock()
	s := struct {
		NativeGo       bool `json:"nativeGo"`
		NetworkProbe   bool `json:"networkProbe"`
		FarmPlanning   bool `json:"farmPlanning"`
		GameAutomation bool `json:"gameAutomation"`
		Farm           bool `json:"farm"`
		Fishing        bool `json:"fishing"`
		Teacher        bool `json:"teacher"`
		Diamond        bool `json:"diamond"`
	}{true, true, true, hasProtocol, false, false, false, false}
	b, _ := json.Marshal(s)
	return string(b)
}

func (e *Engine) StateJSON() string {
	e.mu.Lock()
	defer e.mu.Unlock()
	status := "protocol_missing"
	if e.worker != nil {
		status = "idle"
	}
	if e.running {
		status = "running"
	}
	s := struct {
		Status            string   `json:"status"`
		Accounts          int      `json:"accounts"`
		ProtocolAvailable bool     `json:"protocolAvailable"`
		Logs              []string `json:"logs"`
	}{status, len(e.accounts), e.worker != nil, append([]string(nil), e.logs...)}
	b, _ := json.Marshal(s)
	return string(b)
}

func (e *Engine) logLocked(message string) {
	e.logs = append(e.logs, time.Now().Format("15:04:05")+" "+message)
	if len(e.logs) > 150 {
		e.logs = append([]string(nil), e.logs[len(e.logs)-150:]...)
	}
}
