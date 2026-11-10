package com.dowerp.sukuna;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;

public final class Fx {
    public static final DustParticleEffect BLOOD = new DustParticleEffect(0xB0101A, 1.1F);
    public static final DustParticleEffect DARK = new DustParticleEffect(0x20050A, 1.3F);
    public static final DustParticleEffect WHITE = new DustParticleEffect(0xF4F4F8, 0.9F);
    public static final DustParticleEffect GOLD = new DustParticleEffect(0xF2C14E, 0.7F);
    public static final DustParticleEffect PALE = new DustParticleEffect(0xFFF1B8, 0.5F);

    private Fx() {
    }

    public static ServerWorld world(ServerPlayerEntity p) {
        return (ServerWorld) p.getEntityWorld();
    }

    public static void puff(ServerWorld w, ParticleEffect e, double x, double y, double z, int n, double dx, double dy, double dz, double speed) {
        w.spawnParticles(e, x, y, z, n, dx, dy, dz, speed);
    }

    public static void far(ServerWorld w, ParticleEffect e, double x, double y, double z, int n, double dx, double dy, double dz, double speed) {
        w.spawnParticles(e, true, false, x, y, z, n, dx, dy, dz, speed);
    }

    public static void sound(ServerWorld w, double x, double y, double z, SoundEvent s, SoundCategory c, float volume, float pitch) {
        w.playSound(null, x, y, z, s, c, volume, pitch);
    }

    public static void sound(ServerWorld w, double x, double y, double z, RegistryEntry<SoundEvent> s, SoundCategory c, float volume, float pitch) {
        w.playSound(null, x, y, z, s, c, volume, pitch);
    }

    public static Vec3d[] basis(Vec3d forward) {
        Vec3d f = forward.normalize();
        Vec3d helper = Math.abs(f.y) > 0.95 ? new Vec3d(1.0, 0.0, 0.0) : new Vec3d(0.0, 1.0, 0.0);
        Vec3d r = f.crossProduct(helper).normalize();
        Vec3d u = r.crossProduct(f).normalize();
        return new Vec3d[] {r, u};
    }

    public static void crescent(ServerWorld w, Vec3d c, Vec3d dir, double roll, double radius, ParticleEffect e) {
        Ink.slash(w, c, dir, roll, radius, e == BLOOD ? Ink.BLOOD : e == GOLD ? Ink.GOLD : Ink.WHITE);
    }

    public static void line(ServerWorld w, Vec3d a, Vec3d b, ParticleEffect e, double step) {
        Vec3d d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) {
            return;
        }
        Vec3d n = d.multiply(1.0 / len);
        for (double t = 0.0; t <= len; t += step) {
            Vec3d p = a.add(n.multiply(t));
            w.spawnParticles(e, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }
}
