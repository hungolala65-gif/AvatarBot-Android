package com.avatarbot.mobile;

import android.app.Application;
import com.avatarbot.mobile.legacy.EncryptedRmsBackend;
import javax.microedition.rms.RecordStore;

/** Installs encrypted Java ME store support before any bot classes load. */
public final class AvatarBotApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        RecordStore.installEncryptedBackend(new EncryptedRmsBackend(this));
    }
}
