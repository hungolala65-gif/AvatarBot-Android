package com.avatarbot.mobile.legacy;

/**
 * Fail-closed authentication liveness guard. Observing a running JAR thread is not
 * proof that a game login succeeded: the original BotWorker catches cycle errors
 * and keeps running. This policy only evaluates read-only session observations.
 *
 * Awaiting LoginSuccess for three minutes stops an otherwise busy-looping worker;
 * after a known-good session, ten continuous minutes without confirmed login
 * are tolerated for expected between-cycle disconnects (up to five-minute
 * user-selected intervals) and network reconnects.
 * A monitor that cannot inspect the JAR is not evidence of failed login: the
 * policy returns UNKNOWN instead of stopping a potentially healthy bot.
 *
 * There are NO automatic password retries or bypasses in this policy.
 */
public final class AuthWatchdogPolicy {
    public static final long INITIAL_AUTH_GRACE_MS = 180000L;
    public static final long RECONNECT_GRACE_MS = 600000L;
    public static final String OK = "ok";
    public static final String UNKNOWN = "unknown";
    public static final String INITIAL_AUTH_TIMEOUT = "initial_auth_timeout";
    public static final String RECONNECT_TIMEOUT = "reconnect_timeout";

    private long noConfirmationSince = -1;
    private boolean confirmedEver = false;
    private boolean tripped = false;

    public synchronized void reset() {
        noConfirmationSince = -1;
        confirmedEver = false;
        tripped = false;
    }

    /** Monotonic elapsedRealtime milliseconds, never wall clock time. */
    public synchronized String observe(String sessionState, long elapsedMs) {
        if (elapsedMs < 0) throw new IllegalArgumentException("Negative elapsed time");
        if (tripped) return confirmedEver ? RECONNECT_TIMEOUT : INITIAL_AUTH_TIMEOUT;
        if ("confirmed".equals(sessionState)) {
            confirmedEver = true;
            noConfirmationSince = -1;
            return OK;
        }
        if ("unavailable".equals(sessionState) || "inactive".equals(sessionState)) {
            // Reflection or monitor unavailability cannot prove a failed login.
            noConfirmationSince = -1;
            return UNKNOWN;
        }
        if (!"awaiting".equals(sessionState) && !"disconnected".equals(sessionState)) {
            noConfirmationSince = -1;
            return UNKNOWN;
        }
        if (noConfirmationSince < 0 || elapsedMs < noConfirmationSince) {
            noConfirmationSince = elapsedMs;
            return OK;
        }
        long limit = confirmedEver ? RECONNECT_GRACE_MS : INITIAL_AUTH_GRACE_MS;
        if (elapsedMs - noConfirmationSince >= limit) {
            tripped = true;
            return confirmedEver ? RECONNECT_TIMEOUT : INITIAL_AUTH_TIMEOUT;
        }
        return OK;
    }
    public synchronized boolean wasEverConfirmed() { return confirmedEver; }
}
