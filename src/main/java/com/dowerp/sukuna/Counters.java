package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Контрприёмы: «Простая территория» гасит гарантированное попадание чужих территорий,
 * «Падающий цветок» отвечает автоматическим контрударом на первые удары по владельцу.
 */
public final class Counters {
    public static final int SIMPLE_TICKS = 80;
    public static final int BLOSSOM_TICKS = 60;
    public static final int BLOSSOM_HITS = 3;
    public static final double BLOSSOM_RADIUS = 6.0;

    private Counters() {
    }

    /** Территория владельца не наносит урон и эффекты цели, пока у неё активна «Простая территория». */
    public static boolean nullifies(LivingEntity e) {
        if (!(e instanceof ServerPlayerEntity sp)) return false;
        SukunaState s = Players.peek(sp);
        return s != null && s.simpleUntil > Fx.world(sp).getTime();
    }

    public static boolean castSimpleDomain(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        long now = w.getTime();
        if (s.simpleUntil > now) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.simpleUntil = now + SIMPLE_TICKS;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, SIMPLE_TICKS, 1, false, false));
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2F, 1.9F);
        p.sendMessage(Text.translatable("sukuna.msg.simple_domain"), true);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.simpleUntil <= w.getTime()) {
                    s.simpleUntil = 0L;
                    return true;
                }
                if (t++ % 4 == 0) {
                    Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 2.6, Ink.WHITE, 0.35F);
                }
                return false;
            }
        });
        return true;
    }

    public static boolean castFallingBlossom(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        long now = w.getTime();
        if (s.blossomUntil > now) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.blossomUntil = now + BLOSSOM_TICKS;
        s.blossomHits = BLOSSOM_HITS;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, BLOSSOM_TICKS, 3, false, false));
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_CHERRY_LEAVES_PLACE, SoundCategory.PLAYERS, 1.6F, 0.8F);
        p.sendMessage(Text.translatable("sukuna.msg.falling_blossom"), true);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.blossomUntil <= w.getTime() || s.blossomHits <= 0) {
                    s.blossomUntil = 0L;
                    s.blossomHits = 0;
                    return true;
                }
                if (t++ % 3 == 0) {
                    Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.8 + (t % 9) * 0.1, p.getZ()), 1.8, Ink.BLOOD, 0.3F);
                }
                return false;
            }
        });
        return true;
    }

    /** Контрудар «Падающего цветка» по атакующему. */
    public static void blossomStrike(ServerPlayerEntity p, SukunaState s, LivingEntity attacker, ServerWorld w) {
        Dmg.hit(p, attacker, 16.0 * s.power());
        Vec3d away = new Vec3d(attacker.getX() - p.getX(), 0.0, attacker.getZ() - p.getZ());
        if (away.lengthSquared() > 1.0E-4) {
            attacker.setVelocity(away.normalize().multiply(1.2).add(0.0, 0.3, 0.0));
            attacker.velocityDirty = true;
        }
        Vec3d at = new Vec3d(attacker.getX(), attacker.getY() + attacker.getHeight() * 0.6, attacker.getZ());
        Ink.impact(w, at, away.lengthSquared() > 1.0E-4 ? away.normalize() : new Vec3d(1, 0, 0), Ink.WHITE);
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.6, Ink.BLOOD, 0.6F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.5F, 1.6F);
        if (s.blossomHits <= 0) {
            s.blossomUntil = 0L;
        }
    }
}
