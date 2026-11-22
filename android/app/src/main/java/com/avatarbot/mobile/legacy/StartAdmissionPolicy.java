package com.avatarbot.mobile.legacy;

/** Pure, conservative admission policy for Android Service commands.
 * Duplicate Starts are ignored without changing active session metadata.
 * Starting a new account requires an explicit Stop and a fully quiescent worker.
 */
public final class StartAdmissionPolicy {
    private StartAdmissionPolicy() { }
    public static boolean acceptManualStart(boolean coreActive, boolean observingWorker,
        boolean scheduledRetry, boolean offlineWaiting) {
        return !coreActive && !observingWorker && !scheduledRetry && !offlineWaiting;
    }
}
