package com.dowerp.sukuna;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public final class Wheel {
    private Wheel() {
    }

    public static void draw(ServerWorld w, Vec3d c, float yawDeg, float angleDeg, double radius) {
        double yaw = Math.toRadians(yawDeg);
        Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3d up = new Vec3d(0.0, 1.0, 0.0);
        for (int i = 0; i < 18; i++) {
            double a = i * Math.PI * 2.0 / 18.0;
            Vec3d p = c.add(right.multiply(Math.cos(a) * radius)).add(up.multiply(Math.sin(a) * radius));
            w.spawnParticles(Fx.GOLD, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(angleDeg + k * 45.0);
            Vec3d d = right.multiply(Math.cos(a)).add(up.multiply(Math.sin(a)));
            for (int j = 1; j <= 3; j++) {
                Vec3d p = c.add(d.multiply(radius * j / 3.0));
                w.spawnParticles(Fx.PALE, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            Vec3d tip = c.add(d.multiply(radius * 1.28));
            w.spawnParticles(Fx.GOLD, tip.x, tip.y, tip.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
