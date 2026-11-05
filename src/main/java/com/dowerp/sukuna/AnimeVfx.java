package com.dowerp.sukuna;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.concurrent.ThreadLocalRandom;

public final class AnimeVfx {
    public static final DustParticleEffect BLACK_FLASH_CORE = new DustParticleEffect(0x050103, 1.8F);
    public static final DustParticleEffect BLACK_FLASH_CRIMSON = new DustParticleEffect(0xC7001E, 1.4F);
    public static final DustParticleEffect CE_BLUE = new DustParticleEffect(0x1B82FF, 1.3F);

    private AnimeVfx() {}

    public static void animeSlash(ServerWorld w, Vec3d center, Vec3d dir, double radius, double roll) {
        Ink.slash(w, center, dir, roll, radius, Ink.WHITE);
        Ink.slash(w, center.add(dir.normalize().multiply(0.15)), dir, roll + 0.14, radius * 0.9, Ink.BLOOD);
        Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.4F, 1.5F);
    }

    public static void blackFlash(ServerWorld w, Vec3d pos) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        for (int i = 0; i < 9; i++) {
            Vec3d d = new Vec3d(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5).normalize();
            double len = 1.6 + rnd.nextDouble() * 1.8;
            Ink.bolt(w, pos.add(d.multiply(0.2)), pos.add(d.multiply(len)), Ink.BLACK, Ink.RED, 0.8F);
        }
        Ink.dot(w, pos, Ink.RED, 2.6F);
        Ink.dot(w, pos, Ink.BLACK, 1.9F);
        Ink.dot(w, pos, Ink.WHITE, 0.9F);
        Ink.ring(w, pos.add(0, -0.2, 0), 1.3, Ink.RED, 0.7F);
        Ink.splatter(w, pos, 3.0, 14, Ink.RED);
        Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.5F, 0.65F);
        Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 2.0F, 1.4F);
    }

    public static void ringShockwave(ServerWorld w, Vec3d c, double r, ParticleEffect p) {
        int color = p == CE_BLUE ? Ink.BLUE : p == Fx.BLOOD ? Ink.BLOOD : p == Fx.GOLD ? Ink.GOLD : Ink.WHITE;
        Ink.ring(w, c, r, color, 0.7F);
    }

    public static void playSukunaAwakening(ServerWorld w, ServerPlayerEntity p) {
        Vec3d pos = new Vec3d(p.getX(), p.getY(), p.getZ());
        ringShockwave(w, pos.add(0, 0.2, 0), 3.0, Fx.BLOOD);
        w.spawnParticles(ParticleTypes.ASH, pos.x, pos.y + 1.0, pos.z, 80, 0.8, 1.0, 0.8, 0.1);
        Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.PLAYERS, 1.2F, 0.8F);
    }

    public static void playHeianIncarnation(ServerWorld w, ServerPlayerEntity p) {
        Vec3d pos = new Vec3d(p.getX(), p.getY(), p.getZ());
        for (int r = 1; r <= 5; r++) {
            ringShockwave(w, pos.add(0, 0.3, 0), r, Fx.GOLD);
        }
        Ink.splatter(w, pos.add(0, 1.2, 0), 4.0, 30, Ink.GOLD);
        w.spawnParticles(ParticleTypes.SCULK_SOUL, pos.x, pos.y + 1.5, pos.z, 40, 1.5, 2.0, 1.5, 0.1);
        Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 3.0F, 0.5F);
        Fx.sound(w, pos.x, pos.y, pos.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 2.0F, 0.5F);
    }

    public static void playHeianReady(ServerWorld w, ServerPlayerEntity p) {
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 2.0F, 0.6F);
        w.spawnParticles(ParticleTypes.TRIAL_SPAWNER_DETECTION, p.getX(), p.getY() + 1.2, p.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
    }
}