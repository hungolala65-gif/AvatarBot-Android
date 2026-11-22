import com.avatarbot.mobile.legacy.RetryPolicy;

public final class RetryPolicyOfflineTest {
    public static void main(String[] args) {
        RetryPolicy p=new RetryPolicy();
        if (p.nextDelayMs(false,false)!=-1 || p.used()!=0) throw new AssertionError("No retry before a real worker start");
        if (p.nextDelayMs(true,false)!=30000 || p.nextDelayMs(true,false)!=60000 ||
            p.nextDelayMs(true,false)!=120000 || p.nextDelayMs(true,false)!=-1)
            throw new AssertionError("Retry must be bounded with exponential backoff");
        p.reset();
        if (p.nextDelayMs(true,false)!=30000 || p.used()!=1) throw new AssertionError("Retry created");
        if (!p.refundUndispatchedAttempt() || p.used()!=0) throw new AssertionError("Lost network must refund not-dispatched retry");
        if (p.refundUndispatchedAttempt()) throw new AssertionError("Cannot refund twice");
        if (p.nextDelayMs(true,false)!=30000) throw new AssertionError("Backoff after network restoration");
        p.reset();
        if(p.nextDelayMs(true,true)!=-1 || p.used()!=0) throw new AssertionError("No retry after manual stop");
        p.cancel();
        if(p.nextDelayMs(true,false)!=-1) throw new AssertionError("Cancelled session must not restart");
        p.reset();
        if(p.nextDelayMs(true,false)!=30000) throw new AssertionError("Explicit new session resets budget");
        System.out.println("PASS: retry budget, manual stop, and bounded backoff");
    }
}
