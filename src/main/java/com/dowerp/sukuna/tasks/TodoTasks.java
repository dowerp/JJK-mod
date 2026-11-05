package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
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
import java.util.List;

public final class TodoTasks {
    public static final double SWAP_RANGE = 12.0;
    public static final double MAX_RANGE = 16.0;
    public static final int BOOST_TICKS = 300;
    public static final int MAX_TARGETS = 24;

    private TodoTasks() {
    }

    private static void moveTo(LivingEntity e, Vec3d pos) {
        if (e instanceof ServerPlayerEntity sp) {
            sp.networkHandler.requestTeleport(pos.x, pos.y, pos.z, sp.getYaw(), sp.getPitch());
        } else {
            e.refreshPositionAndAngles(pos.x, pos.y, pos.z, e.getYaw(), e.getPitch());
            if (e instanceof MobEntity m) {
                m.getNavigation().stop();
            }
        }
        e.fallDistance = 0.0;
    }

    private static void clapFx(ServerWorld w, Vec3d at, float pitch) {
        Ink.ring(w, at.add(0.0, 1.0, 0.0), 1.1, Ink.GOLD, 0.7F);
        Ink.ring(w, at.add(0.0, 0.1, 0.0), 1.6, Ink.WHITE, 0.5F);
        Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.0F, pitch);
        Fx.sound(w, at.x, at.y, at.z, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.2F, 1.8F);
    }

    private static void swap(ServerWorld w, ServerPlayerEntity p, LivingEntity t) {
        Vec3d a = new Vec3d(p.getX(), p.getY(), p.getZ());
        Vec3d b = new Vec3d(t.getX(), t.getY(), t.getZ());
        clapFx(w, a, 1.3F);
        moveTo(p, b);
        moveTo(t, a);
        clapFx(w, b, 1.0F);
        p.swingHand(Hand.MAIN_HAND, true);
    }

    public static boolean castBoogieWoogie(ServerPlayerEntity p, SukunaState s) {
        LivingEntity t = Aim.target(p, SWAP_RANGE);
        if (t == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        swap(Fx.world(p), p, t);
        Players.markCombat(p);
        return true;
    }

    public static boolean castDashStrike(ServerPlayerEntity p, SukunaState s) {
        LivingEntity t = Aim.target(p, 9.0);
        if (t == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        Vec3d tp = new Vec3d(t.getX(), t.getY(), t.getZ());
        Vec3d dir = new Vec3d(tp.x - p.getX(), 0.0, tp.z - p.getZ());
        if (dir.lengthSquared() < 1.0E-4) {
            dir = p.getRotationVec(1.0F);
        }
        dir = dir.normalize();
        Vec3d from = new Vec3d(p.getX(), p.getY() + 1.0, p.getZ());
        moveTo(p, tp.subtract(dir.multiply(1.3)));
        Ink.stroke(w, from, tp.add(0.0, 1.0, 0.0), Ink.GOLD, Ink.WHITE, 0.6F, false);
        Dmg.hit(p, t, 18.0 * s.power());
        t.setVelocity(dir.multiply(1.8).add(0.0, 0.5, 0.0));
        t.velocityDirty = true;
        Vec3d mid = new Vec3d(t.getX(), t.getY() + t.getHeight() * 0.5, t.getZ());
        Ink.impact(w, mid, dir, Ink.GOLD);
        Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.4F, 0.7F);
        p.swingHand(Hand.MAIN_HAND, true);
        return true;
    }

    private static boolean passable(ServerWorld w, Vec3d v) {
        BlockPos bp = BlockPos.ofFloored(v.x, v.y, v.z);
        return w.isPosLoaded(bp) && w.getBlockState(bp).getCollisionShape(w, bp).isEmpty();
    }

    public static boolean castClapDash(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        Vec3d look = p.getRotationVec(1.0F);
        Vec3d dir = new Vec3d(look.x, 0.0, look.z);
        if (dir.lengthSquared() < 1.0E-4) {
            dir = new Vec3d(0.0, 0.0, 1.0);
        }
        dir = dir.normalize();
        Vec3d start = new Vec3d(p.getX(), p.getY(), p.getZ());
        Vec3d end = start;
        for (double d = 0.5; d <= 5.0; d += 0.5) {
            Vec3d next = start.add(dir.multiply(d));
            if (!passable(w, next.add(0.0, 0.2, 0.0)) || !passable(w, next.add(0.0, 1.5, 0.0))) {
                break;
            }
            end = next;
        }
        clapFx(w, start, 1.5F);
        moveTo(p, end);
        clapFx(w, end, 1.2F);
        LivingEntity near = null;
        double best = 4.0;
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(end, end).expand(2.0), x -> Aim.valid(p, x))) {
            double d2 = e.squaredDistanceTo(end.x, end.y, end.z);
            if (d2 <= best) {
                best = d2;
                near = e;
            }
        }
        if (near != null) {
            Vec3d a = end;
            Vec3d b = new Vec3d(near.getX(), near.getY(), near.getZ());
            moveTo(p, b);
            moveTo(near, a);
            clapFx(w, b, 0.9F);
            s.ce += Ability.UTILITY_TECHNIQUE.costFor(s.form);
            Ability prim = Ability.PRIMARY_ATTACK;
            long now = w.getTime();
            long left = s.cooldowns[prim.ordinal()] - now;
            if (left > 0L) {
                s.cooldowns[prim.ordinal()] = now + left / 2L;
            }
            p.sendMessage(Text.translatable("sukuna.msg.todo_free_swap"), true);
            Players.markCombat(p);
        }
        p.swingHand(Hand.MAIN_HAND, true);
        return true;
    }

    public static boolean castBoost(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        if (s.todoBuffUntil > w.getTime()) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.todoBuffUntil = w.getTime() + BOOST_TICKS;
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.4, Ink.GOLD, 0.8F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.6F);
        p.sendMessage(Text.translatable("sukuna.msg.todo_boost"), true);
        return true;
    }

    public static boolean castMaximum(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        List<LivingEntity> targets = new ArrayList<>(w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(MAX_RANGE), x -> Aim.valid(p, x) && x.squaredDistanceTo(p) <= MAX_RANGE * MAX_RANGE));
        if (targets.isEmpty()) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        if (targets.size() > MAX_TARGETS) {
            targets = new ArrayList<>(targets.subList(0, MAX_TARGETS));
        }
        List<LivingEntity> all = new ArrayList<>();
        all.add(p);
        all.addAll(targets);
        int n = all.size();
        List<Vec3d> pos = new ArrayList<>(n);
        for (LivingEntity e : all) {
            pos.add(new Vec3d(e.getX(), e.getY(), e.getZ()));
        }
        for (int i = 0; i < n; i++) {
            clapFx(w, pos.get(i), 1.0F);
        }
        for (int i = 0; i < n; i++) {
            moveTo(all.get(i), pos.get((i + 1) % n));
        }
        double dmg = 25.0 * s.power();
        Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
        for (LivingEntity e : targets) {
            Dmg.hit(p, e, dmg);
            Vec3d away = new Vec3d(e.getX() - c.x, 0.0, e.getZ() - c.z);
            if (away.lengthSquared() < 1.0E-4) {
                away = new Vec3d(1.0, 0.0, 0.0);
            }
            e.setVelocity(away.normalize().multiply(1.4).add(0.0, 0.5, 0.0));
            e.velocityDirty = true;
            Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ()), away.normalize(), Ink.GOLD);
        }
        for (int i = 0; i < 3; i++) {
            Ink.ring(w, c.add(0.0, 0.1 + i * 0.6, 0.0), 3.0 + i * 3.0, i == 1 ? Ink.WHITE : Ink.GOLD, 0.8F);
        }
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0F, 1.4F);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.5F, 1.6F);
        p.swingHand(Hand.MAIN_HAND, true);
        Players.markCombat(p);
        return true;
    }

    public static void tick(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        if (p.age % 10 != 0) return;
        boolean active = s.todoBuffUntil > w.getTime();
        EntityAttributeInstance speed = p.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        EntityAttributeInstance jump = p.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
        if (speed != null) {
            speed.removeModifier(Players.MOD_TODO_SPEED);
            if (active) {
                speed.addTemporaryModifier(new EntityAttributeModifier(Players.MOD_TODO_SPEED, 0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (jump != null) {
            jump.removeModifier(Players.MOD_TODO_JUMP);
            if (active) {
                jump.addTemporaryModifier(new EntityAttributeModifier(Players.MOD_TODO_JUMP, 0.3, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    public static void transform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.form == SukunaForm.TODO) {
            boolean viaMeter = s.meterFull();
            if (!viaMeter) {
                if (now < s.formCooldownUntil) {
                    p.sendMessage(Text.translatable("sukuna.msg.cooldown", (s.formCooldownUntil - now + 19L) / 20L), true);
                    return;
                }
                if (s.ce < s.maxCe() * 0.5) {
                    p.sendMessage(Text.translatable("sukuna.msg.todo_need_ce"), true);
                    return;
                }
            }
            awaken(p, s, w, SukunaState.TODO_AWAKEN_TICKS);
            if (viaMeter) {
                s.domainMeter = Math.max(0.0, s.domainMeter - SukunaState.METER_MAX);
            }
            s.awakenedViaMeter = viaMeter;
        } else {
            Players.endAwakening(p, s, now);
        }
    }

    public static void awaken(ServerPlayerEntity p, SukunaState s, ServerWorld w, int ticks) {
        s.form = SukunaForm.TODO_AWAKENED;
        s.formUntil = w.getTime() + ticks;
        Players.applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.todo_awakened"), false);
        Vec3d base = new Vec3d(p.getX(), p.getY() + 0.1, p.getZ());
        Ink.ring(w, base, 2.0, Ink.GOLD, 0.9F);
        Ink.ring(w, base.add(0, 1.0, 0), 3.0, Ink.WHITE, 0.9F);
        Fx.sound(w, base.x, base.y, base.z, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0F, 1.3F);
    }
}
