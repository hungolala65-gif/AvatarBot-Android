import com.avatarbot.mobile.legacy.LegacyBotRuntime;

/** Protect against a slow secure-store load starting the bot after Stop. */
public final class StartTicketOfflineTest {
    public static void main(String[] args) {
        long first=LegacyBotRuntime.reserveStartTicket();
        if (first < 0 || !LegacyBotRuntime.isStartReserved(first))
            throw new AssertionError("Start reservation missing");
        if (LegacyBotRuntime.reserveStartTicket() != -1)
            throw new AssertionError("Duplicate start request accepted");
        LegacyBotRuntime.stop();
        if (LegacyBotRuntime.isStartReserved(first))
            throw new AssertionError("Cancelled launch retained reservation");
        if (LegacyBotRuntime.startReserved(first, "offline_dummy", "pw", "127.0.0.1", 19128, "", 0, false, false))
            throw new AssertionError("A cancelled queued start was allowed to run");
        if (LegacyBotRuntime.active()) throw new AssertionError("Cancelled start became active");
        if (LegacyBotRuntime.setFailure(first, new IllegalArgumentException("secret")))
            throw new AssertionError("Stale exception changed worker state");
        long second=LegacyBotRuntime.reserveStartTicket();
        if (second <= first || !LegacyBotRuntime.isStartReserved(second))
            throw new AssertionError("Fresh ticket not issued");
        if (LegacyBotRuntime.setFailure(first, new RuntimeException()))
            throw new AssertionError("Old failure interrupted new launch");
        if (!LegacyBotRuntime.setFailure(second, new IllegalStateException("password leak")))
            throw new AssertionError("Current failure was ignored");
        if (!"failed".equals(LegacyBotRuntime.state()) || !"IllegalStateException".equals(LegacyBotRuntime.error()))
            throw new AssertionError("Failure status/secret sanitization incorrect");
        LegacyBotRuntime.stop();
        if (!"stopped".equals(LegacyBotRuntime.state()))
            throw new AssertionError("Explicit Stop did not reset failure state");
        System.out.println("PASS: async launch cancellation, stale ticket rejection, clean Stop and sanitized failure");
    }
}
