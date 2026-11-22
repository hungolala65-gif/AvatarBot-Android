package javax.microedition.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Small, fail-closed MIDP Connector replacement. No HTTP, file or arbitrary
 * outbound endpoints; only the user-selected game server is allowed.
 * The JAR's separate third-party solver endpoint is intentionally blocked.
 */
public final class Connector {
    private static volatile Target target;
    private Connector() { }

    public static synchronized void authorize(String host, int port, String proxyURI) {
        if (host == null || host.length() == 0 || port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid server");
        }
        Proxy proxy = Proxy.parse(proxyURI);
        Target old = target;
        target = new Target(host, port, proxy);
        if (old != null) old.closeAll();
    }
    /** Close live sockets as well as disallowing future opens. */
    public static synchronized void revoke() {
        Target old = target;
        target = null;
        if (old != null) old.closeAll();
    }
    public static Connection open(String url) throws IOException {
        Target allowed = target;
        if (allowed == null) throw new IOException("Network access not authorized");
        final URI requested;
        try { requested = new URI(url); }
        catch (Exception e) { throw new IOException("Malformed socket URL", e); }
        if (!"socket".equalsIgnoreCase(requested.getScheme())
                || requested.getRawUserInfo() != null || requested.getPath() != null && !requested.getPath().isEmpty()
                || requested.getQuery() != null || requested.getFragment() != null
                || !allowed.host.equalsIgnoreCase(requested.getHost()) || allowed.port != requested.getPort()) {
            throw new IOException("Remote endpoint blocked by Android network policy");
        }
        Socket socket = new Socket();
        try {
            allowed.register(socket); // Registration before connect makes stop interrupt pending TCP dial.
            if (allowed.proxy == null) {
                socket.connect(new InetSocketAddress(allowed.host, allowed.port), 10000);
            } else {
                socket.connect(new InetSocketAddress(allowed.proxy.host, allowed.proxy.port), 10000);
                // A connected but silent proxy must not block the bot startup indefinitely.
                socket.setSoTimeout(10000);
                negotiateSocks5(socket, allowed);
            }
            socket.setSoTimeout(20000);
            socket.setKeepAlive(true);
            if (!allowed.isActive()) throw new IOException("Connection authorization revoked");
            return new SocketAdapter(socket, allowed);
        } catch (IOException | RuntimeException e) {
            allowed.unregister(socket);
            try { socket.close(); } catch (IOException ignored) { }
            throw e;
        }
    }
    private static void negotiateSocks5(Socket s, Target allowed) throws IOException {
        Proxy p = allowed.proxy;
        InputStream in = s.getInputStream();
        OutputStream out = s.getOutputStream();
        out.write(new byte[]{5, 1, (byte)(p.user == null ? 0 : 2)});
        out.flush();
        int ver = readByte(in), auth = readByte(in);
        if (ver != 5 || auth == 255 || auth != (p.user == null ? 0 : 2)) throw new IOException("SOCKS5 auth method refused");
        if (p.user != null) {
            byte[] u = p.user.getBytes(StandardCharsets.UTF_8);
            byte[] pass = p.password.getBytes(StandardCharsets.UTF_8);
            if (u.length == 0 || u.length > 255 || pass.length > 255) throw new IOException("Bad SOCKS5 credentials length");
            out.write(1); out.write(u.length); out.write(u); out.write(pass.length); out.write(pass); out.flush();
            if (readByte(in) != 1 || readByte(in) != 0) throw new IOException("SOCKS5 authentication rejected");
        }
        byte[] host = allowed.host.getBytes(StandardCharsets.UTF_8);
        if (host.length == 0 || host.length > 255) throw new IOException("Bad target host");
        out.write(new byte[]{5, 1, 0, 3, (byte)host.length}); out.write(host);
        out.write(allowed.port >> 8); out.write(allowed.port & 255); out.flush();
        if (readByte(in) != 5 || readByte(in) != 0 || readByte(in) != 0) throw new IOException("SOCKS5 connect failed");
        int atyp = readByte(in);
        if (atyp == 1) discard(in, 4);
        else if (atyp == 4) discard(in, 16);
        else if (atyp == 3) discard(in, readByte(in));
        else throw new IOException("SOCKS5 address type invalid");
        discard(in, 2);
    }
    private static int readByte(InputStream in) throws IOException {
        int b=in.read(); if (b < 0) throw new IOException("Unexpected EOF"); return b;
    }
    private static void discard(InputStream in, int count) throws IOException {
        for (int i=0;i<count;i++) readByte(in);
    }
    private static final class Target {
        final String host; final int port; final Proxy proxy;
        private boolean active = true;
        private final Set<Socket> sockets = new HashSet<>();
        Target(String host, int port, Proxy proxy) { this.host=host; this.port=port; this.proxy=proxy; }
        synchronized void register(Socket socket) throws IOException {
            if (!active) throw new IOException("Connection authorization revoked");
            sockets.add(socket);
        }
        synchronized void unregister(Socket socket) { sockets.remove(socket); }
        synchronized boolean isActive() { return active; }
        synchronized void closeAll() {
            active = false;
            for (Socket socket : sockets) {
                try { socket.close(); } catch (IOException ignored) { }
            }
            sockets.clear();
        }
    }
    private static final class Proxy {
        final String host, user, password; final int port;
        Proxy(String host, int port, String user, String password) {
            this.host=host; this.port=port; this.user=user; this.password=password;
        }
        static Proxy parse(String value) {
            if (value == null || value.trim().isEmpty()) return null;
            URI u;
            try { u=new URI(value.trim()); }
            catch (Exception ex) { throw new IllegalArgumentException("Malformed SOCKS5 proxy"); }
            if (!"socks5".equalsIgnoreCase(u.getScheme()) || u.getHost() == null
                || u.getPort()<1 || u.getPort()>65535 || u.getPath()!=null && !u.getPath().isEmpty()
                || u.getQuery()!=null || u.getFragment()!=null) throw new IllegalArgumentException("Invalid SOCKS5 proxy");
            String user=null, pass="";
            if (u.getRawUserInfo()!=null) {
                String info=u.getUserInfo(); int n=info.indexOf(':');
                user=n<0 ? info : info.substring(0,n); pass=n<0? "":info.substring(n+1);
            }
            return new Proxy(u.getHost(),u.getPort(),user,pass);
        }
    }
    private static final class SocketAdapter implements SocketConnection {
        private final Socket socket;
        private final Target target;
        SocketAdapter(Socket socket, Target target) { this.socket=socket; this.target=target; }
        public DataInputStream openDataInputStream() throws IOException { return new DataInputStream(socket.getInputStream()); }
        public DataOutputStream openDataOutputStream() throws IOException { return new DataOutputStream(socket.getOutputStream()); }
        public void close() throws IOException { target.unregister(socket); socket.close(); }
    }
}
