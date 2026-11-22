package com.avatarbot.mobile.legacy;

import farm2x.Bot;
import farm2x.BotWorker;
import farm2x.Session;
import java.lang.reflect.Field;

/**
 * Read-only monitor for the *original* JAR's live session, without modifying its
 * packet formats or claiming task success. Verified against Bot.connectLogin bytecode:
 * ownerID is assigned on LoginSuccess (-4), never on LoginFail (-5).
 *
 * Possible results: awaiting, confirmed, disconnected, unavailable. A confirmed
 * value only proves that a positive character ID was parsed in the currently
 * open session. It does NOT prove that Farm/Fishing/Diamond/Teacher completed.
 */
public final class LegacySessionInspector {
    private LegacySessionInspector() {}
    private static final Field WORKER_BOT;
    private static final Field BOT_SESSION;
    private static final Field BOT_OWNER_ID;

    static {
        Field a=null, b=null, c=null;
        try {
            a=BotWorker.class.getDeclaredField("bot"); a.setAccessible(true);
            b=Bot.class.getDeclaredField("session"); b.setAccessible(true);
            c=Bot.class.getDeclaredField("ownerID"); c.setAccessible(true);
        } catch (ReflectiveOperationException | RuntimeException error) {
            // Always fall back to unavailable rather than assuming login success.
            a=null; b=null; c=null;
        }
        WORKER_BOT=a; BOT_SESSION=b; BOT_OWNER_ID=c;
    }

    /**
     * Bytecode-grounded LoginSuccess evidence for a BotWorker cycle. BotWorker
     * constructs a fresh Bot for every cycle, and Bot.ownerID is assigned only
     * when connectLogin receives LoginSuccess (-4). The session may already
     * be CLOSED when UI polls; do not mistake that for a missed login.
     *
     * The returned object identity must be counted ONCE per cycle; this is NOT
     * a completed Auto action, nor does it imply the current socket is open.
     */
    public static Object authenticatedCycleIdentity(BotWorker worker) {
        if (worker == null || WORKER_BOT == null || BOT_SESSION == null || BOT_OWNER_ID == null)
            return null;
        try {
            Bot bot=(Bot)WORKER_BOT.get(worker);
            if (bot == null || BOT_SESSION.get(bot) == null) return null;
            return BOT_OWNER_ID.getInt(bot)>0 ? bot : null;
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return null;
        }
    }

    public static String inspect(BotWorker worker) {
        if (worker == null || WORKER_BOT == null || BOT_SESSION == null || BOT_OWNER_ID == null)
            return "unavailable";
        try {
            Bot bot=(Bot)WORKER_BOT.get(worker);
            if (bot == null) return "awaiting";
            Session session=(Session)BOT_SESSION.get(bot);
            if (session == null) return "awaiting";
            if (session.isClosed()) return "disconnected";
            int characterId=BOT_OWNER_ID.getInt(bot);
            // A false negative for an unusual ID=0 is safer than false success.
            return characterId > 0 ? "confirmed" : "awaiting";
        } catch (ReflectiveOperationException | RuntimeException ex) {
            return "unavailable";
        }
    }
}
