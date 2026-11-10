package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ChosoTasks {
    public static final int RED_SCALE_TICKS = 500;
    public static final int STACK_TICKS = 300;
    public static final int HARDEN_TICKS = 400;

    private ChosoTasks() {
    }

    private static boolean solid(ServerWorld w, Vec3d p) {
        BlockPos bp = BlockPos.ofFloored(p.x, p.y, p.z);
        BlockState bs = w.getBlockState(bp);
        return !bs.isAir() && !bs.getCollisionShape(w, bp).isEmpty();
    }

    private static Vec3d hand(ServerPlayerEntity p) {
        return p.getEyePos().add(p.getRotationVec(1.0F).multiply(1.0)).add(0.0, -0.3, 0.0);
    }

    public static void tick(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        ThreadLocalRandom r = ThreadLocalRandom.current();
        convergenceTick(p, s, w, now);

        if (s.redScaleUntil > 0L) {
            boolean stack = s.redScaleStack;
            if (now >= s.redScaleUntil || !p.isAlive()) {
                endScale(p, s, false);
            } else {
                if (s.redScaleUntil > 0L && now % 10 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, stack ? 2 : 1, false, false));
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, stack ? 3 : 1, false, false));
                    if (stack) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 30, 1, false, false));
                    }
                }
                if (now % 6 == 0) {
                    Ink.dot(w, new Vec3d(p.getX() + r.nextDouble() - 0.5, p.getY() + r.nextDouble() * 1.8, p.getZ() + r.nextDouble() - 0.5), Ink.RED, 0.6F);
                }
            }
        }

        if (s.hardenUntil > 0L) {
            if (now >= s.hardenUntil || !p.isAlive()) {
                endHarden(p, s, false);
            } else {
                if (s.hardenUntil > 0L && now % 10 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 30, 1, false, false));
                }
            }
        }

    }

    private static void drawWings(ServerPlayerEntity p, ServerWorld w, long now) {
        double yaw = Math.toRadians(p.getYaw());
        Vec3d fwd = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3d base = new Vec3d(p.getX(), p.getY(), p.getZ()).subtract(fwd.multiply(0.3));
        double flap = Math.sin(now * 0.15) * 0.15;
        for (int k = -1; k <= 1; k += 2) {
            List<Vec3d> upper = new ArrayList<>();
            List<Vec3d> lower = new ArrayList<>();
            for (int i = 0; i <= 6; i++) {
                double lat = 0.3 + 0.28 * i;
                upper.add(base.add(right.multiply(k * lat)).add(0.0, 1.45 + 0.35 * i - 0.04 * i * i + flap * i * 0.3, 0.0).subtract(fwd.multiply(0.05 * i)));
                lower.add(base.add(right.multiply(k * lat * 0.85)).add(0.0, 1.25 + 0.12 * i + flap * i * 0.2, 0.0).subtract(fwd.multiply(0.04 * i)));
            }
            Ink.polyline(w, upper, Ink.BLOOD, Ink.BLACK, 0.55F);
            Ink.polyline(w, lower, Ink.RED, Ink.BLACK, 0.4F);
        }
        if (now % 8 == 0) {
            Ink.ring(w, new Vec3d(p.getX(), p.getY() + 2.35, p.getZ()), 0.45, Ink.RED, 0.35F);
        }
    }

    public static boolean toggleScale(ServerPlayerEntity p, SukunaState s, boolean stack) {
        ServerWorld w = Fx.world(p);
        long now = w.getTime();
        if (s.redScaleUntil > 0L) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.redScaleUntil = now + (stack ? STACK_TICKS : RED_SCALE_TICKS);
        s.redScaleStack = stack;
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 1.2, Ink.RED, 0.7F);
        Ink.ring(w, base.add(0, 1.0, 0), 1.0, Ink.BLOOD, 0.5F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 2.0F, stack ? 1.2F : 0.9F);
        p.sendMessage(Text.translatable(stack ? "sukuna.msg.choso_stack_on" : "sukuna.msg.choso_scale_on"), true);
        return true;
    }

    private static void endScale(ServerPlayerEntity p, SukunaState s, boolean noEnergy) {
        boolean wasStack = s.redScaleStack;
        s.redScaleUntil = 0L;
        s.redScaleStack = false;
        p.sendMessage(Text.translatable(noEnergy ? "sukuna.msg.choso_no_blood" : "sukuna.msg.choso_scale_off"), true);
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.6F);
        if (wasStack) {
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 160, 0, false, true));
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 0, false, true));
        }
    }

    public static boolean toggleHardening(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        if (s.hardenUntil > 0L) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.hardenUntil = w.getTime() + HARDEN_TICKS;
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 0.9, Ink.BLOOD, 0.8F);
        Ink.bloodSpray(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.6, 8);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ANVIL_PLACE, SoundCategory.PLAYERS, 0.8F, 1.6F);
        p.sendMessage(Text.translatable("sukuna.msg.choso_harden_on"), true);
        return true;
    }

    private static void endHarden(ServerPlayerEntity p, SukunaState s, boolean noEnergy) {
        s.hardenUntil = 0L;
        p.sendMessage(Text.translatable(noEnergy ? "sukuna.msg.choso_no_blood" : "sukuna.msg.choso_harden_off"), true);
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 1, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 0, false, true));
    }

    public static void stopAll(SukunaState s) {
        s.redScaleUntil = 0L;
        s.redScaleStack = false;
        s.hardenUntil = 0L;
        s.piercingCharging = false;
        s.convergenceUntil = 0L;
        s.convergenceCharges = 0;
        s.stillTicks = 0;
        s.convergenceMul = 1.0;
    }

    private static final int PIERCE_MIN = 8;
    private static final int PIERCE_MAX = 55;

    public static boolean castPiercing(ServerPlayerEntity p, SukunaState s, boolean wing) {
        ServerWorld w = Fx.world(p);
        double baseDamage = (wing ? 30.0 : 22.0) * s.power();
        double baseRange = wing ? 56.0 : 40.0;
        s.piercingCharging = true;
        s.busyUntil = w.getTime() + PIERCE_MIN + 4;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.5F, 0.6F);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) {
                    s.piercingCharging = false;
                    return true;
                }
                t++;
                boolean holding = s.held(Ability.PRIMARY_ATTACK);
                if (t < PIERCE_MIN || (holding && t < PIERCE_MAX)) {
                    s.busyUntil = w.getTime() + 4;
                    charge();
                    return false;
                }
                s.piercingCharging = false;
                fire((double) Math.min(t, PIERCE_MAX) / PIERCE_MAX);
                return true;
            }

            private void charge() {
                Vec3d c = hand(p);
                double k = (double) t / PIERCE_MAX;
                int orbs = 3 + (int) (k * 5);
                for (int i = 0; i < orbs; i++) {
                    double a = t * 0.9 + i * Math.PI * 2.0 / orbs;
                    double rr = 0.9 * (1.0 - (t % 10) / 12.0) + 0.15;
                    Vec3d o = c.add(Math.cos(a) * rr, Math.sin(a) * rr, 0.0);
                    Ink.bloodBlob(w, o, 0.95);
                }
                Ink.bloodBlob(w, c, ((float) (0.4 + k * 1.6)) * 0.8 + 0.3);
                if (t % 10 == 0) {
                    Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.0 + k * 1.5, Ink.BLOOD, 0.5F);
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8F, (float) (0.6 + k));
                }
                if (t % 4 == 0) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 1, false, false));
                }
            }

            private void fire(double k) {
                double damage = baseDamage * (1.0 + 2.5 * k);
                double range = baseRange * (1.0 + 0.5 * k);
                double width = 1.2 + 1.8 * k;
                boolean pierceBlocks = k >= 0.95;
                Vec3d dir = p.getRotationVec(1.0F);
                Vec3d origin = hand(p);
                double end = range;
                if (!pierceBlocks) {
                    for (double d = 0.0; d <= range; d += 0.5) {
                        if (solid(w, origin.add(dir.multiply(d)))) {
                            end = d;
                            break;
                        }
                    }
                }
                Vec3d to = origin.add(dir.multiply(end));
                Ink.bloodStream(w, origin, to, 0.5 + ((float) (0.6 + k * 1.6)) * 1.2);
                if (k > 0.45) {
                    Vec3d[] bs = Fx.basis(dir);
                    for (int sgn = -1; sgn <= 1; sgn += 2) {
                        Vec3d off = bs[0].multiply(sgn * (0.8 + k));
                        Ink.bloodStream(w, origin.add(off), to.add(off.multiply(0.4)), 0.5 + (0.5F) * 1.2);
                    }
                }
                Ink.bloodSplat(w, to, dir.multiply(-1.0), 1.8);
                if (pierceBlocks && SukunaMod.config.blockDamage) {
                    for (double d = 1.0; d <= end; d += 1.0) {
                        Vec3d c = origin.add(dir.multiply(d));
                        Cut.removeSafe(w, BlockPos.ofFloored(c.x, c.y, c.z), p);
                    }
                }

                Set<UUID> hit = new HashSet<>();
                double pad = width + 2.0;
                Box box = new Box(Math.min(origin.x, to.x) - pad, Math.min(origin.y, to.y) - pad, Math.min(origin.z, to.z) - pad,
                    Math.max(origin.x, to.x) + pad, Math.max(origin.y, to.y) + pad, Math.max(origin.z, to.z) + pad);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                    Vec3d rel = new Vec3d(e.getX() - origin.x, e.getY() + e.getHeight() * 0.5 - origin.y, e.getZ() - origin.z);
                    double along = rel.dotProduct(dir);
                    double perp = rel.subtract(dir.multiply(along)).length();
                    if (along < 0.0 || along > end + 1.0 || perp > width || !hit.add(e.getUuid())) continue;
                    Dmg.hit(p, e, damage);
                    e.setVelocity(dir.multiply(0.8 + k * 1.2).add(0.0, 0.2, 0.0));
                    e.velocityDirty = true;
                    Ink.bloodSplat(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), dir, 1.8);
                }
                p.swingHand(Hand.MAIN_HAND, true);
                Fx.sound(w, origin.x, origin.y, origin.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, (float) (1.0 + k), (float) (1.9 - k * 0.7));
            }
        });
        return true;
    }

    private interface Bullet {
        boolean step();
    }

    public static boolean castMeteorite(ServerPlayerEntity p, SukunaState s, boolean wing) {
        ServerWorld w = Fx.world(p);
        double damage = (wing ? 10.0 : 8.0) * s.power();
        s.busyUntil = w.getTime() + 10;

        Tasks.add(new Task() {
            private int t = 0;
            private final List<Bullet> bullets = new ArrayList<>();

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                if (t < 7 && t % 3 == 0) {
                    spawn();
                }
                t++;
                Iterator<Bullet> it = bullets.iterator();
                while (it.hasNext()) {
                    if (it.next().step()) it.remove();
                }
                return t >= 7 && bullets.isEmpty();
            }

            private void spawn() {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                Vec3d dir = p.getRotationVec(1.0F).add((r.nextDouble() - 0.5) * 0.05, (r.nextDouble() - 0.5) * 0.05, (r.nextDouble() - 0.5) * 0.05).normalize();
                Vec3d start = hand(p);
                p.swingHand(Hand.MAIN_HAND, true);
                Fx.sound(w, start.x, start.y, start.z, SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS, 1.0F, 1.7F);
                bullets.add(new Bullet() {
                    private Vec3d pos = start;
                    private int age = 0;

                    @Override
                    public boolean step() {
                        for (int sub = 0; sub < 2; sub++) {
                            pos = pos.add(dir.multiply(0.85));
                            Ink.bloodBlob(w, pos, 1.25);
                            List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(0.9), x -> Aim.valid(p, x));
                            if (!hit.isEmpty()) {
                                LivingEntity e = hit.get(0);
                                Dmg.hit(p, e, damage);
                                Ink.bloodSplat(w, pos, dir, 1.8);
                                return true;
                            }
                            if (solid(w, pos)) {
                                Ink.bloodSplat(w, pos, dir.multiply(-1.0), 1.8);
                                return true;
                            }
                        }
                        Ink.bloodStream(w, pos.subtract(dir.multiply(1.6)), pos, 0.5 + (0.35F) * 1.2);
                        return ++age > 30;
                    }
                });
            }
        });
        return true;
    }

    public static boolean castBloodEdge(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        double power = s.power();
        boolean hardened = s.hardenUntil > w.getTime();
        double damage = 7.0 * power * (hardened ? 1.3 : 1.0);
        s.busyUntil = w.getTime() + 10;

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                if (t % 3 == 0) {
                    Vec3d look = p.getRotationVec(1.0F);
                    Vec3d eye = p.getEyePos();
                    Vec3d center = eye.add(look.multiply(2.0));
                    int n = t / 3;
                    Ink.slash(w, center, look, n * 1.05, 2.0, Ink.BLOOD);
                    Ink.bloodStream(w, eye.add(look.multiply(0.6)).add(0, -0.3, 0), eye.add(look.multiply(2.4)).add(0, -0.3, 0), 0.5 + (0.6F) * 1.2);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(eye, eye).expand(3.8), x -> Aim.valid(p, x))) {
                        Vec3d to = new Vec3d(e.getX() - eye.x, e.getY() + e.getHeight() * 0.5 - eye.y, e.getZ() - eye.z);
                        if (to.lengthSquared() < 1.0E-4 || to.normalize().dotProduct(look) < 0.5) continue;
                        Dmg.hit(p, e, damage);
                        Ink.bloodSplat(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ()), look, 1.8);
                    }
                    p.swingHand(n % 2 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                    Fx.sound(w, eye.x, eye.y, eye.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.9F + n * 0.15F);
                }
                t++;
                return t > 7;
            }
        });
        return true;
    }

    public static boolean castCoagulation(ServerPlayerEntity p, SukunaState s, boolean wing) {
        ServerWorld w = Fx.world(p);
        p.heal((wing ? 24.0F : 16.0F) * (float) (s.form.targetHp / 60.0));
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 0.9, Ink.BLOOD, 0.6F);
        Ink.ring(w, base.add(0, 1.0, 0), 0.7, Ink.RED, 0.5F);
        Ink.ring(w, base.add(0, 1.9, 0), 0.5, Ink.BLOOD, 0.4F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_HONEY_BLOCK_PLACE, SoundCategory.PLAYERS, 1.2F, 0.7F);
        return true;
    }

    public static boolean castSupernova(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        double damage = 6.0 * s.power();
        s.busyUntil = w.getTime() + 34;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 2.0F, 1.4F);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                t++;
                Vec3d c = new Vec3d(p.getX(), p.getY() + 1.2, p.getZ());
                if (t < 16) {
                    for (int i = 0; i < 6; i++) {
                        double a = t * 0.5 + i * Math.PI / 3.0;
                        double rr = 1.8 - t * 0.07;
                        Vec3d o = c.add(Math.cos(a) * rr, Math.sin(a * 2.0) * 0.4, Math.sin(a) * rr);
                        Ink.bloodBlob(w, o, 1.25);
                    }
                    return false;
                }
                burst(c);
                return true;
            }

            private void burst(Vec3d c) {
                int pellets = 36;
                int[] hitsPerEntity = new int[1];
                java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
                for (int i = 0; i < pellets; i++) {
                    double y = 1.0 - 2.0 * (i + 0.5) / pellets;
                    double rr = Math.sqrt(1.0 - y * y);
                    double phi = i * 2.399963;
                    Vec3d dir = new Vec3d(Math.cos(phi) * rr, y, Math.sin(phi) * rr);
                    double end = 16.0;
                    for (double d = 0.5; d <= 16.0; d += 0.5) {
                        Vec3d pt = c.add(dir.multiply(d));
                        if (solid(w, pt)) {
                            end = d;
                            break;
                        }
                        List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, new Box(pt, pt).expand(0.6), x -> Aim.valid(p, x));
                        if (!hit.isEmpty()) {
                            LivingEntity e = hit.get(0);
                            int n = counts.merge(e.getUuid(), 1, Integer::sum);
                            if (n <= 8) {
                                Dmg.hit(p, e, damage);
                            }
                            end = d;
                            break;
                        }
                    }
                    Ink.bloodStream(w, c.add(dir.multiply(0.8)), c.add(dir.multiply(Math.min(end, 6.0))), 0.5 + (0.45F) * 1.2);
                }
                Ink.ring(w, c, 2.0, Ink.RED, 0.9F);
                Ink.bloodSpray(w, c, 3.0, 12);
                p.swingHand(Hand.MAIN_HAND, true);
                Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0F, 1.2F);
            }
        });
        return true;
    }

    public static boolean castSlicingExorcism(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d dir = p.getRotationVec(1.0F);
        double damage = 18.0 * s.power();
        s.busyUntil = w.getTime() + 8;
        p.swingHand(Hand.MAIN_HAND, true);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.6F, 0.6F);

        Tasks.add(new Task() {
            private Vec3d pos = p.getEyePos().add(dir.multiply(1.5));
            private int t = 0;
            private final Set<UUID> hit = new HashSet<>();

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                Vec3d prev = pos;
                pos = pos.add(dir.multiply(1.5));
                t++;
                Vec3d[] b = Fx.basis(dir);
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0 + t * 0.9;
                    Vec3d o = pos.add(b[0].multiply(Math.cos(a) * 1.4)).add(b[1].multiply(Math.sin(a) * 1.4));
                    Ink.bloodBlob(w, o, 0.95);
                }
                Ink.bloodStream(w, prev, pos, 0.5 + (0.5F) * 1.2);
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(1.8), x -> Aim.valid(p, x))) {
                    if (hit.add(e.getUuid())) {
                        Dmg.hit(p, e, damage);
                        Ink.bloodSplat(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), dir, 1.8);
                    }
                }
                if (solid(w, pos)) {
                    Ink.bloodSplat(w, pos, dir.multiply(-1.0), 1.8);
                    return true;
                }
                return t >= 26;
            }
        });
        return true;
    }

    public static boolean castWingBarrage(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        double damage = 7.0 * s.power();
        s.busyUntil = w.getTime() + 18;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.0F, 0.6F);

        Tasks.add(new Task() {
            private int t = 0;
            private final List<Bullet> shots = new ArrayList<>();

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                if (t < 16 && t % 2 == 0) {
                    spawn(t / 2);
                }
                t++;
                Iterator<Bullet> it = shots.iterator();
                while (it.hasNext()) {
                    if (it.next().step()) it.remove();
                }
                return t >= 16 && shots.isEmpty();
            }

            private void spawn(int index) {
                double yaw = Math.toRadians(p.getYaw());
                Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
                Vec3d fwd = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
                double side = index % 2 == 0 ? 1.0 : -1.0;
                Vec3d start = new Vec3d(p.getX(), p.getY() + 1.7, p.getZ()).add(right.multiply(side * 0.9)).subtract(fwd.multiply(0.4));

                LivingEntity target = null;
                double best = 30.0 * 30.0;
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(start, start).expand(30.0), x -> Aim.valid(p, x))) {
                    double d = e.squaredDistanceTo(start.x, start.y, start.z);
                    if (d < best) {
                        best = d;
                        target = e;
                    }
                }
                final LivingEntity tg = target;
                final Vec3d look = p.getRotationVec(1.0F);
                shots.add(new Bullet() {
                    private Vec3d pos = start;
                    private Vec3d vel = right.multiply(side * 0.6).add(0.0, 0.5, 0.0);
                    private int age = 0;

                    @Override
                    public boolean step() {
                        Vec3d want = tg != null && tg.isAlive()
                            ? new Vec3d(tg.getX(), tg.getY() + tg.getHeight() * 0.5, tg.getZ()).subtract(pos).normalize()
                            : look;
                        vel = vel.multiply(0.65).add(want.multiply(0.5)).normalize().multiply(1.25);
                        Vec3d prev = pos;
                        pos = pos.add(vel);
                        Ink.bloodBlob(w, pos, 1.18);
                        Ink.bloodStream(w, prev, pos, 0.5 + (0.3F) * 1.2);
                        List<LivingEntity> hit = w.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(1.0), x -> Aim.valid(p, x));
                        if (!hit.isEmpty()) {
                            Dmg.hit(p, hit.get(0), damage);
                            Ink.bloodSplat(w, pos, vel, 1.8);
                            return true;
                        }
                        if (solid(w, pos)) {
                            Ink.bloodSplat(w, pos, vel.multiply(-1.0), 1.8);
                            return true;
                        }
                        return ++age > 40;
                    }
                });
            }
        });
        return true;
    }

    public static boolean castBloodTorrent(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        double damage = 5.0 * s.power();
        s.busyUntil = w.getTime() + 20 + 100 + 10;

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                t++;
                Vec3d look = p.getRotationVec(1.0F);
                Vec3d origin = hand(p);
                ThreadLocalRandom r = ThreadLocalRandom.current();
                if (t % 5 == 1) {
                    p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 10, 2, false, false));
                }

                if (t <= 20) {
                    for (int i = 0; i < 5; i++) {
                        double a = t * 0.6 + i * 1.2566;
                        double rr = 1.6 * (1.0 - t / 20.0);
                        Vec3d o = origin.add(Math.cos(a) * rr, Math.sin(a) * rr, 0.0);
                        Ink.bloodBlob(w, o, 1.25);
                    }
                    Ink.bloodBlob(w, origin, (0.8F + t * 0.12F) * 0.8 + 0.3);
                    if (t == 1) {
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 2.5F, 0.8F);
                    }
                    return false;
                }

                int f = t - 20;
                double len = 32.0;
                for (double d = 0.5; d <= 32.0; d += 0.5) {
                    if (solid(w, origin.add(look.multiply(d)))) {
                        len = d;
                        break;
                    }
                }
                Vec3d end = origin.add(look.multiply(len));
                Vec3d[] b = Fx.basis(look);

                for (int k = 0; k < 3; k++) {
                    double d = r.nextDouble() * len;
                    double ang = r.nextDouble() * Math.PI * 2.0;
                    double rr = Math.sqrt(r.nextDouble()) * 3.0;
                    Vec3d pt = origin.add(look.multiply(d)).add(b[0].multiply(Math.cos(ang) * rr)).add(b[1].multiply(Math.sin(ang) * rr));
                    Ink.bloodBlob(w, pt, 1.70);
                }
                if (f % 3 == 1) {
                    Ink.bloodStream(w, origin, end, 3.0);
                }
                double ringAt = (f * 1.5) % len;
                Vec3d rc = origin.add(look.multiply(ringAt));
                for (int i = 0; i < 6; i++) {
                    double a = i * Math.PI / 3.0;
                    Ink.dot(w, rc.add(b[0].multiply(Math.cos(a) * 3.0)).add(b[1].multiply(Math.sin(a) * 3.0)), Ink.RED, 0.7F);
                }

                if (f % 8 == 1) {
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 2.0F, 0.5F);
                }
                if (f == 1) {
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 3.0F, 0.6F);
                }
                p.swingHand(Hand.MAIN_HAND, true);

                if (f % 5 == 0) {
                    Box box = new Box(origin, end).expand(3.5);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(p, x))) {
                        Vec3d rel = new Vec3d(e.getX() - origin.x, e.getY() + e.getHeight() * 0.5 - origin.y, e.getZ() - origin.z);
                        double along = rel.dotProduct(look);
                        double perp = rel.subtract(look.multiply(along)).length();
                        if (along < 0.0 || along > len || perp > 3.0) continue;
                        Dmg.hit(p, e, damage);
                        e.setVelocity(look.multiply(0.4).add(0.0, 0.1, 0.0));
                        e.velocityDirty = true;
                        Ink.bloodSplat(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), look, 1.8);
                    }
                }

                if (f >= 100) {
                    Ink.ring(w, end, 3.0, Ink.RED, 0.9F);
                    Ink.bloodSpray(w, end, 4.0, 16);
                    Fx.sound(w, end.x, end.y, end.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0F, 0.9F);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    public static void transform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.form == SukunaForm.CHOSO) {
            if (!Players.formReady(p, s)) {
                return;
            }
            awaken(p, s, w, SukunaState.CHOSO_WING_TICKS);
            s.awakenedViaMeter = false;
        } else {
            Players.endAwakening(p, s, now);
        }
    }

    public static void awaken(ServerPlayerEntity p, SukunaState s, ServerWorld w, int ticks) {
        long now = w.getTime();
        s.form = SukunaForm.CHOSO_WING;
        s.formUntil = now + ticks;
        Players.applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.choso_wing"), false);
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 2.0, Ink.RED, 0.9F);
        Ink.ring(w, base.add(0, 1.0, 0), 3.0, Ink.BLOOD, 0.9F);
        Ink.bloodSpray(w, base.add(0, 1.2, 0), 5.0, 24);
        Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 1.0F, 1.4F);
    }

    public static final int CONVERGENCE_STILL_TICKS = 40;
    public static final int CONVERGENCE_WINDOW_TICKS = 200;

    public static boolean convergenceReady(SukunaState s, long now) {
        return s.convergenceCharges > 0 && now < s.convergenceUntil;
    }

    public static void consumeConvergence(ServerPlayerEntity p, SukunaState s) {
        s.convergenceCharges = Math.max(0, s.convergenceCharges - 1);
        if (s.convergenceCharges == 0) {
            s.convergenceUntil = 0L;
        }
        p.sendMessage(Text.translatable("sukuna.msg.choso_convergence_left", s.convergenceCharges), true);
    }

    private static void convergenceTick(ServerPlayerEntity p, SukunaState s, ServerWorld w, long now) {
        double dx = p.getX() - s.lastX;
        double dz = p.getZ() - s.lastZ;
        s.lastX = p.getX();
        s.lastZ = p.getZ();
        if (s.convergenceCharges > 0 && now >= s.convergenceUntil) {
            s.convergenceCharges = 0;
            s.convergenceUntil = 0L;
        }
        boolean still = dx * dx + dz * dz < 1.0E-4 && p.isOnGround() && !s.charging && !s.piercingCharging;
        if (!still || s.convergenceCharges > 0) {
            s.stillTicks = 0;
            return;
        }
        s.stillTicks++;
        if (s.stillTicks % 8 == 0) {
            Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.4 * (1.0 - (double) s.stillTicks / CONVERGENCE_STILL_TICKS) + 0.3, Ink.BLOOD, 0.4F);
        }
        if (s.stillTicks >= CONVERGENCE_STILL_TICKS) {
            s.stillTicks = 0;
            s.convergenceCharges = 2;
            s.convergenceUntil = now + CONVERGENCE_WINDOW_TICKS;
            Ink.ring(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.2, Ink.RED, 0.8F);
            Ink.bloodSpray(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.4, 10);
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.PLAYERS, 1.6F, 1.1F);
            p.sendMessage(Text.translatable("sukuna.msg.choso_convergence"), true);
        }
    }
}
