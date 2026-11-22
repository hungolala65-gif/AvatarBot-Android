import com.avatarbot.mobile.legacy.AuthWatchdogPolicy;

public class AuthWatchdogPolicyOfflineTest {
    static void check(boolean v, String message) { if (!v) throw new AssertionError(message); }
    public static void main(String[] args) {
        AuthWatchdogPolicy p = new AuthWatchdogPolicy();
        check(p.observe("awaiting", 5000L).equals("ok"), "initial awaiting");
        check(p.observe("awaiting", 184999L).equals("ok"), "before initial timeout");
        check(p.observe("awaiting", 185000L).equals("initial_auth_timeout"), "initial timeout");
        check(p.observe("confirmed", 186000L).equals("initial_auth_timeout"), "tripped until reset");
        p.reset();
        check(p.observe("confirmed", 10L).equals("ok"), "good session");
        check(p.wasEverConfirmed(), "remember success");
        check(p.observe("disconnected", 1000L).equals("ok"), "reconnect grace starts");
        check(p.observe("awaiting", 600999L).equals("ok"), "before reconnect timeout");
        check(p.observe("awaiting", 601000L).equals("reconnect_timeout"), "reconnect timeout");
        p.reset();
        check(p.observe("awaiting", 0L).equals("ok"), "new worker");
        check(p.observe("confirmed", 60000L).equals("ok"), "login within grace");
        check(p.observe("awaiting", 80000L).equals("ok"), "temporary cycle disconnect");
        check(p.observe("confirmed", 100000L).equals("ok"), "another successful cycle resets clock");
        check(p.observe("disconnected", 120000L).equals("ok"), "second disconnect");
        check(p.observe("disconnected", 720000L).equals("reconnect_timeout"), "second interval independent");
        p.reset();
        check(p.observe("awaiting", 500L).equals("ok"), "awaiting until unknown");
        check(p.observe("unavailable", 200000L).equals("unknown"), "reflection unknown must not stop a worker");
        check(p.observe("awaiting", 200001L).equals("ok"), "unknown resets unverifiable interval");
        check(p.observe("awaiting", 379999L).equals("ok"), "reset period");
        check(p.observe("awaiting", 380001L).equals("initial_auth_timeout"), "expired reset period");
        p.reset();
        check(p.observe("awaiting", 100000L).equals("ok"), "started after reboot");
        check(p.observe("awaiting", 500L).equals("ok"), "time moved backwards reset period");
        check(p.observe("awaiting", 180500L).equals("initial_auth_timeout"), "monotonic restart");
        try { p.observe("awaiting", -1L); throw new AssertionError("negative time accepted"); }
        catch(IllegalArgumentException expected) { }
        System.out.println("PASS: auth watchdog initial/reconnect timeouts, resets, unknown-state fail-open, monotonic clock");
    }
}
