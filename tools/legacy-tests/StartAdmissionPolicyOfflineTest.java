import com.avatarbot.mobile.legacy.StartAdmissionPolicy;

public final class StartAdmissionPolicyOfflineTest {
    public static void main(String[] args) {
        int accepted=0, rejected=0;
        for (int flags=0; flags<16; flags++) {
            boolean actual=StartAdmissionPolicy.acceptManualStart(
                (flags&1)!=0, (flags&2)!=0, (flags&4)!=0, (flags&8)!=0);
            boolean expected=flags==0;
            if (actual!=expected) throw new AssertionError("Incorrect duplicate Start gate: "+flags);
            if (actual) accepted++; else rejected++;
        }
        if (accepted!=1 || rejected!=15) throw new AssertionError("Incorrect gate counts");
        System.out.println("PASS: 16 Start cases; duplicates never admitted during active/waiting session");
    }
}
