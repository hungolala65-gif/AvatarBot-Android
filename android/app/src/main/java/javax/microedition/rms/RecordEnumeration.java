package javax.microedition.rms;
public interface RecordEnumeration {
    boolean hasNextElement();
    byte[] nextRecord();
    int nextRecordId();
    void destroy();
}
