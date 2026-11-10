package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.entity.TechniqueOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/** Мегуми Фушигуро: Десять теней. Сикигами это 3D-сущности GeckoLib, которыми управляют серверные задачи. */
public final class MegumiTasks {
    private MegumiTasks() {
    }

    private static Vec3d feet(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY(), e.getZ());
    }

    private static Vec3d center(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
    }

    private static boolean gone(ServerPlayerEntity p, LivingEntity t) {
        return !p.isAlive() || p.isRemoved() || t == null || !t.isAlive() || t.isRemoved() || t.getEntityWorld() != p.getEntityWorld();
    }

    public static void transform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        if (s.form == SukunaForm.MEGUMI) {
            if (!Players.formReady(p, s)) {
                return;
            }
            awaken(p, s, w, 900);
            s.awakenedViaMeter = false;
        } else {
            Players.endAwakening(p, s, w.getTime());
        }
    }

    public static void awaken(ServerPlayerEntity p, SukunaState s, ServerWorld w, int ticks) {
        s.form = SukunaForm.MEGUMI_BEASTS;
        s.formUntil = w.getTime() + ticks;
        Players.applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.megumi_beasts"), false);
        Vec3d base = feet(p);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, base, 0.0F, 30);
        Ink.ring(w, base.add(0.0, 0.1, 0.0), 2.4, Ink.PURPLE, 0.9F);
        Ink.ring(w, base.add(0.0, 1.0, 0.0), 3.2, Ink.BLUE, 0.9F);
        Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.2F, 0.8F);
    }

    private static boolean beastPulse(ServerPlayerEntity p, SukunaState s, net.minecraft.entity.EntityType<TechniqueOrbEntity> type,
                                      double speed, int ticks, double radius, double dmg, double knock, boolean pierce) {
        final ServerWorld w = Fx.world(p);
        final Vec3d dir = p.getRotationVec(1.0F);
        final Vec3d start = p.getEyePos().add(dir.multiply(1.6)).subtract(0.0, 1.0, 0.0);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, feet(p).add(dir.x, 0.0, dir.z), 0.0F, 14);
        final Set<java.util.UUID> hit = new HashSet<>();
        Fx3d.pulse(w, type, p, start, dir, speed, ticks, radius, pierce, e -> {
            if (!hit.add(e.getUuid())) return;
            Dmg.hit(p, e, dmg * s.power());
            e.setVelocity(dir.multiply(knock).add(0.0, 0.45, 0.0));
            e.velocityDirty = true;
            Ink.impact(w, center(e), dir, Ink.PURPLE);
        });
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 14;
        Players.markCombat(p);
        return true;
    }

    /** P (звери): Великий змей пронзает линию перед тобой. */
    public static boolean castSerpent(ServerPlayerEntity p, SukunaState s) {
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.4F, 1.6F);
        return beastPulse(p, s, ModEntities.SHIKI_SERPENT, 1.5, 11, 1.7, 16.0, 1.2, false);
    }

    /** S (звери): Пронзающий бык таранит вперёд. */
    public static boolean castOx(ServerPlayerEntity p, SukunaState s) {
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.6F, 1.0F);
        return beastPulse(p, s, ModEntities.SHIKI_OX, 2.0, 9, 1.9, 24.0, 2.0, false);
    }

    /** V (звери) и B (Агито): Тигр прыгает вперёд, ультимейт бьёт по большой площади. */
    public static boolean castTiger(ServerPlayerEntity p, SukunaState s, boolean ult) {
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.6F, 0.6F);
        return beastPulse(p, s, ModEntities.SHIKI_TIGER, ult ? 2.2 : 2.0, ult ? 12 : 8, ult ? 3.2 : 2.2, ult ? 55.0 : 28.0, ult ? 2.4 : 1.6, ult);
    }

    /** C (звери): Круглый олень лечит владельца. */
    public static boolean castDeer(ServerPlayerEntity p, SukunaState s) {
        final ServerWorld w = Fx.world(p);
        Vec3d right = Fx.basis(p.getRotationVec(1.0F))[0];
        final Vec3d offset = right.multiply(1.6);
        final TechniqueOrbEntity deer = Fx3d.spawn(w, ModEntities.SHIKI_DEER, feet(p).add(offset), p.getYaw(), 70);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, feet(p).add(offset), 0.0F, 14);
        p.heal(10.0F);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, false, true));
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.5F, 1.4F);
        Ink.ring(w, feet(p).add(0.0, 0.2, 0.0), 1.8, Ink.GOLD, 0.7F);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (deer == null || deer.isRemoved() || !p.isAlive() || p.isRemoved()) {
                    Fx3d.drop(deer);
                    return true;
                }
                t++;
                Fx3d.place(deer, feet(p).add(offset), p.getYaw());
                if (t % 15 == 0) {
                    Ink.ring(w, feet(p).add(0.0, 0.3, 0.0), 1.4, Ink.GOLD, 0.5F);
                }
                if (t >= 60) {
                    Fx3d.drop(deer);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    /** P: Божественные псы. Белый и чёрный пёс бегут к цели и кусают её. */
    public static boolean castDivineDogs(ServerPlayerEntity p, SukunaState s) {
        final LivingEntity target = Aim.target(p, 20.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        final ServerWorld w = Fx.world(p);
        Vec3d right = Fx.basis(p.getRotationVec(1.0F))[0];
        Vec3d base = feet(p);
        final Vec3d[] pos = {base.add(right.multiply(1.2)), base.add(right.multiply(-1.2))};
        final TechniqueOrbEntity[] dogs = {
            Fx3d.spawn(w, ModEntities.SHIKI_DOG, pos[0], p.getYaw(), 70),
            Fx3d.spawn(w, ModEntities.SHIKI_DOG_BLACK, pos[1], p.getYaw(), 70)};
        for (Vec3d v : pos) {
            Fx3d.spawn(w, ModEntities.SHADOW_POOL, v, 0.0F, 18);
        }
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.5F, 0.9F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 12;
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (gone(p, target)) {
                    Fx3d.drop(dogs[0]);
                    Fx3d.drop(dogs[1]);
                    return true;
                }
                t++;
                Vec3d tc = feet(target);
                for (int i = 0; i < 2; i++) {
                    Vec3d side = Fx.basis(tc.subtract(pos[i]).lengthSquared() > 1.0E-4 ? tc.subtract(pos[i]).normalize() : new Vec3d(0, 0, 1))[0].multiply(i == 0 ? 0.9 : -0.9);
                    Vec3d goal = tc.add(side);
                    Vec3d to = goal.subtract(pos[i]);
                    double d = to.length();
                    if (d > 1.0) {
                        pos[i] = pos[i].add(to.normalize().multiply(Math.min(d, 1.15)));
                    }
                    Fx3d.place(dogs[i], pos[i].add(0.0, Math.abs(Math.sin((t + i * 3) * 0.7)) * 0.25, 0.0), Fx3d.yawTo(pos[i], tc));
                    if (d <= 1.9 && (t + i * 4) % 8 == 0) {
                        Dmg.hit(p, target, 6.0 * s.power());
                        Ink.impact(w, center(target), tc.subtract(pos[i]).lengthSquared() > 1.0E-4 ? tc.subtract(pos[i]).normalize() : new Vec3d(1, 0, 0), Ink.WHITE);
                        Fx.sound(w, tc.x, tc.y, tc.z, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0F, 1.2F);
                    }
                }
                if (t >= 56) {
                    Fx3d.drop(dogs[0]);
                    Fx3d.drop(dogs[1]);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    /** S: Нуэ. Кружит над целью, пикирует и бьёт молнией по площади. */
    public static boolean castNue(ServerPlayerEntity p, SukunaState s) {
        final LivingEntity target = Aim.target(p, 26.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        final ServerWorld w = Fx.world(p);
        final Vec3d start = feet(p).add(0.0, 2.0, 0.0);
        final TechniqueOrbEntity nue = Fx3d.spawn(w, ModEntities.SHIKI_NUE, start, p.getYaw(), 70);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, feet(p), 0.0F, 14);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 2.0F, 1.5F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 20;
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;
            private Vec3d pos = start;
            private Vec3d strike;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || nue == null || nue.isRemoved()) {
                    Fx3d.drop(nue);
                    return true;
                }
                t++;
                Vec3d tc = target.isAlive() && !target.isRemoved() ? feet(target) : (strike != null ? strike : feet(p));
                if (t <= 22) {
                    double a = t * 0.5;
                    Vec3d orbit = tc.add(Math.cos(a) * 4.0, 9.0 + Math.sin(t * 0.3), Math.sin(a) * 4.0);
                    pos = pos.add(orbit.subtract(pos).multiply(0.25));
                    if (t % 5 == 0) {
                        Ink.dot(w, pos.add(0.0, 0.8, 0.0), Ink.BLUE, 0.9F);
                    }
                } else if (t <= 30) {
                    if (strike == null) {
                        strike = tc;
                        Fx.sound(w, tc.x, tc.y, tc.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 2.0F, 1.6F);
                    }
                    pos = pos.add(strike.subtract(pos).multiply(0.45));
                    if (t == 30) {
                        Vec3d hit = strike;
                        Ink.bolt(w, hit.add(0.0, 14.0, 0.0), hit, Ink.WHITE, Ink.BLUE, 1.4F);
                        Ink.ring(w, hit.add(0.0, 0.1, 0.0), 3.2, Ink.BLUE, 0.8F);
                        Fx.sound(w, hit.x, hit.y, hit.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 3.0F, 1.2F);
                        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(hit, hit).expand(3.2), x -> Aim.valid(p, x))) {
                            Dmg.hit(p, e, 20.0 * s.power());
                            e.setVelocity(e.getVelocity().add(0.0, 0.5, 0.0));
                            e.velocityDirty = true;
                        }
                    }
                } else {
                    pos = pos.add(0.0, 0.8, 0.0);
                }
                Fx3d.place(nue, pos, Fx3d.yawTo(pos, tc));
                if (t >= 40) {
                    Fx3d.drop(nue);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    /** U: Жаба. Язык хватает цель и подтягивает к Мегуми. */
    public static boolean castToad(ServerPlayerEntity p, SukunaState s) {
        final LivingEntity target = Aim.target(p, 14.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        final ServerWorld w = Fx.world(p);
        Vec3d right = Fx.basis(p.getRotationVec(1.0F))[0];
        final Vec3d toadPos = feet(p).add(right.multiply(1.5));
        final TechniqueOrbEntity toad = Fx3d.spawn(w, ModEntities.SHIKI_TOAD, toadPos, Fx3d.yawTo(toadPos, feet(target)), 40);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, toadPos, 0.0F, 14);
        Fx.sound(w, toadPos.x, toadPos.y, toadPos.z, SoundEvents.ENTITY_FROG_TONGUE, SoundCategory.PLAYERS, 1.6F, 0.8F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 16;
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (gone(p, target) || toad == null || toad.isRemoved()) {
                    Fx3d.drop(toad);
                    return true;
                }
                t++;
                Vec3d mouth = toadPos.add(0.0, 0.9, 0.0);
                if (t <= 10) {
                    Ink.stroke(w, mouth, center(target), Ink.BLOOD, Ink.WHITE, 0.45F, false);
                }
                if (t <= 8) {
                    Vec3d to = feet(p).subtract(feet(target));
                    if (to.lengthSquared() > 4.0) {
                        target.setVelocity(to.normalize().multiply(0.95).add(0.0, 0.1, 0.0));
                        target.velocityDirty = true;
                    }
                }
                if (t == 8) {
                    Dmg.hit(p, target, 8.0 * s.power());
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, true));
                }
                if (t >= 30) {
                    Fx3d.drop(toad);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    /** V: Кролики-побег. Рой отвлекает врагов (слепота и замедление), Мегуми ускоряется и невидим. */
    public static boolean castRabbits(ServerPlayerEntity p, SukunaState s) {
        final ServerWorld w = Fx.world(p);
        final Vec3d origin = feet(p);
        final int n = 8;
        final TechniqueOrbEntity[] rabbits = new TechniqueOrbEntity[n];
        for (int i = 0; i < n; i++) {
            rabbits[i] = Fx3d.spawn(w, ModEntities.SHIKI_RABBIT, origin, 0.0F, 50);
        }
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, origin, 0.0F, 20);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(8.0), x -> Aim.valid(p, x))) {
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0, false, true));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1, false, true));
        }
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 50, 0, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 100, 1, false, true));
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RABBIT_JUMP, SoundCategory.PLAYERS, 2.0F, 1.4F);
        p.sendMessage(Text.translatable("sukuna.msg.megumi_rabbits"), true);
        p.swingHand(Hand.MAIN_HAND, true);
        Players.markCombat(p);
        final double[] ang = new double[n];
        for (int i = 0; i < n; i++) {
            ang[i] = i * Math.PI * 2.0 / n + ThreadLocalRandom.current().nextDouble(-0.2, 0.2);
        }
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                t++;
                double r = 0.5 + Math.min(t, 22) * 0.32;
                for (int i = 0; i < n; i++) {
                    Vec3d at = origin.add(Math.cos(ang[i]) * r, Math.abs(Math.sin(t * 0.7 + i)) * 0.4, Math.sin(ang[i]) * r);
                    Fx3d.place(rabbits[i], at, (float) Math.toDegrees(-ang[i]) + 90.0F);
                }
                if (t >= 44 || p.isRemoved()) {
                    for (TechniqueOrbEntity rb : rabbits) {
                        Fx3d.drop(rb);
                    }
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    /** B: Максимальный слон. Слон поднимается из тени и обрушивает поток воды вперёд. */
    public static boolean castElephant(ServerPlayerEntity p, SukunaState s) {
        final ServerWorld w = Fx.world(p);
        final Vec3d look = p.getRotationVec(1.0F);
        final Vec3d flat = new Vec3d(look.x, 0.0, look.z).lengthSquared() > 1.0E-4 ? new Vec3d(look.x, 0.0, look.z).normalize() : new Vec3d(0, 0, 1);
        final Vec3d base = feet(p).add(flat.multiply(3.5));
        final float yaw = p.getYaw();
        final TechniqueOrbEntity elephant = Fx3d.spawn(w, ModEntities.SHIKI_ELEPHANT, base.subtract(0.0, 2.5, 0.0), yaw, 60);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, base, 0.0F, 30);
        Fx3d.spawn(w, ModEntities.SHADOW_POOL, base.add(flat.multiply(1.5)), 0.0F, 30);
        Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 2.5F, 0.6F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 40;
        Players.markCombat(p);
        final Set<java.util.UUID> hit = new HashSet<>();
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || elephant == null || elephant.isRemoved()) {
                    Fx3d.drop(elephant);
                    return true;
                }
                t++;
                double rise = Math.min(1.0, t / 10.0);
                Fx3d.place(elephant, base.subtract(0.0, 2.5 * (1.0 - rise), 0.0), yaw);
                if (t == 14) {
                    Vec3d tip = base.add(0.0, 2.6, 0.0).add(flat.multiply(-1.5));
                    Vec3d dir = p.getRotationVec(1.0F);
                    Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 3.0F, 0.7F);
                    for (int i = -1; i <= 1; i++) {
                        Vec3d off = Fx.basis(dir)[0].multiply(i * 0.9);
                        Fx3d.pulse(w, ModEntities.WATER_SURGE, p, tip.add(off), dir, 1.6, 9, 1.9, false, e -> {
                            if (!hit.add(e.getUuid())) return;
                            Dmg.hit(p, e, 40.0 * s.power());
                            e.setVelocity(dir.multiply(1.8).add(0.0, 0.5, 0.0));
                            e.velocityDirty = true;
                            Ink.impact(w, center(e), dir, Ink.BLUE);
                        });
                    }
                }
                if (t >= 40) {
                    Fx3d.drop(elephant);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

}
