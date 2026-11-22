import farm2x.Config;
import javax.microedition.rms.RecordStore;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/** Verifies the safe list of persisted stores, snapshot round-trip and failures. */
public final class RmsPersistenceOfflineTest {
    private static int assertions;
    private static void ok(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        assertions++;
    }
    static final class MemoryEncryptedBackend implements RecordStore.Backend {
        final Map<String, byte[]> blobs = new HashMap<>();
        @Override public byte[] load(String name) {
            byte[] b = blobs.get(name);
            return b == null ? null : b.clone();
        }
        @Override public void save(String name, byte[] data) { blobs.put(name, data.clone()); }
        @Override public void delete(String name) { blobs.remove(name); }
    }
    private static void forgetInMemoryStores() throws Exception {
        Field field = RecordStore.class.getDeclaredField("STORES");
        field.setAccessible(true);
        ((Map<?,?>) field.get(null)).clear();
    }
    public static void main(String[] args) throws Exception {
        MemoryEncryptedBackend backend = new MemoryEncryptedBackend();
        RecordStore.installEncryptedBackend(backend);
        Config config = Config.load();
        config.username = "private_account";
        config.password = "private_password";
        config.save();
        ok(!backend.blobs.containsKey("farm2x-config"), "Account credentials must never be persisted through RMS");
        RecordStore s = RecordStore.openRecordStore("farm2x-teacher", true);
        byte[] first = "subject:question:answer".getBytes(StandardCharsets.UTF_8);
        int id = s.addRecord(first, 0, first.length);
        ok(backend.blobs.containsKey("farm2x-teacher"), "Teacher snapshot saved");
        first[0] = 0; // inputs cannot mutate internal state
        ok(s.getRecord(id)[0] != 0, "RMS owns input bytes");
        forgetInMemoryStores();
        RecordStore restored = RecordStore.openRecordStore("farm2x-teacher", false);
        ok(Arrays.equals(restored.getRecord(id), "subject:question:answer".getBytes(StandardCharsets.UTF_8)),
            "Teacher snapshot survives simulated process exit");
        byte[] changed = "new_answer".getBytes(StandardCharsets.UTF_8);
        restored.setRecord(id, changed, 0, changed.length);
        forgetInMemoryStores();
        ok(Arrays.equals(RecordStore.openRecordStore("farm2x-teacher", false).getRecord(id), changed),
            "Record updates survive simulated process exit");
        RecordStore stats = RecordStore.openRecordStore("farm2x-stats", true);
        stats.addRecord(new byte[]{1,2,3}, 0, 3);
        ok(backend.blobs.containsKey("farm2x-stats"), "Stats persisted");
        RecordStore.deleteRecordStore("farm2x-teacher");
        ok(!backend.blobs.containsKey("farm2x-teacher"), "Delete also removes snapshot");
        backend.blobs.put("farm2x-teacher", new byte[]{99,88});
        boolean bad = false;
        try { RecordStore.openRecordStore("farm2x-teacher", true); }
        catch (IllegalStateException ex) { bad = true; }
        ok(bad, "Corrupt snapshots rejected rather than overwritten");
        System.out.println("PASS " + assertions + " RMS persistence assertions");
    }
}
