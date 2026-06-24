package com.professor.client.task;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class BackgroundTaskManager {

    private static final ExecutorService POOL = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "Xerion-BG");
        t.setDaemon(true);
        return t;
    });

    private static final AtomicInteger activeTasks     = new AtomicInteger(0);
    private static final AtomicBoolean cancelRequested = new AtomicBoolean(false);
    private static volatile String     lastStatus      = "Idle";
    private static volatile int        lastColor       = 0xFF88DDFF;
    private static volatile long       packetsTotal    = 0;
    private static volatile long       packetsSent     = 0;

    /** Submit a background task. GUI can close freely; task keeps running. */
    public static Future<?> submit(String name, Runnable task) {
        cancelRequested.set(false);
        activeTasks.incrementAndGet();
        setStatus("⚡ " + name + " — running…", 0xFF00FF99);
        return POOL.submit(() -> {
            try {
                task.run();
                setStatus("✓ " + name + " — done", 0xFF00FF99);
            } catch (Exception e) {
                setStatus("✗ " + name + " — error", 0xFFFF2244);
            } finally {
                activeTasks.decrementAndGet();
            }
        });
    }

    /** Request cancellation — does NOT destroy the pool (tasks can still be submitted later). */
    public static void cancelAll() {
        cancelRequested.set(true);
        activeTasks.set(0);
        setStatus("⛔ Cancelled", 0xFFFF2244);
    }

    public static boolean isCancelled()    { return cancelRequested.get(); }
    public static int     getActive()      { return activeTasks.get(); }
    public static boolean isIdle()         { return activeTasks.get() == 0; }
    public static String  getStatus()      { return lastStatus; }
    public static int     getStatusColor() { return lastColor; }

    public static void setPacketProgress(long sent, long total) {
        packetsSent  = sent;
        packetsTotal = total;
    }
    public static long getPacketsSent()  { return packetsSent; }
    public static long getPacketsTotal() { return packetsTotal; }

    private static void setStatus(String s, int c) { lastStatus = s; lastColor = c; }

    /** Call inside a background task to check if cancellation was requested. */
    public static boolean shouldStop() {
        return cancelRequested.get() || Thread.currentThread().isInterrupted();
    }
}
