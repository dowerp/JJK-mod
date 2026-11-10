package com.dowerp.sukuna;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
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
import java.util.concurrent.ThreadLocalRandom;

public final class DismantleTask implements Task {
    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private Vec3d pos;
    private final Vec3d dir;
    private final double roll;
    private final double damage;
    private final int fingers;
    private double left;
    private int delay;
    private int trail;
    private final Set<UUID> hit = new HashSet<>();

    private DismantleTask(ServerPlayerEntity owner, Vec3d start, Vec3d dir, double roll, double damage, double range, int fingers, int delay) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.pos = start;
        this.dir = dir;
        this.roll = roll;
        this.damage = damage;
        this.left = range;
        this.fingers = fingers;
        this.delay = delay;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d start = p.getEyePos().add(look.multiply(0.8));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double range = 30.0 + s.statFingers();
        double damage = 6.0 * s.power();
        for (int i = 0; i < 3; i++) {
            Vec3d d = look.add((r.nextDouble() - 0.5) * 0.06, (r.nextDouble() - 0.5) * 0.06, (r.nextDouble() - 0.5) * 0.06).normalize();
            Tasks.add(new DismantleTask(p, start, d, r.nextDouble() * Math.PI, damage, range, s.statFingers(), i * 2));
        }
        p.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    @Override
    public boolean tick() {
        if (delay > 0) {
            delay--;
            return false;
        }
        if (owner.isRemoved()) {
            return true;
        }
        Vec3d from = pos;
        int sub = 6;
        double step = 3.0 / sub;
        for (int i = 0; i < sub; i++) {
            Vec3d next = pos.add(dir.multiply(step));
            BlockPos bp = BlockPos.ofFloored(next.x, next.y, next.z);
            if (!w.isPosLoaded(bp)) {
                return true;
            }
            BlockState st = w.getBlockState(bp);
            if (!st.getCollisionShape(w, bp).isEmpty()) {
                hurt(from, pos);
                gash(next);
                return true;
            }
            pos = next;
            left -= step;
            if (left <= 0.0) {
                hurt(from, pos);
                return true;
            }
        }
        hurt(from, pos);
        Interact.cutProjectiles(w, owner, from, pos, 2.2);
        if ((trail++ & 1) == 0) {
            Ink.slashLite(w, pos, dir, roll, 1.9, Ink.BLOOD);
        }
        if (trail % 4 == 0) {
            Ink.cutMark(w, pos, dir, roll, 2.0, Ink.BLOOD);
        }
        return false;
    }

    private void hurt(Vec3d a, Vec3d b) {
        Box box = new Box(a, b).expand(1.7);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(owner, x) && !hit.contains(x.getUuid()))) {
            hit.add(e.getUuid());
            Dmg.hit(owner, e, damage);
            Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), dir, Ink.BLOOD);
        }
    }

    private void gash(Vec3d at) {
        Ink.impact(w, at, dir.multiply(-1.0), Ink.WHITE);
        Ink.splatter(w, at, 1.6, 8, Ink.BLOOD);
        Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 0.6F);
        if (!SukunaMod.config.blockDamage) {
            return;
        }
        Vec3d[] b = Fx.basis(dir);
        Vec3d lat = b[0].multiply(Math.cos(roll)).add(b[1].multiply(Math.sin(roll)));
        double len = 4.0 + fingers * 0.1;
        for (double a = -len; a <= len; a += 0.5) {
            for (double d = -0.5; d <= 3.0; d += 0.5) {
                Vec3d p = at.add(lat.multiply(a)).add(dir.multiply(d));
                Cut.remove(w, BlockPos.ofFloored(p.x, p.y, p.z), null);
            }
        }
    }
}
