package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class SukunaTasks {
    private SukunaTasks() {
    }

    public static boolean dismantleStorm(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        s.busyUntil = w.getTime() + 30;
        p.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.6F, 0.6F);
        double damage = 7.0 * s.power();

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved()) return true;
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Vec3d c = new Vec3d(p.getX(), p.getY() + 1.2, p.getZ());
                if (t % 2 == 0) {
                    for (int k = 0; k < 3; k++) {
                        Vec3d off = new Vec3d(r.nextDouble() - 0.5, (r.nextDouble() - 0.5) * 0.6, r.nextDouble() - 0.5).normalize().multiply(2.5 + r.nextDouble() * 5.5);
                        Vec3d dir = new Vec3d(r.nextDouble() - 0.5, (r.nextDouble() - 0.5) * 0.5, r.nextDouble() - 0.5).normalize();
                        Ink.slash(w, c.add(off), dir, r.nextDouble() * Math.PI, 1.8 + r.nextDouble() * 1.4, k == 0 ? Ink.BLOOD : Ink.WHITE);
                    }
                }
                if (t % 6 == 0) {
                    Box box = new Box(c.x - 9, c.y - 4, c.z - 9, c.x + 9, c.y + 5, c.z + 9);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                        if (e.squaredDistanceTo(c.x, c.y, c.z) > 81.0) continue;
                        Dmg.hit(p, e, damage);
                        Ink.impact(w, e.getEyePos(), c.subtract(e.getEyePos()), Ink.BLOOD);
                    }
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 0.7F + t * 0.02F);
                    p.swingHand(t % 12 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                }
                t++;
                return t >= 30;
            }
        });
        return true;
    }

    public static boolean blackFlash(ServerPlayerEntity p, SukunaState s) {
        LivingEntity target = Aim.target(p, 9.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        Vec3d to = new Vec3d(target.getX() - p.getX(), 0.0, target.getZ() - p.getZ());
        if (to.lengthSquared() > 4.0) {
            p.setVelocity(to.normalize().multiply(1.7).add(0.0, 0.2, 0.0));
            p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
        }
        Ink.stroke(w, p.getEyePos().add(0, -0.4, 0), target.getEyePos(), Ink.BLACK, Ink.RED, 0.7F, true);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || !target.isAlive()) return true;
                t++;
                if (p.squaredDistanceTo(target) <= 5.0 || t >= 8) {
                    BlackFlash.open(p, s, target, 70.0 * s.power(), w, Ability.BLACK_FLASH);
                    p.swingHand(Hand.MAIN_HAND, true);
                    return true;
                }
                return false;
            }
        });
        return true;
    }
}
