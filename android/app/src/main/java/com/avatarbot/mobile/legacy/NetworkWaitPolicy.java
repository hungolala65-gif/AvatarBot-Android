package com.avatarbot.mobile.legacy;

/** Monotonic, bounded offline waiting. This does not consume worker retry attempts.
 *  Caller provides elapsedRealtime() values, never wall-clock timestamps.
 */
public final class NetworkWaitPolicy {
    public static final long MAX_WAIT_MS=30L*60L*1000L;
    private long sinceMs=-1L;
    public synchronized void begin(long elapsedNowMs) {
        if (elapsedNowMs < 0) throw new IllegalArgumentException("Invalid monotonic time");
        if (sinceMs<0) sinceMs=elapsedNowMs;
    }
    public synchronized boolean waiting() { return sinceMs>=0; }
    public synchronized boolean expired(long elapsedNowMs) {
        return sinceMs>=0 && elapsedNowMs>=sinceMs && elapsedNowMs-sinceMs>=MAX_WAIT_MS;
    }
    public synchronized boolean restore(long elapsedNowMs) {
        if (!waiting() || expired(elapsedNowMs)) return false;
        sinceMs=-1L;
        return true;
    }
    public synchronized void cancel() { sinceMs=-1L; }
    public synchronized long remainingMs(long elapsedNowMs) {
        if (!waiting()) return 0;
        return Math.max(0L, MAX_WAIT_MS-Math.max(0L,elapsedNowMs-sinceMs));
    }
}
