package com.dowerp.sukuna;

import com.dowerp.sukuna.entity.MahoragaEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Optional;

public final class Aim {
    private Aim() {
    }

    public static boolean valid(ServerPlayerEntity owner, LivingEntity e) {
        if (e == owner || !e.isAlive() || e.isSpectator()) {
            return false;
        }
        if (e instanceof MahoragaEntity m && m.ownedBy(owner.getUuid())) {
            return false;
        }
        if (e instanceof TameableEntity t && t.isOwner(owner)) {
            return false;
        }
        return !(e instanceof PlayerEntity) || SukunaMod.config.damagePlayers;
    }

    public static LivingEntity target(ServerPlayerEntity p, double range) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d eye = p.getEyePos();
        Vec3d end = eye.add(look.multiply(range));
        Box area = p.getBoundingBox().stretch(look.multiply(range)).expand(1.5);
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, area, x -> valid(p, x))) {
            Optional<Vec3d> hit = e.getBoundingBox().expand(0.4).raycast(eye, end);
            if (hit.isPresent()) {
                double d = eye.squaredDistanceTo(hit.get());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = e;
                }
            }
        }
        return best;
    }
}
