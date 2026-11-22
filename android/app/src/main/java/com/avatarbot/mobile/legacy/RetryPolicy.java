package com.avatarbot.mobile.legacy;

/** Conservative, session-scoped retry budget. Never retries a rejected login automatically.
 *  Only the foreground Android service may use this after a worker actually ran
 *  and then terminated unexpectedly. Manual Stop cancels the budget permanently
 *  until the next explicit Start. No boot/persistent background auto-start.
 */
public final class RetryPolicy {
    private static final long[] DELAYS_MS = {30000L, 60000L, 120000L};
    private int used = 0;
    private boolean cancelled = false;

    public synchronized void reset() { used = 0; cancelled = false; }
    public synchronized void cancel() { cancelled = true; }
    public synchronized int used() { return used; }
    /** Refund only when a scheduled retry was NEVER sent to Android Service due to lost network. */
    public synchronized boolean refundUndispatchedAttempt() {
        if (cancelled || used <= 0) return false;
        used--;
        return true;
    }
    public synchronized long nextDelayMs(boolean previouslyStarted, boolean userRequestedStop) {
        if (cancelled || userRequestedStop || !previouslyStarted || used >= DELAYS_MS.length) return -1;
        return DELAYS_MS[used++];
    }
}
