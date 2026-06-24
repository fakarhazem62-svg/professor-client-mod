package com.professor.client;

/**
 * Global persistent module toggles — survive screen close/reopen.
 * Referenced from ProfessorClientMod tick and ProfessorScreen UI.
 */
public final class XerionModules {

    private XerionModules() {}

    // ── Combat modules ─────────────────────────────────────────────────────
    public static volatile boolean autoSwing     = false;
    public static volatile int     autoSwingDelay = 50;   // ms between auto-swings
    private static volatile long   lastAutoSwing  = 0;

    public static volatile boolean antiAfk        = false;
    private static volatile long   lastAfkMove    = 0;

    // ── Utility modules ────────────────────────────────────────────────────
    public static volatile boolean noFallAlways   = false; // send ground=true constantly
    public static volatile boolean antiKbAlways   = false; // send position every tick

    // ── Session statistics ─────────────────────────────────────────────────
    public static final long   sessionStart   = System.currentTimeMillis();
    public static volatile long totalPktsSent = 0;
    public static volatile int  actionsRun    = 0;

    // ── Auto-swing gate ────────────────────────────────────────────────────
    public static boolean canAutoSwing() {
        long now = System.currentTimeMillis();
        if (now - lastAutoSwing >= autoSwingDelay) { lastAutoSwing = now; return true; }
        return false;
    }

    // ── Anti-AFK gate ──────────────────────────────────────────────────────
    public static boolean shouldAfkMove() {
        long now = System.currentTimeMillis();
        if (now - lastAfkMove >= 28_000) { lastAfkMove = now; return true; }
        return false;
    }

    // ── Session time string ────────────────────────────────────────────────
    public static String sessionTimeStr() {
        long s = (System.currentTimeMillis() - sessionStart) / 1000;
        if (s < 60) return s + "s";
        return (s / 60) + "m " + (s % 60) + "s";
    }

    // ── Module count (how many are ON) ─────────────────────────────────────
    public static int activeCount() {
        int n = 0;
        if (autoSwing)   n++;
        if (antiAfk)     n++;
        if (noFallAlways)n++;
        if (antiKbAlways)n++;
        return n;
    }
}
