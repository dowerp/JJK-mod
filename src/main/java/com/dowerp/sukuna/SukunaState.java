package com.dowerp.sukuna;

import java.util.UUID;

public final class SukunaState {
    public static final int HEIAN_KILLS = 20;
    public static final int OPEN_DOMAIN_KILLS = 5;
    public static final int PLAYER_KILL_VALUE = 5;
    public static final int HEIAN_DURATION_TICKS = 1200;
    public static final int GOJO_AWAKEN_TICKS = 900;
    public static final int GOJO_AWAKEN_COOLDOWN = 2400;
    public static final int CHOSO_WING_TICKS = 900;
    public static final int OPEN_DOMAIN_FINGERS = 5;
    public static final int MAX_FINGERS = 20;
    public static final int TODO_AWAKEN_TICKS = 900;
    public static final double METER_MAX = 100.0;

    public SukunaCharacter character = SukunaCharacter.ITADORI;
    public SukunaForm form = SukunaForm.YUJI;
    public double ce = 100.0;
    public int fingers = 1;
    public boolean initialized;
    public final long[] cooldowns = new long[Ability.values().length];

    public int hostileKills = 0;
    public boolean heianUnlocked = false;

    public long divergentTimingExpires = 0L;
    public UUID divergentTargetUuid = null;
    public long focusUntil = 0L;

    public boolean wheel;
    public float wheelAngle;
    public float wheelTarget;
    public final Adaptation adaptation = new Adaptation();

    public long busyUntil;
    public int domainLeft;
    public boolean inDomain;
    public boolean charging;
    public UUID mahoraga;
    public int lastFlags = -1;

    public long formUntil = 0L;
    public long redScaleUntil = 0L;
    public boolean redScaleStack = false;
    public long hardenUntil = 0L;
    public long formCooldownUntil = 0L;

    public int heldMask;
    public long kamutokeUntil;
    public boolean piercingCharging;
    public long runUntil;
    public long combatUntil;
    public boolean noCooldown;
    public long infinityOffUntil;
    public double domainMeter;
    public int lastMeterSent = -1;
    public boolean lowHpAwakenUsed;
    public boolean awakenedViaMeter;
    public long convergenceUntil;
    public int convergenceCharges;
    public int stillTicks;
    public double lastX;
    public double lastZ;
    public double convergenceMul = 1.0;
    public long m1Until;
    public int m1Chain;
    public long m1ChainUntil;
    public long lastDamagedAt = Long.MIN_VALUE / 2;
    public boolean mahoragaSeen;
    public long todoBuffUntil;
    public boolean sealing;
    public boolean deferCharge;
    public long makiSenseUntil;
    public final java.util.Map<java.util.UUID, Integer> squeeze = new java.util.HashMap<>();
    public String headVictim = "";
    public long headUntil;

    public boolean vessel() {
        return character == SukunaCharacter.GOJO || character == SukunaCharacter.MAKI || fingers > 0;
    }

    public int statFingers() {
        return character == SukunaCharacter.ITADORI ? MAX_FINGERS : fingers;
    }

    public boolean openDomainReady() {
        return fingers >= OPEN_DOMAIN_FINGERS || hostileKills >= OPEN_DOMAIN_KILLS;
    }

    public boolean meterOn() {
        return SukunaMod.config.domainMeterEnabled;
    }

    public void addMeter(double amount) {
        if (!meterOn() || amount <= 0.0 || form == SukunaForm.MAKI) return;
        domainMeter = Math.min(METER_MAX, domainMeter + amount);
    }

    public boolean meterFull() {
        return meterOn() && form != SukunaForm.MAKI && domainMeter >= METER_MAX - 1.0E-6;
    }

    public boolean held(Ability a) {
        return (heldMask & (1 << a.ordinal())) != 0;
    }

    public double maxCe() {
        return switch (form) {
            case YUJI -> 80.0 + 8.0 * statFingers();
            case SUKUNA -> 150.0 + 10.0 * statFingers();
            case HEIAN -> 220.0 + 12.0 * statFingers();
            case GOJO -> 300.0;
            case GOJO_AWAKENED -> 400.0;
            case CHOSO -> 250.0;
            case CHOSO_WING -> 380.0;
            case TODO -> 260.0;
            case TODO_AWAKENED -> 420.0;
            case MAKI -> 0.0;
        };
    }

    public double power() {
        double base = switch (form) {
            case YUJI -> 1.0 + 0.03 * statFingers();
            case SUKUNA -> 1.25 + 0.04 * statFingers();
            case HEIAN -> 1.6 + 0.05 * statFingers();
            case GOJO -> 1.1;
            case GOJO_AWAKENED -> 1.6;
            case CHOSO -> 1.15;
            case CHOSO_WING -> 1.5;
            case TODO -> 1.2;
            case TODO_AWAKENED -> 1.6;
            case MAKI -> 1.8;
        };
        return base * convergenceMul * SukunaMod.config.damageMultiplier;
    }
}
