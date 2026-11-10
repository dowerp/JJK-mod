package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class HeianTasks {
    private HeianTasks() {}

    private static final int KAMUTOKE_BOLTS = 7;

    public static boolean castKamutoke(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        long now = w.getTime();
        s.busyUntil = now + 62;
        s.kamutokeUntil = now + 76;
        double damage = 38.0 * s.power();
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0F, 1.7F);

        Tasks.add(new Task() {
            private int t = 0;
            private int fired = 0;
            private int next = 18;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved()) {
                    s.kamutokeUntil = 0L;
                    return true;
                }
                t++;
                double k = Players.HEIAN_HEIGHT / 1.8;
                Vec3d tip = new Vec3d(p.getX(), p.getY() + 2.3 * k, p.getZ());
                if (t < 18) {
                    double a = t * 0.8;
                    Ink.dot(w, tip.add(Math.cos(a) * 0.5, 0.0, Math.sin(a) * 0.5), Ink.GOLD, 0.7F);
                    Ink.dot(w, tip.add(Math.cos(a + 3.14) * 0.5, 0.2, Math.sin(a + 3.14) * 0.5), Ink.WHITE, 0.6F);
                    if (t % 4 == 0) {
                        Ink.bolt(w, tip.add(0, 1.4, 0), tip, Ink.WHITE, Ink.GOLD, 0.35F);
                    }
                    if (t == 10) {
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.6F, 0.6F);
                        p.swingHand(Hand.MAIN_HAND, true);
                    }
                    return false;
                }
                if (fired < KAMUTOKE_BOLTS && t >= next) {
                    next = t + 4 + ThreadLocalRandom.current().nextInt(3);
                    fired++;
                    strike(tip);
                }
                if (t % 3 == 0) {
                    Ink.bolt(w, tip, tip.add((ThreadLocalRandom.current().nextDouble() - 0.5) * 1.6, 0.8 + ThreadLocalRandom.current().nextDouble(), (ThreadLocalRandom.current().nextDouble() - 0.5) * 1.6), Ink.WHITE, Ink.GOLD, 0.3F);
                }
                if (fired >= KAMUTOKE_BOLTS && t >= next + 6) {
                    s.kamutokeUntil = 0L;
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.2F, 1.2F);
                    return true;
                }
                return false;
            }

            private void strike(Vec3d tip) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                double ang = r.nextDouble() * Math.PI * 2.0;
                double dist = 4.0 + r.nextDouble() * 9.0;
                double x = p.getX() + Math.cos(ang) * dist;
                double z = p.getZ() + Math.sin(ang) * dist;
                int sy = Cut.surfaceY(w, MathHelper.floor(x), MathHelper.floor(z), MathHelper.floor(p.getY()));
                double y = sy == Integer.MIN_VALUE ? p.getY() : sy + 1.0;
                Vec3d at = new Vec3d(x, y, z);

                LightningEntity lightning = EntityType.LIGHTNING_BOLT.create(w, null);
                if (lightning != null) {
                    lightning.refreshPositionAfterTeleport(at.x, at.y, at.z);
                    w.spawnEntity(lightning);
                }
                Ink.bolt(w, at.add(0, 24, 0), at, Ink.WHITE, Ink.BLACK, 0.9F);
                Ink.stroke(w, tip, at.add(0, 1.0, 0), Ink.GOLD, Ink.BLACK, 0.4F, true);
                Ink.ring(w, at.add(0, 0.1, 0), 3.0, Ink.GOLD, 0.8F);
                Ink.impact(w, at.add(0, 0.8, 0), new Vec3d(0, 1, 0), Ink.GOLD);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(4.5), x2 -> Aim.valid(p, x2))) {
                    Dmg.hit(p, e, damage);
                }
                Fx.sound(w, at.x, at.y, at.z, SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.PLAYERS, 2.5F, 0.8F);
                p.swingHand(fired % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
            }
        });
        return true;
    }

    public static boolean castKamaitachi(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int i = 0; i < 6; i++) {
            Vec3d offset = new Vec3d((rnd.nextDouble() - 0.5) * 4.0, (rnd.nextDouble() - 0.5) * 2.0, (rnd.nextDouble() - 0.5) * 4.0);
            Vec3d center = p.getEyePos().add(look.multiply(5.0 + i * 2.5)).add(offset);
            AnimeVfx.animeSlash(w, center, look, 3.2, rnd.nextDouble() * Math.PI);
        }

        Interact.cutProjectiles(w, p, p.getEyePos(), p.getEyePos().add(look.multiply(24.0)), 3.5);
        Box sweepBox = p.getBoundingBox().stretch(look.multiply(24.0)).expand(3.0);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, sweepBox, x -> Aim.valid(p, x))) {
            Dmg.hit(p, e, 24.0 * s.power());
        }

        p.swingHand(Hand.MAIN_HAND, true);
        return true;
    }

    public static boolean castTrueWorldSlash(ServerPlayerEntity p, SukunaState s) {
        return WorldSlashTask.cast(p, s, true);
    }

    public static boolean castFourArmFlurry(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        LivingEntity target = Aim.target(p, 12.0);
        Vec3d focus = target != null
            ? new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ())
            : p.getEyePos().add(p.getRotationVec(1.0F).multiply(5.0));
        s.busyUntil = w.getTime() + 20;
        double damage = 9.0 * s.power();

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved()) return true;
                if (t % 2 == 0) {
                    int arm = (t / 2) % 4;
                    ThreadLocalRandom r = ThreadLocalRandom.current();
                    Vec3d dir = focus.subtract(p.getEyePos()).normalize();
                    double roll = arm * Math.PI / 4.0 + r.nextDouble() * 0.3;
                    Ink.slash(w, focus.add((r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 1.2, (r.nextDouble() - 0.5) * 1.2), dir, roll, 2.6, arm % 2 == 0 ? Ink.BLOOD : Ink.WHITE);
                    Box box = new Box(focus, focus).expand(2.4);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                        Dmg.hit(p, e, damage);
                        Ink.impact(w, e.getEyePos(), dir, Ink.BLOOD);
                    }
                    p.swingHand(arm % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                    Fx.sound(w, focus.x, focus.y, focus.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 1.0F + arm * 0.12F);
                }
                t++;
                return t >= 16;
            }
        });
        return true;
    }
}
