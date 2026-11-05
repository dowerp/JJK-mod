package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class M1 {
    public static final int COOLDOWN_TICKS = 10;
    public static final int CHAIN_WINDOW_TICKS = 24;
    public static final double REACH = 3.4;

    public static final int ANIM_PUNCH = 2;
    public static final int ANIM_KICK = 3;
    public static final int ANIM_SLASH = 4;

    private M1() {
    }

    public static void handle(ServerPlayerEntity p) {
        if (SukunaMod.config.legacyBindings) return;
        if (!p.isAlive() || p.isSpectator() || !p.getMainHandStack().isEmpty()) return;
        SukunaState s = Players.get(p);
        s.character = Players.character(p);
        if (!s.character.playable || s.charging || s.piercingCharging) return;
        ServerWorld w = Fx.world(p);
        long now = w.getTime();
        if (now < s.m1Until) return;
        LivingEntity t = Aim.target(p, REACH);
        if (t == null || !p.canSee(t)) return;

        s.m1Until = now + COOLDOWN_TICKS;
        s.m1Chain = now <= s.m1ChainUntil ? (s.m1Chain + 1) % 4 : 0;
        s.m1ChainUntil = now + CHAIN_WINDOW_TICKS;

        double dmg = ThreadLocalRandom.current().nextDouble(4.0, 6.0) * s.power();
        Dmg.hit(p, t, dmg);

        Vec3d push = new Vec3d(t.getX() - p.getX(), 0.0, t.getZ() - p.getZ());
        if (push.lengthSquared() > 1.0E-4) {
            double k = s.m1Chain == 3 ? 0.6 : 0.2;
            t.setVelocity(t.getVelocity().add(push.normalize().multiply(k)).add(0.0, s.m1Chain == 3 ? 0.2 : 0.0, 0.0));
            t.velocityDirty = true;
        }

        p.swingHand(Hand.MAIN_HAND, true);
        boolean blade = s.form == SukunaForm.SUKUNA || s.form == SukunaForm.HEIAN;
        int anim = blade ? ANIM_SLASH : (s.m1Chain % 2 == 0 ? ANIM_PUNCH : ANIM_KICK);
        Players.broadcastNamedAnim(p, anim);
    }
}
