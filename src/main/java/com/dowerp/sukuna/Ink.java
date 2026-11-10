package com.dowerp.sukuna;

import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.entity.SpriteFxEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public final class Ink {
    public static final int BLACK = 0x07030A;
    public static final int WHITE = 0xFAFAFF;
    public static final int RED = 0xC7001E;
    public static final int BLOOD = 0xB0101A;
    public static final int GOLD = 0xF2C14E;
    public static final int BLUE = 0x1B82FF;
    public static final int PURPLE = 0x9B30FF;
    public static final int DEEP_PURPLE = 0x2A0A5E;
    public static final int FIRE = 0xFF7A1A;
    public static final int FIRE_CORE = 0xFFE27A;

    private static final Map<Integer, DustParticleEffect> CACHE = new ConcurrentHashMap<>();

    private Ink() {
    }

    private static DustParticleEffect dust(int color, float size) {
        int q = Math.max(1, Math.min(40, Math.round(size * 10.0F)));
        int rgb = color & 0xFFFFFF;
        return CACHE.computeIfAbsent(rgb * 64 + q, k -> new DustParticleEffect(rgb, q / 10.0F));
    }

    private static final int MAX_DOTS_PER_TICK = 450;
    private static int budget = MAX_DOTS_PER_TICK;


    private static final int MAX_SPRITES_PER_TICK = 60;
    private static int spriteBudget = MAX_SPRITES_PER_TICK;

    private static boolean neutral(int c) {
        return c == WHITE || c == BLACK;
    }

    static int palette(int core, int outline) {
        int c = neutral(core) ? outline : core;
        if (neutral(c)) return 0;
        if (c == RED || c == BLOOD) return 1;
        if (c == BLUE) return 2;
        if (c == PURPLE || c == DEEP_PURPLE) return 3;
        if (c == GOLD || c == FIRE || c == FIRE_CORE) return 4;
        int r = (c >> 16) & 255;
        int g = (c >> 8) & 255;
        int b = c & 255;
        if (b > r && b > g) return b > 200 && r > 120 ? 3 : 2;
        if (r > g && r > b) return g > 120 ? 4 : 1;
        return 0;
    }

    static void sprite(ServerWorld w, Vec3d center, Vec3d dir, int kind, int pal, double length, double width, double roll, int life) {
        if (spriteBudget <= 0) {
            return;
        }
        spriteBudget--;
        SpriteFxEntity e = ModEntities.SPRITE.create(w, SpawnReason.TRIGGERED);
        if (e == null) {
            return;
        }
        Vec3d d = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : dir.normalize();
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, d.y))));
        e.refreshPositionAndAngles(center.x, center.y, center.z, yaw, pitch);
        e.configure(kind, pal, length, width, Math.toDegrees(roll), life);
        w.spawnEntity(e);
    }

    public static void fireArrow(ServerWorld w, Vec3d center, Vec3d dir, double length, double width, int life) {
        sprite(w, center, dir, SpriteFxEntity.ARROW, 4, length, width, 0.0, life);
    }

    public static void bloodBlob(ServerWorld w, Vec3d pos, double size) {
        sprite(w, pos, new Vec3d(0, 0, 1), SpriteFxEntity.BLOB, 1, size * 0.32, size * 0.32, ThreadLocalRandom.current().nextDouble() * 6.28, 9);
    }

    public static void bloodSplat(ServerWorld w, Vec3d pos, Vec3d normal, double size) {
        Vec3d n = normal.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : normal;
        sprite(w, pos, n, SpriteFxEntity.SPLASH, 1, size * 0.3, size * 0.3, ThreadLocalRandom.current().nextDouble() * 6.28, 6);
    }

    public static void bloodStream(ServerWorld w, Vec3d a, Vec3d b, double width) {
        Vec3d ab = b.subtract(a);
        double len = ab.length();
        if (len < 0.05) {
            return;
        }
        int parts = Math.max(1, (int) Math.ceil(len / 16.0));
        double seg = len / parts;
        for (int i = 0; i < parts; i++) {
            Vec3d mid = a.add(ab.multiply((i + 0.5) / parts));
            sprite(w, mid, ab, SpriteFxEntity.TORRENT, 1, seg * 1.04, width * 0.38, ThreadLocalRandom.current().nextDouble() * 6.28, 4);
        }
    }

    public static void bloodSpray(ServerWorld w, Vec3d c, double spread, int count) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        int n = Math.max(1, count / 6);
        for (int i = 0; i < n; i++) {
            Vec3d p = c.add((r.nextDouble() - 0.5) * 2 * spread, (r.nextDouble() - 0.3) * spread, (r.nextDouble() - 0.5) * 2 * spread);
            bloodBlob(w, p, 0.5 + r.nextDouble() * 0.9);
        }
        bloodSplat(w, c.add(0.0, 0.05, 0.0), new Vec3d(0, 1, 0), spread * 1.4);
    }

    public static void distort(ServerWorld w, Vec3d pos, double size) {
        sprite(w, pos, new Vec3d(ThreadLocalRandom.current().nextDouble() - 0.5, ThreadLocalRandom.current().nextDouble() - 0.5, 1.0), SpriteFxEntity.DISTORT, 0, size, size, 0.0, 16);
    }

    public static void resetBudget() {
        budget = MAX_DOTS_PER_TICK;
        spriteBudget = MAX_SPRITES_PER_TICK;
    }

    public static void dot(ServerWorld w, Vec3d p, int color, float size) {
        if (budget <= 0) {
            return;
        }
        budget--;
        w.spawnParticles(dust(color, size), p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    public static void stroke(ServerWorld w, Vec3d a, Vec3d b, int core, int outline, float width, boolean taper) {
        Vec3d ab = b.subtract(a);
        double len = ab.length();
        if (len < 0.05) {
            return;
        }
        int pal = palette(core, outline);
        int parts = Math.max(1, (int) Math.ceil(len / 16.0));
        double seg = len / parts;
        for (int i = 0; i < parts; i++) {
            Vec3d mid = a.add(ab.multiply((i + 0.5) / parts));
            sprite(w, mid, ab, SpriteFxEntity.STREAK, pal, seg * 1.04, 0.35 + width * 0.9, 0.0, 7);
        }
    }

    public static void polyline(ServerWorld w, List<Vec3d> pts, int core, int outline, float width) {
        int n = pts.size();
        for (int i = 0; i < n; i++) {
            double t = n <= 1 ? 0.5 : i / (double) (n - 1);
            float k = (float) Math.sin(Math.PI * t);
            float size = Math.max(0.25F, width * (0.3F + 0.7F * k));
            Vec3d p = pts.get(i);
            if ((i & 1) == 0) {
                dot(w, p, outline, size + 0.45F);
            }
            dot(w, p, core, size);
        }
    }

    public static void slashLite(ServerWorld w, Vec3d c, Vec3d dir, double roll, double radius, int core) {
        sprite(w, c, dir, SpriteFxEntity.CRESCENT, palette(core, BLACK), radius * 2.0, radius * 2.0, roll, 9);
    }

    public static void slash(ServerWorld w, Vec3d c, Vec3d dir, double roll, double radius, int core) {
        int pal = palette(core, BLACK);
        sprite(w, c, dir, SpriteFxEntity.CRESCENT, pal, radius * 2.3, radius * 2.3, roll, 10);
        sprite(w, c.add(dir.normalize().multiply(0.15)), dir, SpriteFxEntity.CRESCENT, 0, radius * 1.7, radius * 1.7, roll + 0.12, 8);
    }

    private static List<Vec3d> arcs(ServerWorld w, Vec3d c, Vec3d dir, double roll, double radius, int core, int n) {
        Vec3d f = dir.normalize();
        Vec3d[] b = Fx.basis(f);
        Vec3d lat = b[0].multiply(Math.cos(roll)).add(b[1].multiply(Math.sin(roll)));
        List<Vec3d> outer = new ArrayList<>();
        List<Vec3d> inner = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double ang = (-1.0 + 2.0 * i / n) * 1.15;
            Vec3d base = c.add(lat.multiply(Math.sin(ang) * radius)).add(f.multiply((Math.cos(ang) - 1.0) * radius * 0.9));
            outer.add(base);
            inner.add(base.subtract(f.multiply(0.22 * Math.cos(ang * 0.9))));
        }
        polyline(w, outer, WHITE, BLACK, 1.15F);
        polyline(w, inner, core, BLACK, 0.7F);
        return outer;
    }

    public static void cutMark(ServerWorld w, Vec3d c, Vec3d dir, double roll, double length, int core) {
        Vec3d f = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0, 0, 1) : dir.normalize();
        Vec3d[] b = Fx.basis(f);
        Vec3d lat = b[0].multiply(Math.cos(roll)).add(b[1].multiply(Math.sin(roll)));
        Vec3d side = f.crossProduct(lat).normalize();
        stroke(w, c.add(lat.multiply(-length)), c.add(lat.multiply(length)), WHITE, BLACK, 0.9F, true);
        stroke(w, c.add(lat.multiply(-length * 0.8)).add(side.multiply(0.18)), c.add(lat.multiply(length * 0.8)).add(side.multiply(0.18)), core, BLACK, 0.35F, true);
        stroke(w, c.add(lat.multiply(-length * 0.6)).add(side.multiply(-0.2)), c.add(lat.multiply(length * 0.6)).add(side.multiply(-0.2)), BLACK, core, 0.3F, true);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < 5; i++) {
            Vec3d p = c.add(lat.multiply((r.nextDouble() - 0.5) * 2.0 * length)).add(side.multiply((r.nextDouble() - 0.5) * 0.9));
            dot(w, p, i % 2 == 0 ? core : BLACK, 0.4F + r.nextFloat() * 0.5F);
        }
    }

    public static void impact(ServerWorld w, Vec3d pos, Vec3d dir, int core) {
        int pal = palette(core, BLACK);
        Vec3d d = dir.lengthSquared() < 1.0E-6 ? new Vec3d(0, 1, 0) : dir;
        sprite(w, pos, d, SpriteFxEntity.BURST, pal, 2.6, 2.6, ThreadLocalRandom.current().nextDouble() * 6.28, 7);
    }

    public static void ring(ServerWorld w, Vec3d c, double radius, int core, float width) {
        sprite(w, c, new Vec3d(0, 1, 0), SpriteFxEntity.RING, palette(core, BLACK), radius * 2.2, radius * 2.2, 0.0, 9);
    }

    public static void bolt(ServerWorld w, Vec3d a, Vec3d b, int core, int outline, float width) {
        Vec3d ab = b.subtract(a);
        double len = ab.length();
        if (len < 0.05) {
            return;
        }
        int pal = palette(core, outline);
        int parts = Math.max(1, (int) Math.ceil(len / 16.0));
        double seg = len / parts;
        for (int i = 0; i < parts; i++) {
            Vec3d mid = a.add(ab.multiply((i + 0.5) / parts));
            sprite(w, mid, ab, SpriteFxEntity.BOLT, pal, seg * 1.05, 0.7 + width * 1.2, ThreadLocalRandom.current().nextDouble() * 6.28, 6);
        }
    }

    public static void splatter(ServerWorld w, Vec3d c, double spread, int count, int color) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        for (int i = 0; i < count; i++) {
            Vec3d p = c.add((r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * spread, (r.nextDouble() - 0.5) * spread);
            dot(w, p, i % 3 == 0 ? color : BLACK, 0.4F + r.nextFloat() * 0.7F);
        }
    }
}
