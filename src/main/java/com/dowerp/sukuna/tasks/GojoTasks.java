package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.entity.TechniqueOrbEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class GojoTasks {
    private GojoTasks() {
    }

    public static final class LingeringBlue {
        final ServerWorld world;
        final java.util.UUID owner;
        final Vec3d center;
        final TechniqueOrbEntity orb;
        long expire;
        boolean used;

        LingeringBlue(ServerWorld world, java.util.UUID owner, Vec3d center, TechniqueOrbEntity orb, long expire) {
            this.world = world;
            this.owner = owner;
            this.center = center;
            this.orb = orb;
            this.expire = expire;
        }
    }

    private static final List<LingeringBlue> LINGERING = new ArrayList<>();
    private static final double COMBINED_RADIUS = 24.0;

    public static void clearLingering() {
        for (LingeringBlue b : LINGERING) {
            dropOrb(b.orb);
        }
        LINGERING.clear();
    }

    private static boolean absorbIntoBlue(ServerWorld w, ServerPlayerEntity p, SukunaState s, Vec3d redPos) {
        for (LingeringBlue b : LINGERING) {
            if (b.used || b.world != w || b.orb.isRemoved() || !b.owner.equals(p.getUuid())) continue;
            if (b.center.squaredDistanceTo(redPos) > 3.5 * 3.5) continue;
            b.used = true;
            combinedBlast(w, p, s, b.center);
            dropOrb(b.orb);
            return true;
        }
        return false;
    }

    private static void combinedBlast(ServerWorld w, ServerPlayerEntity p, SukunaState s, Vec3d c) {
        TechniqueOrbEntity blue = spawnOrb(w, ModEntities.ORB_BLUE, c, BLUE_SIZE, 60);
        TechniqueOrbEntity red = spawnOrb(w, ModEntities.ORB_RED, c, RED_SIZE, 60);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.MASTER, 3.0F, 0.9F);
        final double dmg = PURPLE_DAMAGE * s.power();

        Tasks.add(new Task() {
            private int t = 0;
            private TechniqueOrbEntity purple;
            private double wave = 0.0;
            private final Set<UUID> hit = new HashSet<>();

            @Override
            public boolean tick() {
                t++;
                if (t <= 36) {
                    double k = t / 36.0;
                    double ang = t * 0.55;
                    double rr = 3.2 * (1.0 - k);
                    placeOrb(blue, c.add(Math.cos(ang) * rr * 0.6, Math.sin(ang * 0.7) * rr * 0.3, Math.sin(ang) * rr * 0.6), BLUE_SIZE);
                    placeOrb(red, c.add(-Math.cos(ang) * rr, -Math.sin(ang * 0.7) * rr * 0.3, -Math.sin(ang) * rr), RED_SIZE);
                    if (t % 4 == 0) {
                        Ink.ring(w, c, 0.5 + k * 1.2, Ink.PURPLE, 0.4F);
                        Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.MASTER, 1.6F, 0.5F + (float) k);
                    }
                    return false;
                }
                if (t == 37) {
                    dropOrb(blue);
                    dropOrb(red);
                    purple = spawnOrb(w, ModEntities.ORB_PURPLE, c, PURPLE_SIZE, 90);
                    Ink.impact(w, c, new Vec3d(0, 1, 0), Ink.PURPLE);
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 3.0F, 1.2F);
                }
                if (t <= 62) {
                    placeOrb(purple, c.add(0.0, Math.sin(t * 0.8) * 0.12, 0.0), PURPLE_SIZE);
                    if (t % 5 == 0) {
                        Ink.ring(w, c, 2.0 + (t - 37) * 0.18, Ink.DEEP_PURPLE, 0.6F);
                        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.5F, 0.5F + (t - 37) * 0.02F);
                    }
                    return false;
                }
                if (t == 63) {
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 4.0F, 0.5F);
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 4.0F, 0.6F);
                    Ink.impact(w, c, new Vec3d(0, 1, 0), Ink.WHITE);
                }
                double prev = wave;
                wave = Math.min(COMBINED_RADIUS, wave + 1.1);
                Ink.ring(w, c, wave, Ink.PURPLE, 1.0F);
                Ink.ring(w, c.add(0, 1.2, 0), wave * 0.85, Ink.WHITE, 0.8F);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(wave), x -> Aim.valid(p, x) && !hit.contains(x.getUuid()))) {
                    double d = Math.sqrt(e.squaredDistanceTo(c.x, c.y, c.z));
                    if (d > wave || d < prev - 1.0) continue;
                    hit.add(e.getUuid());
                    Dmg.hit(p, e, dmg);
                    Vec3d away = new Vec3d(e.getX() - c.x, 0.4, e.getZ() - c.z);
                    e.setVelocity(away.normalize().multiply(2.5));
                    e.velocityDirty = true;
                }
                if (SukunaMod.config.blockDamage) {
                    int r = (int) Math.min(8.0, wave);
                    int lo = (int) Math.max(0.0, prev);
                    for (int dx = -r; dx <= r; dx++) {
                        for (int dy = -r; dy <= r; dy++) {
                            for (int dz = -r; dz <= r; dz++) {
                                int d2 = dx * dx + dy * dy + dz * dz;
                                if (d2 <= r * r && d2 >= lo * lo) {
                                    Cut.removeSafe(w, BlockPos.ofFloored(c.x + dx, c.y + dy, c.z + dz), p);
                                }
                            }
                        }
                    }
                }
                if (wave >= COMBINED_RADIUS) {
                    dropOrb(purple);
                    return true;
                }
                return false;
            }
        });
    }

    public static final double BLUE_SIZE = 1.4;
    public static final double RED_SIZE = 1.4;
    public static final double PURPLE_SIZE = 4.0;

    private static TechniqueOrbEntity spawnOrb(ServerWorld w, EntityType<TechniqueOrbEntity> type, Vec3d center, double size, int life) {
        TechniqueOrbEntity e = type.create(w, SpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.refreshPositionAndAngles(center.x, center.y - size / 2.0, center.z, 0.0F, 0.0F);
        e.setLife(life);
        w.spawnEntity(e);
        return e;
    }

    private static void placeOrb(TechniqueOrbEntity e, Vec3d center, double size) {
        if (e != null && !e.isRemoved()) {
            e.moveTo(center.x, center.y - size / 2.0, center.z);
        }
    }

    private static void dropOrb(TechniqueOrbEntity e) {
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }

    private static EntityType<TechniqueOrbEntity> blueType(boolean full) {
        return full ? ModEntities.ORB_BLUE : ModEntities.ORB_BLUE_SMALL;
    }

    private static EntityType<TechniqueOrbEntity> redType(boolean full) {
        return full ? ModEntities.ORB_RED : ModEntities.ORB_RED_SMALL;
    }

    private static Vec3d handPos(ServerPlayerEntity p, boolean right, double up, double forward) {
        double yaw = Math.toRadians(p.getYaw());
        Vec3d fwd = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d r = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3d side = right ? r : r.multiply(-1.0);
        return new Vec3d(p.getX(), p.getY() + up, p.getZ()).add(side.multiply(0.5)).add(fwd.multiply(forward));
    }

    private static boolean solidAt(ServerWorld w, Vec3d v) {
        BlockPos bp = BlockPos.ofFloored(v.x, v.y, v.z);
        BlockState bs = w.getBlockState(bp);
        return !bs.isAir() && !bs.getCollisionShape(w, bp).isEmpty();
    }

    public static double infinityRadius(SukunaState s) {
        return s.form == SukunaForm.GOJO_AWAKENED ? 9.0 : 7.0;
    }

    public static void infinityTick(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.infinityToggledOff || now < s.infinityOffUntil) {
            return;
        }
        s.ce = Math.max(0.0, s.ce - 0.5);
        if (s.ce < 10.0) {
            s.infinityOffUntil = now + 60;
            p.sendMessage(Text.translatable("sukuna.msg.infinity_off"), true);
            Ink.distort(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 5.0);
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.2F, 1.4F);
            return;
        }
        if (p.age % 2 != 0) {
            return;
        }
        double radius = infinityRadius(s);
        double px = p.getX();
        double py = p.getY() + 1.0;
        double pz = p.getZ();
        Box box = p.getBoundingBox().expand(radius);
        for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, box, e -> e.getOwner() != p)) {
            double dx = pr.getX() - px;
            double dy = pr.getY() - py;
            double dz = pr.getZ() - pz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > radius * radius) {
                continue;
            }
            if (d2 < 9.0) {
                pr.setVelocity(new Vec3d(dx, dy, dz).normalize().multiply(0.18));
            } else {
                pr.setVelocity(pr.getVelocity().multiply(0.35));
            }
            pr.velocityDirty = true;
            if (p.age % 6 == 0) {
                Ink.distort(w, new Vec3d(pr.getX(), pr.getY(), pr.getZ()), 1.6);
            }
        }
        java.util.Set<UUID> squeezedNow = new HashSet<>();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
            double dx = e.getX() - px;
            double dz = e.getZ() - pz;
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > radius) {
                continue;
            }
            int amp = d < 3.8 ? 5 : d < 5.5 ? 3 : 1;
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8, amp, false, false));
            if (d >= 3.8 || d <= 1.0E-3) {
                continue;
            }
            Vec3d away = new Vec3d(dx / d, 0.0, dz / d);
            Vec3d mid = new Vec3d(e.getX(), e.getY() + Math.max(0.6, e.getHeight() * 0.5), e.getZ());
            boolean wall = false;
            for (double k = 0.4; k <= 1.6 && !wall; k += 0.6) {
                Vec3d probe = mid.add(away.multiply(e.getWidth() * 0.5 + k));
                wall = solidAt(w, probe) || solidAt(w, probe.add(0.0, 0.5, 0.0));
            }
            if (wall) {
                squeezedNow.add(e.getUuid());
                int ticks = s.squeeze.merge(e.getUuid(), 2, Integer::sum);
                e.setVelocity(Vec3d.ZERO);
                e.velocityDirty = true;
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 7, false, false));
                Dmg.hitQuiet(p, e, 3.5);
                if (ticks % 8 == 0) {
                    Ink.distort(w, mid, 2.4);
                    Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.5F, 0.5F + ticks * 0.01F);
                }
                if (ticks >= 40) {
                    Dmg.hit(p, e, 60.0);
                    Ink.impact(w, mid, away, Ink.WHITE);
                    Ink.distort(w, mid, 5.0);
                    Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 1.6F);
                    Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0F, 0.6F);
                    s.squeeze.remove(e.getUuid());
                    squeezedNow.remove(e.getUuid());
                }
            } else {
                Vec3d v = e.getVelocity();
                e.setVelocity(away.x * 0.5, Math.max(v.y, 0.0), away.z * 0.5);
                e.velocityDirty = true;
                if (p.age % 6 == 0) {
                    Ink.distort(w, mid.subtract(away.multiply(0.6)), 1.8);
                }
            }
        }
        s.squeeze.keySet().retainAll(squeezedNow);
    }

    public static void infinityFlash(ServerWorld w, ServerPlayerEntity p) {
        if (w.getTime() % 3 == 0) {
            Ink.distort(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 5.0);
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8F, 1.6F);
        }
    }

    public static boolean castBlue(ServerPlayerEntity p, SukunaState s, boolean max) {
        ServerWorld w = Fx.world(p);
        final boolean full = max;
        final double radius = SukunaMod.config.bluePullRadius;
        final int duration = 40;
        final double damage = (full ? 9.0 : 4.0) * s.power();
        final double burst = (full ? 30.0 : 12.0) * s.power();
        s.busyUntil = w.getTime() + 34;
        p.swingHand(Hand.OFF_HAND, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0F, 0.6F);
        Vec3d start = p.getEyePos().add(p.getRotationVec(1.0F).multiply(6.0));
        TechniqueOrbEntity orb = spawnOrb(w, blueType(full), start, BLUE_SIZE, duration + 10);

        Tasks.add(new Task() {
            private int t = 0;
            private boolean killed = false;
            private Vec3d center = start;

            @Override
            public boolean tick() {
                if (!p.isAlive()) {
                    dropOrb(orb);
                    return true;
                }
                center = p.getEyePos().add(p.getRotationVec(1.0F).multiply(6.0));
                placeOrb(orb, center.add(0.0, Math.sin(t * 0.25) * 0.1, 0.0), BLUE_SIZE);
                for (int i = 0; i < 2; i++) {
                    double a = t * 0.7 + i * Math.PI;
                    double rr = radius * (1.0 - ((t * 2 + i * 8) % 24) / 24.0);
                    Vec3d pos = center.add(Math.cos(a) * rr, Math.sin(a * 0.5) * rr * 0.4, Math.sin(a) * rr);
                    Ink.dot(w, pos, Ink.BLUE, 0.6F);
                }
                if (t % 8 == 0) {
                    Ink.ring(w, center, Math.max(0.5, radius * (1.0 - (t % 16) / 16.0)), Ink.BLUE, 0.4F);
                }
                if (t % 10 == 0) {
                    Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.PLAYERS, 1.2F, 1.6F);
                }

                Box box = new Box(center, center).expand(radius);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                    Vec3d to = center.subtract(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
                    double dist = to.length();
                    if (dist > radius || dist < 1.0E-3) continue;
                    Vec3d pull = to.normalize().multiply(Math.min(1.0, 0.3 + dist * 0.08));
                    e.setVelocity(e.getVelocity().multiply(0.4).add(pull));
                    e.velocityDirty = true;
                    if (t % 5 == 0) {
                        Dmg.hit(p, e, damage);
                        if (!e.isAlive()) killed = true;
                    }
                }
                for (Entity o : w.getEntitiesByClass(Entity.class, box, x -> (x instanceof ProjectileEntity pr && pr.getOwner() != p) || x instanceof ItemEntity)) {
                    Vec3d to = center.subtract(o.getX(), o.getY(), o.getZ());
                    double dist = to.length();
                    if (dist > radius || dist < 0.8) continue;
                    o.setVelocity(to.normalize().multiply(Math.min(1.2, 0.3 + dist * 0.1)));
                    o.velocityDirty = true;
                }
                t++;
                if (t >= duration) {
                    if (full && killed && orb != null && !orb.isRemoved()) {
                        orb.setLife(900);
                        LingeringBlue lb = new LingeringBlue(w, p.getUuid(), center, orb, w.getTime() + 600L);
                        LINGERING.add(lb);
                        Tasks.add(new Task() {
                            private int k = 0;

                            @Override
                            public boolean tick() {
                                k++;
                                if (lb.used || w.getTime() >= lb.expire || orb.isRemoved()) {
                                    LINGERING.remove(lb);
                                    if (!lb.used) dropOrb(orb);
                                    return true;
                                }
                                placeOrb(orb, lb.center.add(0.0, Math.sin(k * 0.2) * 0.1, 0.0), BLUE_SIZE);
                                if (k % 12 == 0) {
                                    Ink.ring(w, lb.center, 1.6, Ink.BLUE, 0.4F);
                                    Fx.sound(w, lb.center.x, lb.center.y, lb.center.z, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8F, 0.6F);
                                }
                                return false;
                            }
                        });
                        p.sendMessage(net.minecraft.text.Text.translatable("sukuna.msg.blue_lingers"), true);
                        return true;
                    }
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(radius * 0.7), x -> Aim.valid(p, x))) {
                        Dmg.hit(p, e, burst);
                    }
                    Ink.impact(w, center, new Vec3d(0, 1, 0), Ink.BLUE);
                    Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 2.0F, 0.8F);
                    dropOrb(orb);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    public static boolean castRed(ServerPlayerEntity p, SukunaState s, boolean max) {
        ServerWorld w = Fx.world(p);
        Vec3d dir = p.getRotationVec(1.0F);
        final boolean fullRed = max;
        double radius = max ? 9.0 : 4.5;
        double damage = (max ? 50.0 : 26.0) * s.power();
        s.busyUntil = w.getTime() + 26;
        p.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5F, 0.6F);
        Vec3d startPos = handPos(p, true, 1.3, 0.9).add(dir.multiply(0.6));
        TechniqueOrbEntity orb = spawnOrb(w, redType(fullRed), startPos, RED_SIZE, 60);

        Tasks.add(new Task() {
            private Vec3d pos = startPos;
            private int t = 0;
            private int warm = 8;

            @Override
            public boolean tick() {
                if (!p.isAlive()) {
                    dropOrb(orb);
                    return true;
                }
                if (warm > 0) {
                    warm--;
                    Vec3d hp = handPos(p, true, 1.3, 0.9).add(p.getRotationVec(1.0F).multiply(0.6));
                    pos = hp;
                    placeOrb(orb, hp, RED_SIZE);
                    if (warm % 3 == 0) {
                        Ink.ring(w, hp, 0.5 + warm * 0.08, Ink.RED, 0.3F);
                        Fx.sound(w, hp.x, hp.y, hp.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9F, 1.6F - warm * 0.05F);
                    }
                    return false;
                }
                for (int sub = 0; sub < 3; sub++) {
                    pos = pos.add(dir.multiply(0.6));
                    if (fullRed && absorbIntoBlue(w, p, s, pos)) {
                        dropOrb(orb);
                        return true;
                    }
                    boolean hitEntity = !w.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(1.4), x -> Aim.valid(p, x)).isEmpty();
                    BlockPos bp = BlockPos.ofFloored(pos.x, pos.y, pos.z);
                    BlockState bs = w.getBlockState(bp);
                    boolean hitBlock = !bs.isAir() && !bs.getCollisionShape(w, bp).isEmpty();
                    if (hitEntity || hitBlock) {
                        explode(pos);
                        return true;
                    }
                }
                placeOrb(orb, pos, RED_SIZE);
                Ink.stroke(w, pos.subtract(dir.multiply(3.0)), pos, Ink.RED, Ink.BLACK, 0.7F, true);
                Ink.dot(w, pos, Ink.RED, 1.2F);
                t++;
                if (t >= 30) {
                    explode(pos);
                    return true;
                }
                return false;
            }

            private void explode(Vec3d at) {
                dropOrb(orb);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(radius), x -> Aim.valid(p, x))) {
                    Vec3d away = new Vec3d(e.getX() - at.x, 0.0, e.getZ() - at.z);
                    if (away.lengthSquared() < 1.0E-4) away = dir;
                    Dmg.hit(p, e, damage);
                    e.setVelocity(away.normalize().multiply(2.2).add(0.0, 0.7, 0.0));
                    e.velocityDirty = true;
                }
                if (SukunaMod.config.blockDamage) {
                    int rad = max ? 4 : 2;
                    for (int dx = -rad; dx <= rad; dx++) {
                        for (int dy = -rad; dy <= rad; dy++) {
                            for (int dz = -rad; dz <= rad; dz++) {
                                if (dx * dx + dy * dy + dz * dz <= rad * rad + 1) {
                                    Cut.removeSafe(w, BlockPos.ofFloored(at.x + dx, at.y + dy, at.z + dz), p);
                                }
                            }
                        }
                    }
                }
                Ink.impact(w, at, dir.multiply(-1.0), Ink.RED);
                Ink.ring(w, at, radius * 0.6, Ink.RED, 0.8F);
                Ink.ring(w, at.add(0, 1.0, 0), radius * 0.35, Ink.WHITE, 0.5F);
                Ink.splatter(w, at, radius, 20, Ink.RED);
                Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 3.0F, 0.7F);
            }
        });
        return true;
    }

    private static void push(ServerPlayerEntity p, Vec3d dir) {
        p.setVelocity(dir);
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    public static boolean castCombo(ServerPlayerEntity p, SukunaState s) {
        LivingEntity tg = Aim.target(p, 9.0);
        if (tg == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        double power = s.power();
        s.busyUntil = w.getTime() + 20;

        Tasks.add(new Task() {
            private int t = 0;
            private int hits = 0;
            private int wait = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || !tg.isAlive()) return true;
                t++;
                if (p.distanceTo(tg) > 2.8) {
                    if (t > 14) return true;
                    Vec3d to = new Vec3d(tg.getX() - p.getX(), 0.0, tg.getZ() - p.getZ()).normalize().multiply(0.95);
                    push(p, new Vec3d(to.x, Math.max(0.0, p.getVelocity().y), to.z));
                    return false;
                }
                if (wait-- > 0) return false;
                wait = 1;
                boolean last = hits == 5;
                Dmg.hit(p, tg, (last ? 20.0 : 9.0) * power);
                Vec3d c = new Vec3d(tg.getX(), tg.getY() + tg.getHeight() * 0.6, tg.getZ());
                Vec3d dir = c.subtract(p.getEyePos());
                Ink.impact(w, c, dir, last ? Ink.BLUE : Ink.WHITE);
                p.swingHand(hits % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.2F, 0.9F + hits * 0.1F);
                if (last) {
                    Vec3d away = new Vec3d(tg.getX() - p.getX(), 0.0, tg.getZ() - p.getZ()).normalize().multiply(1.8).add(0.0, 0.4, 0.0);
                    tg.setVelocity(away);
                    tg.velocityDirty = true;
                    return true;
                }
                hits++;
                return false;
            }
        });
        return true;
    }

    public static boolean castStepKick(ServerPlayerEntity p, SukunaState s) {
        LivingEntity tg = Aim.target(p, 14.0);
        if (tg == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        double power = s.power();
        s.busyUntil = w.getTime() + 14;
        Ink.stroke(w, p.getEyePos().add(0, -0.4, 0), tg.getEyePos(), Ink.WHITE, Ink.BLUE, 0.6F, true);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || !tg.isAlive()) return true;
                t++;
                if (p.distanceTo(tg) > 2.6 && t < 9) {
                    Vec3d to = new Vec3d(tg.getX() - p.getX(), 0.0, tg.getZ() - p.getZ()).normalize().multiply(1.8);
                    push(p, new Vec3d(to.x, Math.max(0.0, p.getVelocity().y), to.z));
                    return false;
                }
                Vec3d dir = new Vec3d(tg.getX() - p.getX(), 0.0, tg.getZ() - p.getZ());
                dir = dir.lengthSquared() < 1.0E-4 ? p.getRotationVec(1.0F) : dir.normalize();
                Dmg.hit(p, tg, 30.0 * power);
                tg.setVelocity(dir.multiply(2.4).add(0.0, 0.45, 0.0));
                tg.velocityDirty = true;
                Vec3d c = new Vec3d(tg.getX(), tg.getY() + tg.getHeight() * 0.5, tg.getZ());
                Ink.slash(w, c, dir, 0.3, 2.2, Ink.BLUE);
                Ink.impact(w, c, dir, Ink.WHITE);
                p.swingHand(Hand.MAIN_HAND, true);
                Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.6F, 0.8F);
                return true;
            }
        });
        return true;
    }

    public static final double PURPLE_DAMAGE = 100.0;
    private static final double PURPLE_RANGE = 64.0;
    private static final double PURPLE_SPEED = 3.0;
    private static final double PURPLE_HIT_RADIUS = 8.0;
    private static final int PURPLE_CUT_RADIUS = 4;

    public static boolean castPurple(ServerPlayerEntity p, SukunaState s) {
        final boolean full = s.form == SukunaForm.GOJO_AWAKENED;
        final double purpleDamage = full ? PURPLE_DAMAGE : 30.0;
        final double purpleRange = full ? PURPLE_RANGE : 36.0;
        final double purpleSpeed = full ? PURPLE_SPEED : 2.0;
        final double purpleHit = full ? PURPLE_HIT_RADIUS : 2.6;
        final int purpleCut = full ? PURPLE_CUT_RADIUS : 1;
        final double purpleSize = full ? PURPLE_SIZE : 1.4;
        final EntityType<TechniqueOrbEntity> purpleType = full ? ModEntities.ORB_PURPLE : ModEntities.ORB_PURPLE_SMALL;
        ServerWorld w = Fx.world(p);
        final int chargeTicks = SukunaMod.config.purpleChargeTicks;
        final long startTick = w.getTime();
        s.busyUntil = w.getTime() + 62 + chargeTicks;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 3.0F, 1.2F);

        Tasks.add(new Task() {
            private int t = 0;
            private int pre = 0;
            private boolean selfHurt;
            private boolean fired;
            private TechniqueOrbEntity blueOrb;
            private TechniqueOrbEntity redOrb;
            private TechniqueOrbEntity purple;
            private Vec3d origin = Vec3d.ZERO;
            private Vec3d dir = Vec3d.ZERO;
            private double dist;
            private final Set<UUID> hit = new HashSet<>();

            @Override
            public boolean tick() {
                if (!p.isAlive()) {
                    dropOrb(blueOrb);
                    dropOrb(redOrb);
                    dropOrb(purple);
                    return true;
                }
                if (!fired) {
                    if (s.lastDamagedAt > startTick) {
                        dropOrb(blueOrb);
                        dropOrb(redOrb);
                        dropOrb(purple);
                        Fx.puff(w, net.minecraft.particle.ParticleTypes.CLOUD, p.getX(), p.getY() + 1.6, p.getZ(), 20, 0.5, 0.4, 0.5, 0.05);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
                        p.sendMessage(Text.translatable("sukuna.msg.purple_interrupted"), true);
                        s.busyUntil = w.getTime();
                        return true;
                    }
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 1, false, false));
                    if (pre < chargeTicks) {
                        pre++;
                        Vec3d lk = p.getRotationVec(1.0F);
                        Vec3d focus = p.getEyePos().add(lk.multiply(2.3));
                        if (pre % 3 == 0) {
                            Ink.ring(w, focus, 0.4 + (1.0 - (double) pre / chargeTicks) * 1.4, Ink.DEEP_PURPLE, 0.45F);
                        }
                        if (pre % 6 == 1) {
                            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.4F, 0.7F + 0.03F * pre);
                        }
                        return false;
                    }
                    t++;
                    Vec3d look = p.getRotationVec(1.0F);
                    Vec3d merge = p.getEyePos().add(look.multiply(2.3));
                    Vec3d leftHand = handPos(p, false, 2.1, 0.2);
                    Vec3d rightHand = handPos(p, true, 2.1, 0.2);
                    if (t == 6) {
                        blueOrb = spawnOrb(w, blueType(full), leftHand, BLUE_SIZE, 90);
                        Fx.sound(w, leftHand.x, leftHand.y, leftHand.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.6F, 0.8F);
                        p.swingHand(Hand.OFF_HAND, true);
                    }
                    if (t == 18) {
                        redOrb = spawnOrb(w, redType(full), rightHand, RED_SIZE, 90);
                        Fx.sound(w, rightHand.x, rightHand.y, rightHand.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.6F, 0.7F);
                        p.swingHand(Hand.MAIN_HAND, true);
                    }
                    if (t >= 6 && t < 24) {
                        placeOrb(blueOrb, leftHand, BLUE_SIZE);
                    }
                    if (t >= 18 && t < 24) {
                        placeOrb(redOrb, rightHand, RED_SIZE);
                    }
                    if (t >= 24 && t < 40) {
                        double k = (t - 24) / 16.0;
                        placeOrb(blueOrb, leftHand.add(merge.subtract(leftHand).multiply(k)), BLUE_SIZE);
                        placeOrb(redOrb, rightHand.add(merge.subtract(rightHand).multiply(k)), RED_SIZE);
                        if (t % 3 == 0) {
                            Ink.ring(w, merge, 0.3 + (1.0 - k) * 0.9, Ink.PURPLE, 0.35F);
                        }
                    }
                    if (t == 40) {
                        dropOrb(blueOrb);
                        dropOrb(redOrb);
                        purple = spawnOrb(w, purpleType, merge, purpleSize, 100);
                        Ink.impact(w, merge, look, Ink.PURPLE);
                        Fx.sound(w, merge.x, merge.y, merge.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0F, 1.4F);
                    }
                    if (t > 40 && t < 46) {
                        placeOrb(purple, merge, purpleSize);
                        Ink.ring(w, merge, 0.8 + (t - 40) * 0.25, Ink.DEEP_PURPLE, 0.5F);
                    }
                    if (t >= 46) {
                        fired = true;
                        dir = look;
                        origin = merge;
                        Ink.impact(w, origin, dir, Ink.PURPLE);
                        Ink.ring(w, origin, full ? 3.0 : 1.2, Ink.DEEP_PURPLE, 0.9F);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 4.0F, 0.5F);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 3.0F, 0.5F);
                        p.swingHand(Hand.MAIN_HAND, true);
                    }
                    return false;
                }

                double to = Math.min(purpleRange, dist + purpleSpeed);
                for (double d = dist; d < to; d += 2.0) {
                    Vec3d c = origin.add(dir.multiply(d));
                    if (SukunaMod.config.blockDamage) {
                        int r = purpleCut;
                        for (int dx = -r; dx <= r; dx++) {
                            for (int dy = -r; dy <= r; dy++) {
                                for (int dz = -r; dz <= r; dz++) {
                                    if (dx * dx + dy * dy + dz * dz <= r * r) {
                                        Cut.removeSafe(w, BlockPos.ofFloored(c.x + dx, c.y + dy, c.z + dz), p);
                                    }
                                }
                            }
                        }
                    }
                }
                Vec3d a = origin.add(dir.multiply(dist));
                Vec3d b = origin.add(dir.multiply(to));
                placeOrb(purple, b, purpleSize);
                Ink.stroke(w, a, b, Ink.PURPLE, Ink.DEEP_PURPLE, full ? 2.6F : 0.7F, false);
                Ink.splatter(w, b, full ? 6.0 : 1.5, full ? 10 : 3, Ink.PURPLE);
                if (full) {
                    Ink.ring(w, b, 4.5, Ink.PURPLE, 0.4F);
                }

                Box box = new Box(Math.min(a.x, b.x) - 10, Math.min(a.y, b.y) - 10, Math.min(a.z, b.z) - 10,
                    Math.max(a.x, b.x) + 10, Math.max(a.y, b.y) + 10, Math.max(a.z, b.z) + 10);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x) && !hit.contains(x.getUuid()))) {
                    Vec3d rel = new Vec3d(e.getX() - origin.x, e.getY() + e.getHeight() * 0.5 - origin.y, e.getZ() - origin.z);
                    double along = rel.dotProduct(dir);
                    double perp = rel.subtract(dir.multiply(along)).length();
                    if (along < dist - 2.0 || along > to + 2.0 || perp > purpleHit) continue;
                    hit.add(e.getUuid());
                    Dmg.hit(p, e, purpleDamage);
                    if (!selfHurt && e.squaredDistanceTo(p) < 16.0) {
                        selfHurt = true;
                        p.damage(w, w.getDamageSources().magic(), (float) (purpleDamage * 0.3));
                        p.sendMessage(Text.translatable("sukuna.msg.purple_recoil"), true);
                    }
                    e.setVelocity(dir.multiply(2.0).add(0.0, 0.4, 0.0));
                    e.velocityDirty = true;
                    Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), dir, Ink.PURPLE);
                }
                Fx.sound(w, b.x, b.y, b.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.MASTER, 2.5F, 0.5F);
                dist = to;
                if (dist >= purpleRange) {
                    Ink.impact(w, b, dir.multiply(-1.0), Ink.PURPLE);
                    dropOrb(purple);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    private static final int RUN_TICKS = 300;
    private static final double RUN_TOUCH_DAMAGE = 25.0;

    private static void startRun(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        s.runUntil = now + RUN_TICKS;
        s.busyUntil = Math.max(s.busyUntil, now + 20);
        Players.broadcastNamedAnim(p, 0);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_FLAP, SoundCategory.PLAYERS, 2.0F, 1.6F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2F, 1.8F);
        p.sendMessage(Text.translatable("sukuna.msg.gojo_run"), true);

        Tasks.add(new Task() {
            private final Map<UUID, Long> cooldown = new HashMap<>();
            private Vec3d last = new Vec3d(p.getX(), p.getY(), p.getZ());
            private int t = 0;

            @Override
            public boolean tick() {
                long time = w.getTime();
                if (!p.isAlive() || p.isRemoved() || time >= s.runUntil) {
                    s.runUntil = 0L;
                    return true;
                }
                t++;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 6, 9, false, false));
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 6, 2, false, false));
                Vec3d now3 = new Vec3d(p.getX(), p.getY(), p.getZ());
                if (t % 2 == 0 && now3.squaredDistanceTo(last) > 0.5) {
                    Ink.stroke(w, last.add(0.0, 1.0, 0.0), now3.add(0.0, 1.0, 0.0), Ink.WHITE, Ink.BLUE, 0.4F, true);
                    last = now3;
                }
                if (t % 5 == 0) {
                    Fx.sound(w, now3.x, now3.y, now3.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.9F, 1.5F);
                }
                Box box = p.getBoundingBox().expand(0.4);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                    Long until = cooldown.get(e.getUuid());
                    if (until != null && until > time) continue;
                    cooldown.put(e.getUuid(), time + 8L);
                    Dmg.hit(p, e, RUN_TOUCH_DAMAGE);
                    Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ()), p.getRotationVec(1.0F), Ink.BLUE);
                    Fx.sound(w, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.2F, 1.2F);
                    if (!e.isAlive() && e instanceof ServerPlayerEntity victim) {
                        s.headVictim = victim.getUuid().toString();
                        s.headUntil = time + 1200L;
                        Players.broadcastNamedAnim(p, 1);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WITHER_BREAK_BLOCK, SoundCategory.PLAYERS, 1.2F, 0.7F);
                    }
                }
                return false;
            }
        });
    }

    public static boolean castInstantDomain(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d c = new Vec3d(p.getX(), p.getY() + 1.0, p.getZ());
        double radius = 12.0;
        s.busyUntil = w.getTime() + 10;
        s.infinityOffUntil = w.getTime() + 12 + RUN_TICKS + 20;
        p.sendMessage(Text.translatable("sukuna.msg.gojo_domain_instant"), true);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 1.5F, 1.8F);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                t++;
                stripes(t);
                if (t == 4) {
                    List<LivingEntity> victims = new ArrayList<>();
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), x -> Aim.valid(p, x))) {
                        if (e.squaredDistanceTo(c.x, c.y, c.z) <= radius * radius) {
                            victims.add(e);
                        }
                    }
                    Tasks.add(new FreezeTask(w, victims, 200));
                }
                if (t >= 12) {
                    startRun(p, s, w);
                    return true;
                }
                return false;
            }

            private void stripes(int tick) {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                boolean flip = tick % 2 == 0;
                double reach = 3.0 + tick * 1.1;
                int count = 36;
                for (int i = 0; i < count; i++) {
                    double a = (i + r.nextDouble() * 0.6) * Math.PI * 2.0 / count;
                    double rr = Math.min(radius, reach) * (0.55 + r.nextDouble() * 0.45);
                    Vec3d base = new Vec3d(c.x + Math.cos(a) * rr, c.y - 2.0, c.z + Math.sin(a) * rr);
                    double len = 4.0 + r.nextDouble() * 9.0;
                    int core = (i % 2 == 0) == flip ? Ink.WHITE : Ink.BLACK;
                    int edge = core == Ink.WHITE ? Ink.BLUE : Ink.WHITE;
                    Ink.stroke(w, base, base.add(0.0, len, 0.0), core, edge, 0.5F, false);
                }
                for (int i = 0; i < 6; i++) {
                    double y = c.y - 1.5 + i * 1.6 + r.nextDouble() * 0.5;
                    Ink.ring(w, new Vec3d(c.x, y, c.z), Math.min(radius, reach), (i % 2 == 0) == flip ? Ink.WHITE : Ink.BLACK, 0.35F);
                }
                Ink.splatter(w, c, Math.min(radius, reach), 8, flip ? Ink.WHITE : Ink.BLUE);
                if (tick == 1 || tick == 6) {
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 2.0F, tick == 1 ? 1.9F : 1.4F);
                }
            }
        });
        return true;
    }

    public static void transform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.form == SukunaForm.GOJO) {
            if (!Players.formReady(p, s)) {
                return;
            }
            awaken(p, s, w, SukunaState.GOJO_AWAKEN_TICKS);
            s.awakenedViaMeter = false;
        } else {
            Players.endAwakening(p, s, now);
        }
    }

    public static void awaken(ServerPlayerEntity p, SukunaState s, ServerWorld w, int ticks) {
        long now = w.getTime();
        s.form = SukunaForm.GOJO_AWAKENED;
        s.formUntil = now + ticks;
        Players.applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.gojo_awakened"), false);
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 2.0, Ink.BLUE, 0.9F);
        Ink.ring(w, base.add(0, 1.0, 0), 3.0, Ink.PURPLE, 0.9F);
        Ink.splatter(w, base.add(0, 1.2, 0), 5.0, 24, Ink.WHITE);
        Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0F, 1.6F);
    }

    public static void hold(LivingEntity e, Vec3d anchor) {
        e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 9, false, false));
        Vec3d v = e.getVelocity();
        e.setVelocity(0.0, Math.min(0.0, v.y), 0.0);
        e.velocityDirty = true;
        if (e instanceof MobEntity m) {
            m.setAiDisabled(true);
        } else if (e instanceof ServerPlayerEntity sp) {
            double dx = sp.getX() - anchor.x;
            double dz = sp.getZ() - anchor.z;
            if (dx * dx + dz * dz > 0.04) {
                sp.networkHandler.requestTeleport(anchor.x, sp.getY(), anchor.z, sp.getYaw(), sp.getPitch());
            }
        }
    }

    public static void release(LivingEntity e) {
        if (e instanceof MobEntity m) {
            m.setAiDisabled(false);
        }
    }

    public static final class FreezeTask implements Task {
        private final ServerWorld w;
        private final Map<LivingEntity, Vec3d> anchors = new HashMap<>();
        private int left;

        public FreezeTask(ServerWorld w, List<LivingEntity> victims, int ticks) {
            this.w = w;
            this.left = ticks;
            for (LivingEntity e : victims) {
                anchors.put(e, new Vec3d(e.getX(), e.getY(), e.getZ()));
            }
        }

        @Override
        public boolean tick() {
            left--;
            ThreadLocalRandom r = ThreadLocalRandom.current();
            for (Map.Entry<LivingEntity, Vec3d> en : anchors.entrySet()) {
                LivingEntity e = en.getKey();
                if (!e.isAlive()) continue;
                hold(e, en.getValue());
                if (left % 10 == 0) {
                    Ink.dot(w, new Vec3d(e.getX() + r.nextDouble() - 0.5, e.getY() + e.getHeight() * 0.6, e.getZ() + r.nextDouble() - 0.5), Ink.WHITE, 0.9F);
                }
            }
            if (left <= 0) {
                for (LivingEntity e : anchors.keySet()) {
                    release(e);
                }
                return true;
            }
            return false;
        }
    }
}
