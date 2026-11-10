package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.entity.TechniqueOrbEntity;
import com.dowerp.sukuna.item.ModItems;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Юта Оккоцу (в разработке). Две формы с отдельными панелями техник: Юта (катана, проклятая речь)
 * и Юта с Рикой (кулаки Рики, хват, щит, рывок и высвобождение проклятой энергии, чем дольше зажимаешь, тем шире, дальше и мощнее).
 */
public final class YutaTasks {
    public static final int RIKA_TICKS = 900;
    public static final int RELEASE_MIN = 12;
    public static final int RELEASE_MAX = 80;

    private static final class Rika {
        TechniqueOrbEntity e;
        Vec3d pos;
        Vec3d target;
        long until;
    }

    private static final Map<UUID, Rika> RIKAS = new HashMap<>();

    private YutaTasks() {
    }

    private static Vec3d feet(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY(), e.getZ());
    }

    private static Vec3d center(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
    }

    // ------------------------------------------------------------------ Рика (3D-сущность рядом с Ютой)
    private static void manifest(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        final Rika r = new Rika();
        Vec3d start = feet(p).add(0.0, 0.5, 0.0);
        r.pos = start;
        r.e = Fx3d.spawn(w, ModEntities.RIKA, start, p.getYaw(), RIKA_TICKS + 40);
        RIKAS.put(p.getUuid(), r);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.PLAYERS, 2.0F, 0.8F);
        Tasks.add(new Task() {
            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.YUTA_RIKA || r.e == null || r.e.isRemoved()) {
                    Fx3d.drop(r.e);
                    RIKAS.remove(p.getUuid(), r);
                    return true;
                }
                long now = w.getTime();
                Vec3d look = p.getRotationVec(1.0F);
                Vec3d flat = new Vec3d(look.x, 0.0, look.z).lengthSquared() > 1.0E-4 ? new Vec3d(look.x, 0.0, look.z).normalize() : new Vec3d(0, 0, 1);
                Vec3d goal;
                float yaw;
                if (r.target != null && r.until > now) {
                    goal = r.target;
                    yaw = Fx3d.yawTo(r.pos, goal.add(flat));
                } else {
                    goal = feet(p).subtract(flat.multiply(1.7)).add(Fx.basis(look)[0].multiply(0.7)).add(0.0, 0.5 + Math.sin(now * 0.12) * 0.2, 0.0);
                    yaw = p.getYaw();
                }
                r.pos = r.pos.add(goal.subtract(r.pos).multiply(0.45));
                Fx3d.place(r.e, r.pos, yaw);
                return false;
            }
        });
    }

    private static void command(ServerPlayerEntity p, Vec3d target, int ticks) {
        Rika r = RIKAS.get(p.getUuid());
        if (r != null) {
            r.target = target;
            r.until = Fx.world(p).getTime() + ticks;
        }
    }

    // ------------------------------------------------------------------ превращение
    public static void transform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.form == SukunaForm.YUTA) {
            if (!Players.formReady(p, s)) {
                return;
            }
            awaken(p, s, w, RIKA_TICKS);
            s.awakenedViaMeter = false;
        } else {
            Players.endAwakening(p, s, now);
        }
    }

    public static void awaken(ServerPlayerEntity p, SukunaState s, ServerWorld w, int ticks) {
        s.form = SukunaForm.YUTA_RIKA;
        s.formUntil = w.getTime() + ticks;
        Players.applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.yuta_rika"), false);
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 2.2, Ink.WHITE, 0.9F);
        Ink.ring(w, base.add(0, 1.0, 0), 3.2, Ink.BLUE, 0.9F);
        manifest(p, s, w);
    }

    // ------------------------------------------------------------------ Юта
    public static boolean castSlash(ServerPlayerEntity p, SukunaState s) {
        final boolean sword = p.getMainHandStack().isOf(ModItems.YUTA_KATANA);
        List<LivingEntity> hits = MakiTasks.cone(p, sword ? 4.6 : 3.4, sword ? 0.3 : 0.45);
        if (hits.isEmpty()) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        int n = 0;
        for (LivingEntity e : hits) {
            if (n++ >= 5) break;
            Dmg.hit(p, e, (sword ? 14.0 : 10.0) * s.power());
            Ink.impact(w, center(e), look, sword ? Ink.WHITE : Ink.PURPLE);
            if (!sword) {
                MakiTasks.push(p, e, 0.8, 0.25);
            }
        }
        if (sword) {
            Vec3d c = p.getEyePos().add(look.multiply(1.8));
            Vec3d side = look.crossProduct(new Vec3d(0, 1, 0));
            Ink.stroke(w, c.add(side.multiply(2.0)).add(0.0, 0.8, 0.0), c.add(side.multiply(-2.0)).add(0.0, -0.8, 0.0), Ink.WHITE, Ink.BLUE, 0.9F, true);
        } else {
            Ink.ring(w, p.getEyePos().add(look.multiply(1.6)), 1.0, Ink.PURPLE, 0.5F);
        }
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.4F, 1.0F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 8;
        Players.markCombat(p);
        return true;
    }

    /** S: Проклятая речь «Замри!» — обездвиживает цели в конусе. */
    public static boolean castSpeech(ServerPlayerEntity p, SukunaState s) {
        List<LivingEntity> hits = MakiTasks.cone(p, 10.0, 0.6);
        if (hits.isEmpty()) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        int n = 0;
        for (LivingEntity e : hits) {
            if (n++ >= 6) break;
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 4, false, true));
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 60, 1, false, true));
            Ink.ring(w, center(e), 1.0, Ink.PURPLE, 0.5F);
        }
        Ink.ring(w, p.getEyePos().add(p.getRotationVec(1.0F).multiply(1.5)), 1.4, Ink.WHITE, 0.6F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EVOKER_PREPARE_ATTACK, SoundCategory.PLAYERS, 2.0F, 0.7F);
        p.sendMessage(Text.translatable("sukuna.msg.yuta_speech"), true);
        s.busyUntil = w.getTime() + 10;
        Players.markCombat(p);
        return true;
    }

    private static boolean dash(ServerPlayerEntity p, double max) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d dir = new Vec3d(look.x, 0.0, look.z).lengthSquared() > 1.0E-4 ? new Vec3d(look.x, 0.0, look.z).normalize() : new Vec3d(0, 0, 1);
        Vec3d start = feet(p);
        Vec3d end = start;
        for (double d = 0.5; d <= max; d += 0.5) {
            Vec3d next = start.add(dir.multiply(d));
            if (!MakiTasks.passable(w, next.add(0.0, 0.2, 0.0)) || !MakiTasks.passable(w, next.add(0.0, 1.5, 0.0))) break;
            end = next;
        }
        p.networkHandler.requestTeleport(end.x, end.y, end.z, p.getYaw(), p.getPitch());
        Ink.stroke(w, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), Ink.WHITE, Ink.BLUE, 0.5F, true);
        Fx.sound(w, end.x, end.y, end.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, 1.5F);
        return !end.equals(start);
    }

    public static boolean castStep(ServerPlayerEntity p, SukunaState s) {
        if (!dash(p, 8.0)) {
            p.sendMessage(Text.translatable("sukuna.msg.blocked"), true);
            return false;
        }
        s.busyUntil = Fx.world(p).getTime() + 6;
        return true;
    }

    public static boolean castBoost(ServerPlayerEntity p, SukunaState s) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 200, 1, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 200, 0, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 200, 0, false, true));
        ServerWorld w = Fx.world(p);
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.5, Ink.BLUE, 0.7F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 1.4F);
        return true;
    }

    // ------------------------------------------------------------------ фиолетовая аура (отличительная черта Юты)
    public static void aura(ServerPlayerEntity p, ServerWorld w) {
        java.util.concurrent.ThreadLocalRandom r = java.util.concurrent.ThreadLocalRandom.current();
        int n = p.getEntityWorld() != null && p.isAlive() ? 3 : 0;
        boolean rika = false;
        SukunaState st = Players.peek(p);
        if (st != null) {
            rika = st.form == SukunaForm.YUTA_RIKA;
        }
        for (int i = 0; i < n + (rika ? 3 : 0); i++) {
            Vec3d at = new Vec3d(p.getX() + r.nextDouble(-0.5, 0.5), p.getY() + r.nextDouble(0.1, 1.9), p.getZ() + r.nextDouble(-0.5, 0.5));
            Ink.dot(w, at, Ink.PURPLE, rika ? 1.1F : 0.8F);
        }
        if (p.age % 18 == 0) {
            Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), rika ? 1.5 : 1.0, Ink.PURPLE, 0.35F);
        }
    }

    // ------------------------------------------------------------------ территория «Подлинная взаимная любовь»
    public static boolean castDomain(ServerPlayerEntity p, SukunaState s) {
        if (s.form != SukunaForm.YUTA_RIKA) {
            return false;
        }
        final ServerWorld w = Fx.world(p);
        final Vec3d c = feet(p);
        final double radius = 18.0;
        final int duration = 400;
        s.domainLeft = duration;
        command(p, c.add(0.0, 1.0, 0.0), 40);
        Ink.ring(w, c.add(0.0, 0.2, 0.0), radius, Ink.PURPLE, 1.2F);
        Ink.ring(w, c.add(0.0, 0.2, 0.0), radius * 0.6, Ink.BLUE, 0.9F);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 4.0F, 0.6F);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 4.0F, 0.5F);
        p.sendMessage(Text.translatable("sukuna.msg.yuta_domain"), false);
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.YUTA_RIKA) {
                    s.domainLeft = 0;
                    return true;
                }
                t++;
                s.domainLeft = duration - t;
                if (t % 5 == 0) {
                    Ink.ring(w, c.add(0.0, 0.3 + (t % 30) * 0.18, 0.0), radius * (0.95 - (t % 30) * 0.004), Ink.PURPLE, 0.5F);
                }
                if (t % 8 == 0) {
                    int n = 0;
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), x -> Aim.valid(p, x) && !Counters.nullifies(x) && x.squaredDistanceTo(c.x, c.y, c.z) <= radius * radius)) {
                        if (n++ >= 8) break;
                        rainSword(p, s, w, e);
                    }
                }
                if (t >= duration) {
                    s.domainLeft = 0;
                    p.sendMessage(Text.translatable("sukuna.msg.yuta_domain_end"), true);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    private static void rainSword(ServerPlayerEntity p, SukunaState s, ServerWorld w, LivingEntity target) {
        final Vec3d top = new Vec3d(target.getX(), target.getY() + 11.0, target.getZ());
        final TechniqueOrbEntity sword = Fx3d.spawn(w, ModEntities.YUTA_SWORD, top, p.getYaw(), 20);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (sword == null || sword.isRemoved()) return true;
                t++;
                Vec3d goal = new Vec3d(target.getX(), target.getY() + 0.2, target.getZ());
                Vec3d pos = top.add(goal.subtract(top).multiply(Math.min(1.0, t / 7.0)));
                Fx3d.place(sword, pos.subtract(0.0, 0.1, 0.0), p.getYaw());
                if (t >= 7) {
                    Fx3d.drop(sword);
                    if (target.isAlive() && !target.isRemoved() && !p.isRemoved() && !Counters.nullifies(target)) {
                        Dmg.hit(p, target, 9.0 * s.power());
                        Ink.impact(w, center(target), new Vec3d(0, -1, 0), Ink.PURPLE);
                        Fx.sound(w, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.0F, 1.3F);
                    }
                    return true;
                }
                return false;
            }
        });
    }

    public static boolean castRikaAbility(Ability a, ServerPlayerEntity p, SukunaState s) {
        if (s.form != SukunaForm.YUTA_RIKA) {
            return false;
        }
        return switch (a) {
            case RIKA_FIST -> castRikaFist(p, s);
            case RIKA_GRAB -> castRikaGrab(p, s);
            case RIKA_GUARD -> castRikaGuard(p, s);
            case RIKA_RUSH -> castRikaDash(p, s);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ Рика
    public static boolean castRikaFist(ServerPlayerEntity p, SukunaState s) {
        final LivingEntity target = com.dowerp.sukuna.Aim.target(p, 12.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        final ServerWorld w = Fx.world(p);
        command(p, feet(target).add(0.0, 0.5, 0.0), 12);
        s.busyUntil = w.getTime() + 12;
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || !target.isAlive() || target.isRemoved()) return true;
                if (++t < 5) return false;
                Vec3d dir = center(target).subtract(center(p));
                dir = dir.lengthSquared() > 1.0E-4 ? dir.normalize() : new Vec3d(0, 0, 1);
                Dmg.hit(p, target, 22.0 * s.power());
                target.setVelocity(dir.multiply(1.6).add(0.0, 0.4, 0.0));
                target.velocityDirty = true;
                Ink.impact(w, center(target), dir, Ink.WHITE);
                Fx.sound(w, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 2.0F, 0.7F);
                return true;
            }
        });
        return true;
    }

    public static boolean castRikaGrab(ServerPlayerEntity p, SukunaState s) {
        final LivingEntity target = com.dowerp.sukuna.Aim.target(p, 14.0);
        if (target == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        final ServerWorld w = Fx.world(p);
        command(p, feet(target).add(0.0, 0.5, 0.0), 14);
        s.busyUntil = w.getTime() + 16;
        Players.markCombat(p);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || !target.isAlive() || target.isRemoved()) return true;
                t++;
                if (t <= 8) {
                    Vec3d to = feet(p).add(p.getRotationVec(1.0F).multiply(1.8)).subtract(feet(target));
                    if (to.lengthSquared() > 1.0) {
                        target.setVelocity(to.normalize().multiply(0.9));
                        target.velocityDirty = true;
                    }
                }
                if (t == 9) {
                    Dmg.hit(p, target, 12.0 * s.power());
                    target.setVelocity(0.0, -0.8, 0.0);
                    target.velocityDirty = true;
                    target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, false, true));
                    Ink.ring(w, feet(target).add(0.0, 0.1, 0.0), 2.0, Ink.WHITE, 0.7F);
                    Fx.sound(w, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.2F, 1.4F);
                    return true;
                }
                return false;
            }
        });
        return true;
    }

    public static boolean castRikaGuard(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 2, false, true));
        Vec3d look = p.getRotationVec(1.0F);
        command(p, feet(p).add(new Vec3d(look.x, 0.0, look.z).multiply(1.6)), 60);
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.6, Ink.WHITE, 0.6F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 1.5F, 0.7F);
        return true;
    }

    public static boolean castRikaDash(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        if (!dash(p, 10.0)) {
            p.sendMessage(Text.translatable("sukuna.msg.blocked"), true);
            return false;
        }
        Vec3d at = feet(p);
        command(p, at.subtract(p.getRotationVec(1.0F).multiply(1.0)), 10);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(at, at).expand(3.0), x -> com.dowerp.sukuna.Aim.valid(p, x))) {
            Dmg.hit(p, e, 12.0 * s.power());
            Vec3d d = center(e).subtract(center(p));
            if (d.lengthSquared() > 1.0E-4) {
                e.setVelocity(d.normalize().multiply(1.2).add(0.0, 0.3, 0.0));
                e.velocityDirty = true;
            }
        }
        s.busyUntil = w.getTime() + 8;
        Players.markCombat(p);
        return true;
    }

    // ------------------------------------------------------------------ высвобождение проклятой энергии (удерживай)
    private static EntityType<TechniqueOrbEntity> tierType(int tier) {
        return tier == 0 ? ModEntities.RELEASE_S : tier == 1 ? ModEntities.RELEASE_M : ModEntities.RELEASE_L;
    }

    private static double tierRadius(int tier) {
        return tier == 0 ? 0.375 : tier == 1 ? 0.625 : 1.0;
    }

    public static boolean castRelease(ServerPlayerEntity p, SukunaState s) {
        final ServerWorld w = Fx.world(p);
        s.piercingCharging = true;
        s.busyUntil = w.getTime() + RELEASE_MIN + 4;
        Vec3d look0 = p.getRotationVec(1.0F);
        command(p, feet(p).add(new Vec3d(look0.x, 0.0, look0.z).multiply(1.2)).add(0.0, 0.6, 0.0), RELEASE_MAX + 20);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.5F, 0.6F);
        Tasks.add(new Task() {
            private int t = 0;
            private int tier = -1;
            private TechniqueOrbEntity orb;

            private Vec3d hand() {
                return p.getEyePos().add(p.getRotationVec(1.0F).multiply(1.5)).add(0.0, -0.3, 0.0);
            }

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.YUTA_RIKA) {
                    Fx3d.drop(orb);
                    s.piercingCharging = false;
                    return true;
                }
                t++;
                boolean holding = s.held(Ability.ULTIMATE_SLASH);
                if (t < RELEASE_MIN || (holding && t < RELEASE_MAX)) {
                    s.busyUntil = w.getTime() + 4;
                    double k = (double) t / RELEASE_MAX;
                    int newTier = k < 0.34 ? 0 : k < 0.67 ? 1 : 2;
                    if (newTier != tier || orb == null || orb.isRemoved()) {
                        Fx3d.drop(orb);
                        tier = newTier;
                        orb = Fx3d.spawn(w, tierType(tier), hand().subtract(0.0, tierRadius(tier), 0.0), p.getYaw(), 30);
                    }
                    Fx3d.place(orb, hand().subtract(0.0, tierRadius(tier), 0.0), p.getYaw());
                    if (t % 10 == 0) {
                        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.0 + k * 3.0, Ink.BLUE, 0.5F);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.0F, (float) (0.6 + k));
                    }
                    if (t % 4 == 0) {
                        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 1, false, false));
                    }
                    return false;
                }
                Fx3d.drop(orb);
                s.piercingCharging = false;
                fire((double) Math.min(t, RELEASE_MAX) / RELEASE_MAX);
                return true;
            }

            private void fire(double k) {
                double damage = (30.0 + 120.0 * k) * s.power();
                double length = 12.0 + 30.0 * k;
                double radius = 1.2 + 3.8 * k;
                int tr = k < 0.34 ? 0 : k < 0.67 ? 1 : 2;
                Vec3d dir = p.getRotationVec(1.0F);
                Vec3d start = hand();
                final Set<UUID> hit = new HashSet<>();
                double speed = 2.6;
                int ticks = (int) Math.ceil(length / speed);
                Ink.ring(w, start, 1.0 + k * 2.0, Ink.WHITE, 0.8F);
                Ink.stroke(w, start, start.add(dir.multiply(length)), Ink.WHITE, Ink.BLUE, (float) (0.8 + k * 2.4), true);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, (float) (2.0 + k * 2.0), (float) (1.6 - k * 0.6));
                Fx3d.pulse(w, tierType(tr), p, start, dir, speed, ticks, radius, k >= 0.95, e -> {
                    if (!hit.add(e.getUuid())) return;
                    double dmg = e instanceof PlayerEntity ? Math.min(damage, e.getMaxHealth() * 0.7) : damage;
                    Dmg.hit(p, e, dmg);
                    e.setVelocity(dir.multiply(1.0 + 2.0 * k).add(0.0, 0.4, 0.0));
                    e.velocityDirty = true;
                    Ink.impact(w, center(e), dir, Ink.WHITE);
                });
                p.swingHand(Hand.MAIN_HAND, true);
                Players.markCombat(p);
            }
        });
        return true;
    }
}
