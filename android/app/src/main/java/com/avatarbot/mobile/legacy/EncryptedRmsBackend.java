package com.avatarbot.mobile.legacy;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.microedition.rms.RecordStore;

/** Android-only encrypted persistence for TeacherStore and Stats. */
public final class EncryptedRmsBackend implements RecordStore.Backend {
    private static final String KEY_ALIAS = "avatarbot_rms_key_v1";
    private final SharedPreferences prefs;

    public EncryptedRmsBackend(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("avatarbot_rms_v1", Context.MODE_PRIVATE);
    }
    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        SecretKey existing = (SecretKey) store.getKey(KEY_ALIAS, null);
        if (existing != null) return existing;
        KeyGenerator gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256).build());
        return gen.generateKey();
    }
    private static String requireStoreName(String name) {
        if (!"farm2x-teacher".equals(name) && !"farm2x-stats".equals(name))
            throw new IllegalArgumentException("RMS store not approved for disk storage");
        return name;
    }
    @Override public byte[] load(String name) {
        String encoded = prefs.getString(requireStoreName(name), null);
        if (encoded == null) return null;
        try {
            byte[] packed = Base64.decode(encoded, Base64.NO_WRAP);
            if (packed.length < 29) throw new IllegalStateException("Encrypted RMS corrupted");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128,
                    java.util.Arrays.copyOfRange(packed, 0, 12)));
            return cipher.doFinal(packed, 12, packed.length - 12);
        } catch (Exception ex) { throw new IllegalStateException("Cannot decrypt RMS store", ex); }
    }
    @Override public void save(String name, byte[] plainData) {
        requireStoreName(name);
        if (plainData == null) throw new IllegalArgumentException("Missing RMS data");
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] content = cipher.doFinal(plainData);
            byte[] iv = cipher.getIV();
            byte[] packed = new byte[iv.length + content.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(content, 0, packed, iv.length, content.length);
            if (!prefs.edit().putString(name, Base64.encodeToString(packed, Base64.NO_WRAP)).commit())
                throw new IllegalStateException("Cannot persist RMS store");
        } catch (Exception ex) { throw new IllegalStateException("Cannot encrypt RMS store", ex); }
    }
    @Override public void delete(String name) {
        if (!prefs.edit().remove(requireStoreName(name)).commit())
            throw new IllegalStateException("Cannot delete RMS store");
    }
}
