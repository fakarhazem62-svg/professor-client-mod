package com.professor.client.proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages a list of SOCKS5/HTTP proxies for the Xerion packet flood.
 *
 * When the server kicks/disconnects with "too many packets" or similar,
 * ProxyManager rotates to the next proxy, applies it via system properties,
 * then the mod auto-reconnects — keeping the attack uninterrupted.
 *
 * Proxy format accepted:
 *   host:port             (defaults to SOCKS5)
 *   socks5://host:port
 *   socks4://host:port
 *   http://host:port
 *   user:pass@host:port   (authenticated SOCKS5)
 */
public class ProxyManager {

    public enum ProxyType { SOCKS5, SOCKS4, HTTP }

    public record ProxyEntry(String host, int port, ProxyType type, String user, String pass) {
        @Override public String toString() {
            String prefix = switch(type){ case SOCKS4 -> "socks4"; case HTTP -> "http"; default -> "socks5"; };
            return (user!=null&&!user.isEmpty()) ? prefix+"://"+user+":***@"+host+":"+port : prefix+"://"+host+":"+port;
        }
    }

    private static final List<ProxyEntry> proxies = new ArrayList<>();
    private static final AtomicInteger idx       = new AtomicInteger(0);
    private static volatile boolean enabled      = false;
    private static volatile boolean autoRotate   = false;

    // ── Disconnect auto-reconnect state ───────────────────────────────────
    // Keywords in disconnect messages that trigger auto-rotate
    private static final String[] KICK_KEYWORDS = {
        "too many packet", "packet flood", "sending too many",
        "connection throttled", "rate limit", "exploit", "banned",
        "kicked", "disconnect", "connection lost", "timed out"
    };

    public static boolean isEnabled()     { return enabled && !proxies.isEmpty(); }
    public static boolean isAutoRotate()  { return autoRotate; }
    public static void setEnabled(boolean v)    { enabled = v; }
    public static void setAutoRotate(boolean v) { autoRotate = v; }
    public static int getCount()   { return proxies.size(); }
    public static int getCurrent() { return proxies.isEmpty() ? 0 : idx.get() % proxies.size(); }

    public static ProxyEntry getCurrentEntry() {
        if (proxies.isEmpty()) return null;
        return proxies.get(idx.get() % proxies.size());
    }

    // ── Add proxy from string ─────────────────────────────────────────────
    public static boolean addProxy(String raw) {
        raw = raw.trim();
        if (raw.isEmpty()) return false;
        try {
            ProxyType type = ProxyType.SOCKS5;
            String user = "", pass = "";

            if (raw.startsWith("socks4://")) { type = ProxyType.SOCKS4; raw = raw.substring(9); }
            else if (raw.startsWith("socks5://")) { raw = raw.substring(9); }
            else if (raw.startsWith("http://"))  { type = ProxyType.HTTP; raw = raw.substring(7); }

            // user:pass@host:port
            if (raw.contains("@")) {
                String[] at = raw.split("@", 2);
                String[] cred = at[0].split(":", 2);
                user = cred[0]; pass = cred.length > 1 ? cred[1] : "";
                raw = at[1];
            }

            String[] hp = raw.split(":", 2);
            String host = hp[0];
            int port = Integer.parseInt(hp[1].trim());
            if (port < 1 || port > 65535) return false;

            proxies.add(new ProxyEntry(host, port, type, user, pass));
            return true;
        } catch (Exception e) { return false; }
    }

    public static void removeProxy(int index) {
        if (index >= 0 && index < proxies.size()) {
            proxies.remove(index);
            idx.set(0);
        }
    }

    public static void clear() { proxies.clear(); idx.set(0); }

    public static List<ProxyEntry> getAll() { return new ArrayList<>(proxies); }

    // ── Rotate to next proxy ──────────────────────────────────────────────
    public static ProxyEntry rotate() {
        if (proxies.isEmpty()) return null;
        int next = idx.incrementAndGet() % proxies.size();
        idx.set(next);
        ProxyEntry e = proxies.get(next);
        applyProxy(e);
        return e;
    }

    // ── Apply proxy via system properties ─────────────────────────────────
    // Minecraft's Socket layer reads these before opening connections
    public static void applyProxy(ProxyEntry e) {
        if (e == null) {
            clearSystemProxy();
            return;
        }
        switch (e.type()) {
            case SOCKS5, SOCKS4 -> {
                System.setProperty("socksProxyHost", e.host());
                System.setProperty("socksProxyPort", String.valueOf(e.port()));
                System.setProperty("socksProxyVersion", e.type() == ProxyType.SOCKS4 ? "4" : "5");
                if (e.user() != null && !e.user().isEmpty()) {
                    System.setProperty("java.net.socks.username", e.user());
                    System.setProperty("java.net.socks.password", e.pass() != null ? e.pass() : "");
                }
                // Clear HTTP proxy
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
            }
            case HTTP -> {
                System.setProperty("http.proxyHost",  e.host());
                System.setProperty("http.proxyPort",  String.valueOf(e.port()));
                System.setProperty("https.proxyHost", e.host());
                System.setProperty("https.proxyPort", String.valueOf(e.port()));
                // Clear SOCKS
                System.clearProperty("socksProxyHost");
                System.clearProperty("socksProxyPort");
            }
        }
        System.setProperty("java.net.useSystemProxies", "false");
    }

    public static void clearSystemProxy() {
        System.clearProperty("socksProxyHost");
        System.clearProperty("socksProxyPort");
        System.clearProperty("socksProxyVersion");
        System.clearProperty("http.proxyHost");
        System.clearProperty("http.proxyPort");
        System.clearProperty("https.proxyHost");
        System.clearProperty("https.proxyPort");
    }

    // ── Detect if kick message needs rotation ─────────────────────────────
    public static boolean shouldRotateOnKick(String reason) {
        if (!autoRotate || !enabled || proxies.isEmpty()) return false;
        String low = reason.toLowerCase();
        for (String kw : KICK_KEYWORDS) if (low.contains(kw)) return true;
        return false;
    }

    // ── Always rotate on any disconnect (aggressive mode) ─────────────────
    public static boolean shouldAlwaysRotate() {
        return autoRotate && enabled && !proxies.isEmpty();
    }
}
