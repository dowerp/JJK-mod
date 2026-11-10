package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public final class WorldSlashTask implements Task {
    private static final double RANGE = 64.0;
    private static final int SWEEP_TICKS = 16;
    private static final double STEP = RANGE / SWEEP_TICKS;
    private static final double CROSS_HALF = 24.0;
    private static final double PLANE_HALF = 10.0;
    private static final double PLANE_START = 1.0;
    private static final double SAMPLE = 0.7;

    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final SukunaState s;
    private final boolean plane;
    private final double power;
    private final int total;
    private int charge;
    private boolean released;
    private int sweep;

    private Vec3d f = Vec3d.ZERO;
    private Vec3d right = Vec3d.ZERO;
    private Vec3d up = Vec3d.ZERO;
    private Vec3d origin = Vec3d.ZERO;
    private double along;

    private final Set<Long> seen = new HashSet<>();
    private final Set<UUID> hit = new HashSet<>();

    private WorldSlashTask(ServerPlayerEntity owner, SukunaState s, boolean plane) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.s = s;
        this.plane = plane;
        this.power = s.power();
        this.total = Math.max(10, SukunaMod.config.worldSlashChargeTicks);
        this.charge = total;
        this.along = plane ? PLANE_START : 1.0;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s, boolean plane) {
        s.charging = true;
        s.busyUntil = Fx.world(p).getTime() + SukunaMod.config.worldSlashChargeTicks + SWEEP_TICKS + 20;
        Tasks.add(new WorldSlashTask(p, s, plane));
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 3.0F, 0.6F);
        return true;
    }

    @Override
    public boolean tick() {
        if (owner.isRemoved() || !owner.isAlive()) {
            s.charging = false;
            return true;
        }
        if (!released) {
            aura();
            preview();
            if (charge % 10 == 0) {
                Fx.sound(w, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.HOSTILE, 2.0F, 0.6F + 0.8F * (float) (total - charge) / total);
            }
            charge--;
            if (charge <= 0) {
                release();
            }
            return false;
        }
        return sweep();
    }

    private void aura() {
        double prog = 1.0 - (double) charge / total;
        double px = owner.getX();
        double py = owner.getY();
        double pz = owner.getZ();
        for (int i = 0; i < 3; i++) {
            double a = (total - charge) * 0.5 + i * 2.094;
            double r = 1.7 - prog * 0.9;
            double h = prog * 2.3 + 0.2;
            Fx.far(w, Fx.BLOOD, px + Math.cos(a) * r, py + h, pz + Math.sin(a) * r, 1, 0.0, 0.0, 0.0, 0.0);
            Fx.far(w, Fx.DARK, px + Math.cos(a + 3.14) * r, py + h, pz + Math.sin(a + 3.14) * r, 1, 0.0, 0.0, 0.0, 0.0);
        }
        Ink.dot(w, new Vec3d(px, py + 1.0, pz), Ink.WHITE, 0.8F);
        if (charge % 4 == 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8, 3, false, false));
        }
        if (charge % 10 == 0) {
            owner.swingHand(Hand.MAIN_HAND, true);
        }
    }

    private void preview() {
        if (charge % 3 != 0) {
            return;
        }
        Vec3d look = owner.getRotationVec(1.0F);
        Vec3d[] b = Fx.basis(look);
        Vec3d c = owner.getEyePos().add(look.multiply(plane ? 6.0 : 10.0));
        double pulse = 0.35F + 0.25F * Math.sin(charge * 0.5);
        if (plane) {
            for (double k = -PLANE_HALF; k <= PLANE_HALF; k += 1.0) {
                Ink.dot(w, c.add(b[0].multiply(k)).add(b[1].multiply(PLANE_HALF)), Ink.WHITE, (float) pulse);
                Ink.dot(w, c.add(b[0].multiply(k)).add(b[1].multiply(-PLANE_HALF)), Ink.WHITE, (float) pulse);
                Ink.dot(w, c.add(b[0].multiply(PLANE_HALF)).add(b[1].multiply(k)), Ink.WHITE, (float) pulse);
                Ink.dot(w, c.add(b[0].multiply(-PLANE_HALF)).add(b[1].multiply(k)), Ink.WHITE, (float) pulse);
            }
        } else {
            for (double k = -CROSS_HALF; k <= CROSS_HALF; k += 1.5) {
                Ink.dot(w, c.add(b[0].multiply(k)), Ink.WHITE, (float) pulse);
                Ink.dot(w, c.add(b[1].multiply(k)), Ink.WHITE, (float) pulse);
            }
        }
        Ink.dot(w, c, Ink.BLOOD, 1.1F);
        Ink.stroke(w, owner.getEyePos().add(look.multiply(1.5)), c, Ink.BLOOD, Ink.BLACK, 0.3F, true);
    }

    private void release() {
        released = true;
        f = owner.getRotationVec(1.0F).normalize();
        Vec3d[] b = Fx.basis(f);
        right = b[0];
        up = b[1];
        origin = owner.getEyePos();
        owner.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(w, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 4.0F, 0.7F);
        Fx.sound(w, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.ITEM_TRIDENT_THUNDER, SoundCategory.MASTER, 3.0F, 0.6F);
    }

    private boolean sweep() {
        sweep++;
        double from = along;
        double to = Math.min(RANGE, along + STEP);
        double half = plane ? PLANE_HALF : CROSS_HALF;

        for (double d = from; d < to; d += SAMPLE) {
            for (double u = -half; u <= half; u += SAMPLE) {
                for (double v = -half; v <= half; v += SAMPLE) {
                    if (!plane && !(Math.abs(u) <= 1.0 || Math.abs(v) <= 1.0)) {
                        continue;
                    }
                    Vec3d q = origin.add(f.multiply(d)).add(right.multiply(u)).add(up.multiply(v));
                    BlockPos bp = BlockPos.ofFloored(q.x, q.y, q.z);
                    if (seen.add(bp.asLong()) && w.isPosLoaded(bp)) {
                        Cut.removeSafe(w, bp, owner);
                    }
                }
            }
        }

        damage(from, to, half);
        drawFront(to, half);

        if (sweep % 2 == 0) {
            Vec3d c = origin.add(f.multiply(to));
            Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 3.0F, 0.8F);
        }
        along = to;
        if (along >= RANGE) {
            s.charging = false;
            return true;
        }
        return false;
    }

    private void damage(double from, double to, double half) {
        Vec3d mid = origin.add(f.multiply((from + to) * 0.5));
        double reach = half + 8.0;
        Box box = new Box(mid.x - reach, mid.y - reach, mid.z - reach, mid.x + reach, mid.y + reach, mid.z + reach);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(owner, x) && !hit.contains(x.getUuid()))) {
            Vec3d rel = new Vec3d(e.getX() - origin.x, e.getY() + e.getHeight() * 0.5 - origin.y, e.getZ() - origin.z);
            double d = rel.dotProduct(f);
            double u = rel.dotProduct(right);
            double v = rel.dotProduct(up);
            if (d < from - 1.5 || d > to + 1.5) continue;
            double pad = e.getWidth() * 0.5;
            double padV = e.getHeight() * 0.5;
            boolean inside;
            if (plane) {
                inside = Math.abs(u) <= half + pad && Math.abs(v) <= half + padV;
            } else {
                inside = (Math.abs(v) <= 1.5 + padV && Math.abs(u) <= half + pad) || (Math.abs(u) <= 1.5 + pad && Math.abs(v) <= half + padV);
            }
            if (!inside) continue;
            hit.add(e.getUuid());
            double ratio = plane ? 0.6 : 0.4;
            double base = plane ? 60.0 : 30.0;
            Dmg.hit(owner, e, Math.max(base * power, e.getMaxHealth() * ratio));
            Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), f, Ink.BLOOD);
        }
    }

    private void drawFront(double to, double half) {
        Vec3d c = origin.add(f.multiply(to));
        if (plane) {
            Vec3d a = c.add(right.multiply(-half)).add(up.multiply(-half));
            Vec3d b = c.add(right.multiply(half)).add(up.multiply(-half));
            Vec3d d = c.add(right.multiply(half)).add(up.multiply(half));
            Vec3d e = c.add(right.multiply(-half)).add(up.multiply(half));
            Ink.stroke(w, a, b, Ink.WHITE, Ink.BLACK, 1.0F, false);
            Ink.stroke(w, b, d, Ink.WHITE, Ink.BLACK, 1.0F, false);
            Ink.stroke(w, d, e, Ink.WHITE, Ink.BLACK, 1.0F, false);
            Ink.stroke(w, e, a, Ink.WHITE, Ink.BLACK, 1.0F, false);
            if (sweep % 3 == 1) {
                Ink.stroke(w, a, d, Ink.BLOOD, Ink.BLACK, 0.8F, true);
                Ink.stroke(w, b, e, Ink.BLOOD, Ink.BLACK, 0.8F, true);
            }
        } else {
            Ink.stroke(w, c.add(right.multiply(-half)), c.add(right.multiply(half)), Ink.WHITE, Ink.BLACK, 1.4F, true);
            Ink.stroke(w, c.add(up.multiply(-half)), c.add(up.multiply(half)), Ink.WHITE, Ink.BLACK, 1.4F, true);
        }
        Ink.impact(w, c, f, Ink.BLOOD);
        Ink.splatter(w, c, half * 0.8, 6, Ink.BLOOD);
    }
}
