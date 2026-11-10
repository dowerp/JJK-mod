package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaCharacter;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.net.RemoteStatePayload;
import com.dowerp.sukuna.net.StatePayload;
import net.minecraft.util.Util;

import java.util.HashMap;
import java.util.Map;

public final class RemoteFx {
    public static final class Entry {
        public SukunaCharacter character = SukunaCharacter.ITADORI;
        public SukunaForm form = SukunaForm.YUJI;
        public int flags;
        public long castStartMs = Long.MIN_VALUE / 2;
        public long lastUpdateMs;
        public String head = "";
    }

    private static final Map<Integer, Entry> MAP = new HashMap<>();

    private RemoteFx() {
    }

    public static Entry get(int entityId) {
        return MAP.get(entityId);
    }

    public static void apply(RemoteStatePayload p) {
        Entry e = MAP.computeIfAbsent(p.entityId(), k -> new Entry());
        SukunaCharacter[] cs = SukunaCharacter.values();
        SukunaForm[] fs = SukunaForm.values();
        if (p.character() >= 0 && p.character() < cs.length) e.character = cs[p.character()];
        if (p.form() >= 0 && p.form() < fs.length) e.form = fs[p.form()];
        e.flags = p.flags();
        e.head = p.head();
        e.lastUpdateMs = Util.getMeasuringTimeMs();
    }

    public static void markCast(int entityId) {
        Entry e = MAP.computeIfAbsent(entityId, k -> new Entry());
        e.castStartMs = Util.getMeasuringTimeMs();
    }

    public static float castProgress(Entry e) {
        float p = (Util.getMeasuringTimeMs() - e.castStartMs) / 900.0F;
        return p >= 0.0F && p <= 1.0F ? p : -1.0F;
    }

    public static boolean has(Entry e, int flag) {
        return e != null && (e.flags & flag) != 0;
    }

    public static void prune() {
        long now = Util.getMeasuringTimeMs();
        MAP.values().removeIf(e -> now - e.lastUpdateMs > 15000L);
    }

    public static void reset() {
        MAP.clear();
    }

    public static int wheelFlag() {
        return StatePayload.WHEEL;
    }
}
