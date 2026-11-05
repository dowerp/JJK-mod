package com.dowerp.sukuna.client;

import com.dowerp.sukuna.Ability;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.net.StatePayload;

public final class ClientState {
    public static int ce;
    public static int maxCe;
    public static int flags;
    public static int domainLeft;
    public static int adaptation;
    public static SukunaForm form = SukunaForm.YUJI;
    public static int kills;
    public static int character;
    public static int meterTenths = -1;
    private static int formEndTick;
    private static boolean synced;
    public static Ability lastCast;
    public static int lastCastTick = -1000;

    public static int clientTick;

    public static float wheelAngle;
    public static int wheelClickTick = -100;
    private static int lastAdaptation;

    private static long castStartMs = Long.MIN_VALUE / 2;
    private static final int[] COOLDOWN_END = new int[Ability.values().length];

    private ClientState() {}

    public static void apply(StatePayload p) {
        ce = p.ce();
        maxCe = p.maxCe();
        flags = p.flags();
        domainLeft = p.domainLeft();
        adaptation = p.adaptation();
        if (p.form() >= 0 && p.form() < SukunaForm.values().length) {
            form = SukunaForm.values()[p.form()];
        }
        kills = p.kills();
        character = p.character();
        formEndTick = p.formLeft() > 0 ? clientTick + p.formLeft() : 0;
        Ability[] all = Ability.values();
        for (int i = 0; i < COOLDOWN_END.length; i++) {
            int remaining = i < p.cooldowns().size() ? p.cooldowns().get(i) : 0;
            int before = Math.max(0, COOLDOWN_END[i] - clientTick);
            if (synced && remaining > before + 8 && all[i] != Ability.TRANSFORM) {
                CastAnimations.play(all[i], form);
                lastCast = all[i];
                lastCastTick = clientTick;
            }
            COOLDOWN_END[i] = clientTick + remaining;
        }
        synced = true;
    }

    public static void tickClient() {
        clientTick++;
        float target = adaptation * 45.0F;
        wheelAngle += (target - wheelAngle) * 0.18F;
        if (adaptation != lastAdaptation) {
            if (adaptation > lastAdaptation) {
                wheelClickTick = clientTick;
            }
            lastAdaptation = adaptation;
        }
    }

    public static void markCast() {
        castStartMs = net.minecraft.util.Util.getMeasuringTimeMs();
    }

    public static float armsCastProgress() {
        float p = (net.minecraft.util.Util.getMeasuringTimeMs() - castStartMs) / 650.0F;
        return p >= 0.0F && p <= 1.0F ? p : -1.0F;
    }

    public static int formSecondsLeft() {
        return formEndTick > 0 ? Math.max(0, (formEndTick - clientTick + 19) / 20) : 0;
    }

    public static int cooldownLeft(Ability a) {
        return Math.max(0, COOLDOWN_END[a.ordinal()] - clientTick);
    }

    public static boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public static void reset() {
        ce = 0;
        maxCe = 0;
        flags = 0;
        domainLeft = 0;
        adaptation = 0;
        form = SukunaForm.YUJI;
        kills = 0;
        character = 0;
        meterTenths = -1;
        formEndTick = 0;
        synced = false;
        wheelAngle = 0.0F;
        lastAdaptation = 0;
        java.util.Arrays.fill(COOLDOWN_END, 0);
    }
}
