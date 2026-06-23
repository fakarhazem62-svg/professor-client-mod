package com.professor.client.proxy;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyManager {

    public enum ProxyType { HTTP, SOCKS5 }

    public static class ProxyEntry {
        public final String    host;
        public final int       port;
        public final ProxyType type;
        public final String    user;
        public final String    pass;
        public volatile boolean alive = true;
        public volatile int    uses  = 0;

        public ProxyEntry(String host, int port, ProxyType type, String user, String pass) {
            this.host = host; this.port = port; this.type = type;
            this.user = user; this.pass = pass;
        }
        public ProxyEntry(String host, int port, ProxyType type) { this(host, port, type, null, null); }

        @Override public String toString() {
            return "[" + type.name() + "] " + host + ":" + port + (user != null ? " auth" : "") + "  uses:" + uses;
        }
    }

    private static final List<ProxyEntry> PROXIES = new ArrayList<>();
    private static final AtomicInteger    IDX     = new AtomicInteger(0);
    private static volatile boolean       enabled = false;

    /** Parse and add one proxy line.
     *  Formats: ip:port  |  socks5://ip:port  |  http://ip:port:user:pass */
    public static synchronized void add(String line) {
        line = line.trim();
        if (line.isEmpty() || line.startsWith("#")) return;
        try {
            ProxyType type = ProxyType.HTTP;
            if (line.startsWith("socks5://") || line.startsWith("SOCKS5://")) {
                type = ProxyType.SOCKS5; line = line.substring(9);
            } else if (line.startsWith("http://")) {
                line = line.substring(7);
            }
            String[] p = line.split(":");
            if (p.length < 2) return;
            String host = p[0];
            int    port = Integer.parseInt(p[1].trim());
            String user = p.length > 2 ? p[2] : null;
            String pass = p.length > 3 ? p[3] : null;
            PROXIES.add(new ProxyEntry(host, port, type, user, pass));
        } catch (Exception ignored) {}
    }

    /** Bulk add from multiline text (one proxy per line). */
    public static void addBulk(String text) {
        for (String line : text.split("[\r\n]+")) add(line);
    }

    public static synchronized void remove(int index) {
        if (index >= 0 && index < PROXIES.size()) PROXIES.remove(index);
    }

    public static synchronized void clear() { PROXIES.clear(); IDX.set(0); }

    public static synchronized int count() { return PROXIES.size(); }

    public static void setEnabled(boolean v) { enabled = v; }

    public static boolean isEnabled() { return enabled; }

    public static synchronized List<ProxyEntry> getAll() { return new ArrayList<>(PROXIES); }

    /** Round-robin next proxy (skips dead ones). */
    public static synchronized ProxyEntry next() {
        if (PROXIES.isEmpty()) return null;
        int start = Math.abs(IDX.getAndIncrement() % PROXIES.size());
        for (int i = 0; i < PROXIES.size(); i++) {
            ProxyEntry e = PROXIES.get((start + i) % PROXIES.size());
            if (e.alive) { e.uses++; return e; }
        }
        return null;
    }

    /** Get as java.net.Proxy for use in URL connections. */
    public static Proxy getJavaProxy() {
        ProxyEntry e = next();
        if (e == null) return Proxy.NO_PROXY;
        Proxy.Type jt = e.type == ProxyType.SOCKS5 ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        return new Proxy(jt, new InetSocketAddress(e.host, e.port));
    }

    /** Mark a proxy as dead (won't be rotated to). */
    public static synchronized void markDead(ProxyEntry entry) {
        if (entry != null) entry.alive = false;
    }

    public static int aliveCount() {
        synchronized (PROXIES) { return (int) PROXIES.stream().filter(p -> p.alive).count(); }
    }
}
