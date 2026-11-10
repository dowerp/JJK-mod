package com.dowerp.sukuna;

import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public final class Interact {
    private Interact() {
    }

    public static int cutProjectiles(ServerWorld w, ServerPlayerEntity owner, Vec3d a, Vec3d b, double radius) {
        int n = 0;
        Box box = new Box(a, b).expand(radius);
        Vec3d ab = b.subtract(a);
        double len2 = Math.max(1.0E-6, ab.lengthSquared());
        for (ProjectileEntity pr : w.getEntitiesByClass(ProjectileEntity.class, box, e -> e.getOwner() != owner && e.isAlive())) {
            Vec3d pp = new Vec3d(pr.getX(), pr.getY(), pr.getZ());
            double t = Math.max(0.0, Math.min(1.0, pp.subtract(a).dotProduct(ab) / len2));
            if (pp.distanceTo(a.add(ab.multiply(t))) > radius) {
                continue;
            }
            Ink.impact(w, pp, ab, Ink.WHITE);
            Fx.sound(w, pp.x, pp.y, pp.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 0.8F, 1.5F);
            pr.discard();
            n++;
        }
        return n;
    }
}
