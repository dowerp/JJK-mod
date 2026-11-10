package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class YujiTasks {
    private YujiTasks() {}

    public static boolean castDivergentFist(ServerPlayerEntity p, SukunaState s) {
        LivingEntity target = Aim.target(p, 4.5);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }

        ServerWorld w = Fx.world(p);
        long now = w.getTime();

        boolean focused = now <= s.focusUntil;
        boolean chained = now <= s.divergentTimingExpires
            && s.divergentTargetUuid != null && s.divergentTargetUuid.equals(target.getUuid());
        if (focused || chained) {
            landBlackFlash(p, s, target, w);
            return true;
        }

        Dmg.hit(p, target, 12.0 * s.power());
        Vec3d hitPos = target.getEyePos().add(0, -0.3, 0);
        Ink.impact(w, hitPos, p.getRotationVec(1.0F), Ink.BLUE);
        Fx.sound(w, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.2F, 0.8F);
        p.swingHand(Hand.MAIN_HAND, true);

        s.divergentTimingExpires = now + 14L;
        s.divergentTargetUuid = target.getUuid();

        Tasks.add(new DelayedCursedImpact(p, target, s.power()));
        return true;
    }

    private static void landBlackFlash(ServerPlayerEntity p, SukunaState s, LivingEntity target, ServerWorld w) {
        s.focusUntil = 0L;
        s.divergentTimingExpires = 0L;
        s.divergentTargetUuid = null;
        BlackFlash.open(p, s, target, 60.0 * s.power(), w, Ability.UTILITY_TECHNIQUE);
        p.swingHand(Hand.MAIN_HAND, true);
    }

    private static class DelayedCursedImpact implements Task {
        private final ServerPlayerEntity owner;
        private final LivingEntity target;
        private final double power;
        private int delay = 5;

        public DelayedCursedImpact(ServerPlayerEntity owner, LivingEntity target, double power) {
            this.owner = owner;
            this.target = target;
            this.power = power;
        }

        @Override
        public boolean tick() {
            if (delay-- > 0) return false;
            if (target.isAlive()) {
                ServerWorld w = Fx.world(owner);
                Vec3d pos = target.getEyePos().add(0, -0.3, 0);
                AnimeVfx.ringShockwave(w, pos, 0.9, AnimeVfx.CE_BLUE);
                Dmg.hit(owner, target, 16.0 * power);
                Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.6F);
            }
            return true;
        }
    }

    public static boolean castCursedKick(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        p.setVelocity(look.multiply(1.5).add(0, 0.3, 0));
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));

        Tasks.add(new Task() {
            private int t = 0;
            @Override
            public boolean tick() {
                t++;
                Vec3d pos = new Vec3d(p.getX(), p.getY(), p.getZ());
                Ink.stroke(w, pos.add(0, 0.8, 0), pos.add(0, 0.8, 0).subtract(look.multiply(1.6)), Ink.WHITE, Ink.BLUE, 0.5F, true);
                Box hitBox = p.getBoundingBox().expand(1.2);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, hitBox, x -> Aim.valid(p, x))) {
                    Dmg.hit(p, e, 22.0 * s.power());
                    e.setVelocity(look.multiply(2.2).add(0, 0.5, 0));
                    e.velocityDirty = true;
                    AnimeVfx.ringShockwave(w, e.getEyePos(), 1.4, ParticleTypes.SWEEP_ATTACK);
                    Fx.sound(w, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.0F, 1.8F);
                    return true;
                }
                return t >= 10;
            }
        });
        return true;
    }

    public static boolean triggerFocusBlackFlash(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        s.focusUntil = w.getTime() + 40L;
        p.sendMessage(Text.translatable("sukuna.msg.focusing_kokusen"), true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_DEPLETE, SoundCategory.PLAYERS, 1.2F, 1.8F);

        Tasks.add(new Task() {
            private int t = 0;
            @Override
            public boolean tick() {
                if (!p.isAlive() || w.getTime() > s.focusUntil || s.focusUntil == 0L) return true;
                if (t++ % 3 == 0) {
                    Vec3d hand = p.getEyePos().add(p.getRotationVec(1.0F).multiply(0.9)).add(0.0, -0.35, 0.0);
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    for (int i = 0; i < 2; i++) {
                        Vec3d d = new Vec3d(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().multiply(0.6);
                        Ink.bolt(w, hand, hand.add(d), Ink.BLACK, Ink.RED, 0.35F);
                    }
                    Ink.dot(w, hand, Ink.RED, 0.9F);
                }
                return false;
            }
        });
        return true;
    }

    public static boolean castBlackFlashBarrage(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        LivingEntity target = Aim.target(p, 20.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        double power = s.power();
        s.busyUntil = w.getTime() + 52;
        Vec3d dir = new Vec3d(target.getX() - p.getX(), 0.0, target.getZ() - p.getZ());
        dir = dir.lengthSquared() < 1.0E-4 ? p.getRotationVec(1.0F) : dir.normalize();
        Vec3d stand = new Vec3d(target.getX(), target.getY(), target.getZ()).subtract(dir.multiply(1.6));
        p.requestTeleport(stand.x, stand.y, stand.z);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.5F, 0.7F);

        Tasks.add(new Task() {
            private int t = 0;
            private int hits = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || !target.isAlive()) return true;
                t++;
                if (t % 7 != 0) return false;
                hits++;
                boolean last = hits >= 6;
                Vec3d at = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.6, target.getZ());
                Dmg.hit(p, target, (last ? 45.0 : 14.0) * power);
                Ink.bolt(w, at.add(0.0, 1.6, 0.0), at, Ink.BLACK, Ink.RED, last ? 0.9F : 0.5F);
                Ink.impact(w, at, p.getRotationVec(1.0F), last ? Ink.RED : Ink.BLACK);
                p.swingHand(hits % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, last ? 2.0F : 1.0F, last ? 0.6F : 1.4F);
                if (last) {
                    Vec3d push = p.getRotationVec(1.0F).multiply(2.4).add(0.0, 0.5, 0.0);
                    target.setVelocity(push);
                    target.velocityDirty = true;
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    public static boolean castReinforcement(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 1, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0, false, true));
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 1.0, Ink.BLUE, 0.7F);
        Ink.ring(w, base.add(0, 0.9, 0), 1.3, Ink.WHITE, 0.5F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0F, 1.4F);
        p.sendMessage(Text.translatable("sukuna.msg.yuji_focus"), true);
        return true;
    }

    public static boolean castYujiDomain(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d center = new Vec3d(p.getX(), p.getY(), p.getZ());
        s.domainLeft = 360;
        s.busyUntil = w.getTime() + 40;

        AnimeVfx.ringShockwave(w, center.add(0, 0.2, 0), 14.0, ParticleTypes.WHITE_ASH);
        SphereShell.start(w, center.add(0, 4.0, 0), 14, 360, DomainShell.Style.YUJI);
        Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 2.5F, 0.5F);

        Tasks.add(new Task() {
            private int ticks = 0;
            @Override
            public boolean tick() {
                ticks++;
                if (ticks > 360 || !p.isAlive()) return true;
                if (ticks % 10 == 0) {
                    Box box = new Box(center.x - 14, center.y - 6, center.z - 14, center.x + 14, center.y + 10, center.z + 14);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                        Dmg.hit(p, e, 6.0 * s.power());
                        AnimeVfx.animeSlash(w, e.getEyePos(), p.getRotationVec(1.0F), 1.2, ticks * 0.2);
                    }
                }
                return false;
            }
        });
        return true;
    }
}