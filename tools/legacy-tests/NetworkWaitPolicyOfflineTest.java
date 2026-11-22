import com.avatarbot.mobile.legacy.NetworkWaitPolicy;
import com.avatarbot.mobile.legacy.RetryPolicy;

public final class NetworkWaitPolicyOfflineTest {
    private static void ok(boolean b, String msg) { if(!b) throw new AssertionError(msg); }
    public static void main(String[] args) {
        NetworkWaitPolicy network=new NetworkWaitPolicy();
        RetryPolicy retry=new RetryPolicy();
        network.begin(1000L);
        ok(network.waiting(), "offline wait active");
        ok(network.remainingMs(1000L)==NetworkWaitPolicy.MAX_WAIT_MS, "fresh TTL");
        network.begin(3000L); // no resetting the deadline when another callback fires
        ok(network.remainingMs(3000L)==NetworkWaitPolicy.MAX_WAIT_MS-2000L, "no extend via retry");
        ok(!network.expired(1000L+NetworkWaitPolicy.MAX_WAIT_MS-1), "deadline exclusive");
        ok(retry.used()==0, "offline wait must not consume reconnect budget");
        ok(network.restore(2000L), "can resume when network returns");
        ok(!network.waiting(), "wait cleared on restore");
        ok(retry.nextDelayMs(true,false)==30000, "first actual retry only after online");
        network.begin(5000L);
        ok(network.expired(5000L+NetworkWaitPolicy.MAX_WAIT_MS), "bounded 30m wait");
        ok(!network.restore(5000L+NetworkWaitPolicy.MAX_WAIT_MS), "no reconnect beyond deadline");
        network.cancel();
        ok(!network.waiting(), "manual stop cancels offline waiting");
        ok(!network.restore(999999999), "cancelled wait cannot resume");
        ok(retry.used()==1, "wait state did not use more retries");
        System.out.println("PASS: offline wait, 30-minute TTL, cancellation, retry budget");
    }
}
