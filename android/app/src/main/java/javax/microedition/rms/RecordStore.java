package javax.microedition.rms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minimal Java ME RMS adapter for the user-provided bot core.
 * Only the non-credential stores farm2x-teacher and farm2x-stats may be persisted.
 * The Android Backend encrypts content using Android Keystore before it hits disk.
 * farm2x-config always stays in RAM because it includes the account password.
 */
public final class RecordStore {
    public interface Backend {
        byte[] load(String name);
        void save(String name, byte[] plainData);
        void delete(String name);
    }
    private static final int MAGIC = 0x41525231; // ARR1
    private static final int MAX_RECORDS = 20000;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final Map<String, RecordStore> STORES = new HashMap<>();
    private static Backend backend;
    private final String name;
    private final Map<Integer, byte[]> values = new LinkedHashMap<>();
    private int nextId = 1;

    private RecordStore(String name) { this.name = name; }
    private static boolean persistent(String name) {
        return "farm2x-teacher".equals(name) || "farm2x-stats".equals(name);
    }

    /** Install before first use from Application/Service. Never install an unencrypted backend. */
    public static synchronized void installEncryptedBackend(Backend encryptedBackend) {
        if (encryptedBackend == null) throw new IllegalArgumentException("Backend required");
        if (backend != null && backend != encryptedBackend) {
            // Existing records must not silently switch between different storage contexts.
            if (!STORES.isEmpty()) throw new IllegalStateException("RMS already initialized");
        }
        backend = encryptedBackend;
    }

    public static synchronized RecordStore openRecordStore(String name, boolean create) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("Store name missing");
        RecordStore store = STORES.get(name);
        if (store == null) {
            byte[] packed = persistent(name) && backend != null ? backend.load(name) : null;
            if (packed != null) store = unpack(name, packed);
            else if (create) store = new RecordStore(name);
            if (store != null) STORES.put(name, store);
        }
        if (store == null) throw new IllegalStateException("RecordStore missing");
        return store;
    }
    public static synchronized void deleteRecordStore(String name) {
        STORES.remove(name);
        if (persistent(name) && backend != null) backend.delete(name);
    }
    public void closeRecordStore() { }
    public synchronized int getNumRecords() { return values.size(); }
    public synchronized byte[] getRecord(int id) {
        byte[] v = values.get(id);
        if (v == null) throw new IllegalArgumentException("Record missing");
        return v.clone();
    }
    public synchronized int addRecord(byte[] b, int offset, int length) {
        if (values.size() >= MAX_RECORDS || nextId <= 0) throw new IllegalStateException("RMS full");
        byte[] value = slice(b, offset, length);
        if (value.length > MAX_BYTES) throw new IllegalArgumentException("RMS record too large");
        int id = nextId++;
        values.put(id, value);
        try { persist(); }
        catch (RuntimeException ex) { values.remove(id); nextId--; throw ex; }
        return id;
    }
    public synchronized void setRecord(int id, byte[] b, int offset, int length) {
        if (!values.containsKey(id)) throw new IllegalArgumentException("Record missing");
        byte[] value = slice(b, offset, length);
        if (value.length > MAX_BYTES) throw new IllegalArgumentException("RMS record too large");
        byte[] old = values.put(id, value);
        try { persist(); }
        catch (RuntimeException ex) { values.put(id, old); throw ex; }
    }
    private static byte[] slice(byte[] b, int off, int len) {
        if (b == null || off < 0 || len < 0 || off > b.length - len)
            throw new IllegalArgumentException("Invalid record range");
        return Arrays.copyOfRange(b, off, off + len);
    }
    private synchronized void persist() {
        if (!persistent(name) || backend == null) return;
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC);
            out.writeInt(nextId);
            out.writeInt(values.size());
            int total = 0;
            for (Map.Entry<Integer, byte[]> e : values.entrySet()) {
                total += e.getValue().length;
                if (total > MAX_BYTES) throw new IllegalStateException("RMS size limit exceeded");
                out.writeInt(e.getKey()); out.writeInt(e.getValue().length); out.write(e.getValue());
            }
            out.flush();
            backend.save(name, bytes.toByteArray());
        } catch (IOException e) { throw new IllegalStateException("RMS serialization failed", e); }
    }
    private static RecordStore unpack(String name, byte[] packed) {
        if (packed.length > MAX_BYTES + (MAX_RECORDS * 8) + 12)
            throw new IllegalStateException("RMS snapshot too large");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(packed));
            if (in.readInt() != MAGIC) throw new IOException("RMS format mismatch");
            int next = in.readInt(), count = in.readInt();
            if (next < 1 || count < 0 || count > MAX_RECORDS) throw new IOException("Invalid RMS header");
            RecordStore s = new RecordStore(name);
            int bytes = 0, maxId = 0;
            for (int i = 0; i < count; i++) {
                int id = in.readInt(), len = in.readInt();
                if (id <= 0 || len < 0 || len > MAX_BYTES - bytes || s.values.containsKey(id))
                    throw new IOException("Invalid RMS record");
                byte[] value = new byte[len]; in.readFully(value);
                s.values.put(id, value); bytes += len; maxId = Math.max(maxId, id);
            }
            if (next <= maxId || in.available() != 0) throw new IOException("Invalid RMS tail");
            s.nextId = next;
            return s;
        } catch (IOException ex) { throw new IllegalStateException("RMS snapshot invalid", ex); }
    }
    public synchronized RecordEnumeration enumerateRecords(RecordFilter filter, RecordComparator comparator, boolean keepUpdated) {
        List<Integer> ids = new ArrayList<>();
        for (Map.Entry<Integer, byte[]> entry : values.entrySet()) {
            if (filter == null || filter.matches(entry.getValue().clone())) ids.add(entry.getKey());
        }
        if (comparator != null) ids.sort((a, b) -> comparator.compare(values.get(a).clone(), values.get(b).clone()));
        return new Snapshot(this, ids);
    }
    private static final class Snapshot implements RecordEnumeration {
        private final RecordStore store;
        private final List<Integer> ids;
        private int position = 0;
        Snapshot(RecordStore store, List<Integer> ids) { this.store = store; this.ids = ids; }
        public synchronized boolean hasNextElement() { return position < ids.size(); }
        public synchronized int nextRecordId() { return ids.get(position++); }
        public synchronized byte[] nextRecord() { return store.getRecord(nextRecordId()); }
        public synchronized void destroy() { ids.clear(); position = 0; }
    }
}
