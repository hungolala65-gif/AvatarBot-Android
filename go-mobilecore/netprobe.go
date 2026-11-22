package mobilecore

import (
	"encoding/binary"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net"
	"net/url"
	"strconv"
	"strings"
	"time"
)

// ProbeJSON tests the TCP transport (optionally through SOCKS5) but never logs in
// to the game. It does not prove that game packet negotiation is correct.
func (e *Engine) ProbeJSON(endpoint string, proxyURI string) string {
	type result struct {
		OK        bool   `json:"ok"`
		Endpoint  string `json:"endpoint"`
		ViaProxy  bool   `json:"viaProxy"`
		LatencyMS int64  `json:"latencyMs"`
		Error     string `json:"error,omitempty"`
	}
	r := result{Endpoint: endpoint, ViaProxy: strings.TrimSpace(proxyURI) != ""}
	start := time.Now()
	conn, err := dialEndpoint(endpoint, proxyURI, 6*time.Second)
	r.LatencyMS = time.Since(start).Milliseconds()
	if err != nil {
		r.Error = safeProbeError(err)
	} else {
		r.OK = true
		_ = conn.Close()
	}
	b, _ := json.Marshal(r)
	return string(b)
}

func safeProbeError(err error) string {
	// Do not expose proxy userinfo or password in errors.
	if err == nil {
		return ""
	}
	if errors.Is(err, errProxyAuth) {
		return "SOCKS5 proxy authentication failed"
	}
	if errors.Is(err, errProxyConnect) {
		return "SOCKS5 proxy connection rejected"
	}
	if errors.Is(err, errProxyProtocol) {
		return "SOCKS5 proxy protocol error"
	}
	if errors.Is(err, errBadAddress) {
		return "Invalid host/port or proxy address"
	}
	var nerr net.Error
	if errors.As(err, &nerr) && nerr.Timeout() {
		return "Connection timeout"
	}
	return "TCP connection failed (check host, proxy and network)"
}

var (
	errBadAddress    = errors.New("bad address")
	errProxyProtocol = errors.New("proxy protocol")
	errProxyAuth     = errors.New("proxy auth")
	errProxyConnect  = errors.New("proxy connect")
)

func dialEndpoint(endpoint, proxyURI string, timeout time.Duration) (net.Conn, error) {
	host, portStr, err := net.SplitHostPort(endpoint)
	if err != nil || host == "" {
		return nil, errBadAddress
	}
	port64, err := strconv.ParseUint(portStr, 10, 16)
	if err != nil || port64 == 0 {
		return nil, errBadAddress
	}
	if strings.TrimSpace(proxyURI) == "" {
		return net.DialTimeout("tcp", endpoint, timeout)
	}
	u, err := url.Parse(proxyURI)
	if err != nil || u.Scheme != "socks5" || u.Host == "" || u.Path != "" || u.RawQuery != "" {
		return nil, errBadAddress
	}
	if _, _, err := net.SplitHostPort(u.Host); err != nil {
		return nil, errBadAddress
	}
	conn, err := net.DialTimeout("tcp", u.Host, timeout)
	if err != nil {
		return nil, err
	}
	ok := false
	defer func() {
		if !ok {
			_ = conn.Close()
		}
	}()
	_ = conn.SetDeadline(time.Now().Add(timeout))
	methods := []byte{5, 1, 0}
	username := ""
	password := ""
	if u.User != nil {
		username = u.User.Username()
		password, _ = u.User.Password()
		if len(username) == 0 || len(username) > 255 || len(password) > 255 {
			return nil, errBadAddress
		}
		methods = []byte{5, 2, 0, 2}
	}
	if _, err = conn.Write(methods); err != nil {
		return nil, err
	}
	reply := make([]byte, 2)
	if _, err = io.ReadFull(conn, reply); err != nil {
		return nil, err
	}
	if reply[0] != 5 {
		return nil, errProxyProtocol
	}
	switch reply[1] {
	case 0: // no authentication
	case 2:
		if u.User == nil {
			return nil, errProxyAuth
		}
		auth := append([]byte{1, byte(len(username))}, []byte(username)...)
		auth = append(auth, byte(len(password)))
		auth = append(auth, []byte(password)...)
		if _, err = conn.Write(auth); err != nil {
			return nil, err
		}
		if _, err = io.ReadFull(conn, reply); err != nil {
			return nil, err
		}
		if reply[0] != 1 || reply[1] != 0 {
			return nil, errProxyAuth
		}
	default:
		return nil, errProxyAuth
	}
	request := []byte{5, 1, 0}
	if ip := net.ParseIP(host); ip != nil {
		if v4 := ip.To4(); v4 != nil {
			request = append(request, 1)
			request = append(request, v4...)
		} else {
			request = append(request, 4)
			request = append(request, ip.To16()...)
		}
	} else {
		if len(host) > 255 {
			return nil, errBadAddress
		}
		request = append(request, 3, byte(len(host)))
		request = append(request, []byte(host)...)
	}
	port := make([]byte, 2)
	binary.BigEndian.PutUint16(port, uint16(port64))
	request = append(request, port...)
	if _, err = conn.Write(request); err != nil {
		return nil, err
	}
	header := make([]byte, 4)
	if _, err = io.ReadFull(conn, header); err != nil {
		return nil, err
	}
	if header[0] != 5 || header[2] != 0 {
		return nil, errProxyProtocol
	}
	if header[1] != 0 {
		return nil, errProxyConnect
	}
	addressBytes := 0
	switch header[3] {
	case 1:
		addressBytes = 4
	case 4:
		addressBytes = 16
	case 3:
		size := []byte{0}
		if _, err = io.ReadFull(conn, size); err != nil {
			return nil, err
		}
		addressBytes = int(size[0])
	default:
		return nil, errProxyProtocol
	}
	if _, err = io.CopyN(io.Discard, conn, int64(addressBytes+2)); err != nil {
		return nil, fmt.Errorf("SOCKS5 response truncated: %w", errProxyProtocol)
	}
	_ = conn.SetDeadline(time.Time{})
	ok = true
	return conn, nil
}
