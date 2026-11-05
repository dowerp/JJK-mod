package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class WebTask implements Task {
    private static final int DURATION = 18;

    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final Vec3d origin;
    private final double radius;
    private final double damage;
    private int t;
    private final Set<UUID> hit = new HashSet<>();

    private WebTask(ServerPlayerEntity owner, Vec3d origin, double radius, double damage) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.origin = origin;
        this.radius = radius;
        this.damage = damage;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s) {
        double radius = Math.min(22.0, 10.0 + s.statFingers() * 0.6);
        Vec3d origin = new Vec3d(p.getX(), p.getY(), p.getZ());
        Tasks.add(new WebTask(p, origin, radius, 12.0 * s.power()));
        s.busyUntil = Fx.world(p).getTime() + DURATION;
        p.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(Fx.world(p), origin.x, origin.y, origin.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.6F, 1.2F);
        return true;
    }

    @Override
    public boolean tick() {
        double r0 = radius * t / DURATION;
        double r1 = radius * (t + 1) / DURATION;
        int spokes = 12;
        for (int k = 0; k < spokes; k++) {
            double a = k * Math.PI * 2.0 / spokes + 0.2;
            for (double d = r0; d < r1; d += 0.5) {
                carve(origin.x + Math.cos(a) * d, origin.z + Math.sin(a) * d);
            }
        }
        for (int ring = 1; ring <= 4; ring++) {
            double rr = radius * ring / 4.0;
            if (rr >= r0 && rr < r1) {
                for (double a = 0.0; a < Math.PI * 2.0; a += 0.5 / rr) {
                    carve(origin.x + Math.cos(a) * rr, origin.z + Math.sin(a) * rr);
                }
            }
        }
        strike(r1);
        if (t % 3 == 0) {
            owner.swingHand(Hand.MAIN_HAND, true);
            Fx.sound(w, origin.x, origin.y, origin.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 0.6F + t * 0.04F);
        }
        t++;
        return t >= DURATION;
    }

    private void carve(double x, double z) {
        int bx = MathHelper.floor(x);
        int bz = MathHelper.floor(z);
        int y = Cut.surfaceY(w, bx, bz, MathHelper.floor(origin.y));
        if (y == Integer.MIN_VALUE) {
            return;
        }
        Cut.removeSafe(w, new BlockPos(bx, y, bz), owner);
        Cut.removeSafe(w, new BlockPos(bx, y - 1, bz), owner);
        Ink.dot(w, new Vec3d(x, y + 1.1, z), Ink.BLACK, 0.9F);
        Ink.dot(w, new Vec3d(x, y + 1.1, z), Ink.WHITE, 0.5F);
    }

    private void strike(double r) {
        Box box = new Box(origin.x - r, origin.y - 5.0, origin.z - r, origin.x + r, origin.y + 6.0, origin.z + r);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(owner, x) && !hit.contains(x.getUuid()))) {
            double dx = e.getX() - origin.x;
            double dz = e.getZ() - origin.z;
            if (dx * dx + dz * dz > r * r) {
                continue;
            }
            hit.add(e.getUuid());
            Dmg.hit(owner, e, damage);
            Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), new Vec3d(0, 1, 0), Ink.WHITE);
        }
    }
}
