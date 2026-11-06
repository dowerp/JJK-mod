package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import com.dowerp.sukuna.item.ModItems;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Маки Зенин: Небесное ограничение, ПЭ нет. Весь урон считается на сервере по фактическому положению целей
 * в момент удара (конус / линия), клиент ничего не сообщает о попаданиях.
 */
public final class MakiTasks {
    public static final int ANIM_SLASH = 5;
    public static final int ANIM_ULTIMATE = 6;
    public static final int ANIM_ATTACK = 7;

    public static final int SENSE_TICKS = 160;
    public static final double SENSE_RANGE = 20.0;
    public static final int SOUL_WINDUP = 10;
    public static final int ABSOLUTE_WINDUP = 18;
    public static final double ABSOLUTE_LENGTH = 14.0;

    private MakiTasks() {
    }

    public static final int WEAPON_KATANA = 0;
    public static final int WEAPON_CLOUD = 1;
    public static final int WEAPON_FISTS = 2;

    public static boolean holdsKatana(ServerPlayerEntity p) {
        return p.getMainHandStack().isOf(ModItems.SOUL_SPLIT_KATANA);
    }

    public static boolean holdsCloud(ServerPlayerEntity p) {
        return p.getMainHandStack().isOf(ModItems.PLAYFUL_CLOUD);
    }

    private static boolean needKatana(ServerPlayerEntity p) {
        if (holdsKatana(p) || holdsCloud(p)) {
            return false;
        }
        p.sendMessage(Text.translatable("sukuna.msg.maki_need_katana"), true);
        return true;
    }

    /** Выбор оружия в колесе (U): катана, Игривое облако или кулаки. Работает только у Маки. */
    public static void equip(ServerPlayerEntity p, int weapon) {
        SukunaState s = Players.get(p);
        if (s.form != SukunaForm.MAKI || !p.isAlive()) return;
        if (Fx.world(p).getTime() < s.busyUntil) return;
        var inv = p.getInventory();
        int sel = inv.getSelectedSlot();
        net.minecraft.item.Item want = weapon == WEAPON_KATANA ? ModItems.SOUL_SPLIT_KATANA : weapon == WEAPON_CLOUD ? ModItems.PLAYFUL_CLOUD : null;
        net.minecraft.item.ItemStack inHand = inv.getStack(sel);
        if (want == null) {
            if (inHand.isEmpty()) return;
            if (inHand.isOf(ModItems.SOUL_SPLIT_KATANA) || inHand.isOf(ModItems.PLAYFUL_CLOUD)) {
                for (int i = 0; i < 36; i++) {
                    if (i != sel && inv.getStack(i).isEmpty()) {
                        inv.setStack(i, inHand);
                        inv.setStack(sel, net.minecraft.item.ItemStack.EMPTY);
                        break;
                    }
                }
            }
            return;
        }
        if (inHand.isOf(want)) return;
        int found = -1;
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(want)) {
                found = i;
                break;
            }
        }
        net.minecraft.item.ItemStack stack = found >= 0 ? inv.getStack(found) : new net.minecraft.item.ItemStack(want);
        if (found >= 0) {
            inv.setStack(found, inHand);
        }
        inv.setStack(sel, stack);
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_IRON.value(), SoundCategory.PLAYERS, 1.0F, 1.4F);
    }

    private static List<LivingEntity> cone(ServerPlayerEntity p, double range, double minDot) {
        ServerWorld w = Fx.world(p);
        Vec3d eye = p.getEyePos();
        Vec3d look = p.getRotationVec(1.0F);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range), x -> Aim.valid(p, x))) {
            Vec3d c = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
            Vec3d to = c.subtract(eye);
            double d = to.length();
            if (d < 1.0E-3 || d > range + e.getWidth() * 0.5) continue;
            if (to.multiply(1.0 / d).dotProduct(look) < minDot) continue;
            if (!p.canSee(e)) continue;
            out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
        return out;
    }

    private static void push(ServerPlayerEntity from, LivingEntity t, double h, double up) {
        Vec3d d = new Vec3d(t.getX() - from.getX(), 0.0, t.getZ() - from.getZ());
        if (d.lengthSquared() < 1.0E-4) {
            d = from.getRotationVec(1.0F);
            d = new Vec3d(d.x, 0.0, d.z);
        }
        d = d.normalize().multiply(h);
        t.setVelocity(t.getVelocity().add(d.x, up, d.z));
        t.velocityDirty = true;
    }

    private static void lunge(ServerPlayerEntity p, double speed) {
        Vec3d look = p.getRotationVec(1.0F);
        p.setVelocity(look.x * speed, Math.max(p.getVelocity().y, 0.05), look.z * speed);
        p.velocityDirty = true;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
    }

    private static boolean passable(ServerWorld w, Vec3d v) {
        BlockPos bp = BlockPos.ofFloored(v.x, v.y, v.z);
        return w.isPosLoaded(bp) && w.getBlockState(bp).getCollisionShape(w, bp).isEmpty();
    }

    /** Z: Небесная комбинация — четыре удара за 12 тиков, последний с отбросом. */
    public static boolean castCombo(ServerPlayerEntity p, SukunaState s) {
        final boolean cloudC = holdsCloud(p);
        final double reach = cloudC ? 4.7 : 3.8;
        final double mult = !holdsKatana(p) && !cloudC ? 1.25 : 1.0;
        if (cone(p, reach, 0.55).isEmpty()) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        s.busyUntil = w.getTime() + 14;
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.MAKI) return true;
                if (t % 4 == 0) {
                    boolean last = t == 12;
                    List<LivingEntity> hits = cone(p, reach, 0.55);
                    if (!hits.isEmpty() && t < 12 && hits.get(0).squaredDistanceTo(p) > 4.0) {
                        lunge(p, 0.35);
                    }
                    int n = 0;
                    for (LivingEntity e : hits) {
                        if (n++ >= 3) break;
                        Dmg.physical(p, e, (last ? 6.0 : 4.5) * mult * s.power());
                        Vec3d mid = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ());
                        Ink.impact(w, mid, p.getRotationVec(1.0F), last ? Ink.GOLD : Ink.WHITE);
                        if (last) {
                            push(p, e, 1.3, 0.3);
                        } else {
                            push(p, e, 0.15, 0.0);
                        }
                    }
                    p.swingHand(t % 8 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.3F + 0.1F * (t / 4));
                }
                t++;
                return t > 12;
            }
        });
        Players.markCombat(p);
        return true;
    }

    /** X: рывок и рассечение (нужна катана в руке). */
    public static boolean castDashSlash(ServerPlayerEntity p, SukunaState s) {
        if (needKatana(p)) return false;
        LivingEntity t = Aim.target(p, holdsCloud(p) ? 11.0 : 9.0);
        if (t == null || !p.canSee(t)) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        ServerWorld w = Fx.world(p);
        Vec3d start = new Vec3d(p.getX(), p.getY(), p.getZ());
        Vec3d tp = new Vec3d(t.getX(), t.getY(), t.getZ());
        Vec3d flat = new Vec3d(tp.x - start.x, 0.0, tp.z - start.z);
        double dist = flat.length();
        Vec3d dir = dist < 1.0E-3 ? p.getRotationVec(1.0F) : flat.multiply(1.0 / dist);
        final boolean cloudD = holdsCloud(p);
        double travel = Math.max(0.0, Math.min(cloudD ? 10.0 : 8.0, dist - 1.4));
        Vec3d end = start;
        for (double d = 0.5; d <= travel; d += 0.5) {
            Vec3d next = start.add(dir.multiply(d));
            if (!passable(w, next.add(0.0, 0.2, 0.0)) || !passable(w, next.add(0.0, 1.5, 0.0))) break;
            end = next;
        }
        p.networkHandler.requestTeleport(end.x, end.y, end.z, p.getYaw(), p.getPitch());
        Ink.stroke(w, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), Ink.WHITE, Ink.BLUE, 0.5F, true);
        Fx.sound(w, end.x, end.y, end.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.2F, 0.7F);
        if (end.squaredDistanceTo(tp) <= 3.5 * 3.5 && t.isAlive()) {
            Dmg.physical(p, t, (cloudD ? 12.0 : 14.0) * s.power());
            push(p, t, cloudD ? 1.4 : 0.9, 0.25);
            Ink.impact(w, new Vec3d(t.getX(), t.getY() + t.getHeight() * 0.6, t.getZ()), dir, Ink.WHITE);
        }
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 8;
        return true;
    }

    /** C: чувство проклятий — подсветка ближайших целей, скорость и уклонение на 8 с. */
    public static boolean castSense(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        if (s.makiSenseUntil > w.getTime()) {
            p.sendMessage(Text.translatable("sukuna.msg.already_active"), true);
            return false;
        }
        s.makiSenseUntil = w.getTime() + SENSE_TICKS;
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, SENSE_TICKS, 0, false, false));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, SENSE_TICKS, 0, false, false));
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), SENSE_RANGE * 0.2, Ink.WHITE, 0.5F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.9F);
        p.sendMessage(Text.translatable("sukuna.msg.maki_sense"), true);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.MAKI || s.makiSenseUntil <= w.getTime()) return true;
                if (t % 10 == 0) {
                    List<LivingEntity> near = new ArrayList<>(w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(SENSE_RANGE), x -> Aim.valid(p, x) && x.squaredDistanceTo(p) <= SENSE_RANGE * SENSE_RANGE));
                    near.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
                    int n = 0;
                    for (LivingEntity e : near) {
                        if (n++ >= 12) break;
                        e.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 25, 0, false, false));
                    }
                }
                t++;
                return false;
            }
        });
        return true;
    }

    /** V: Рассекающая душу катана — короткий замах и широкий удар: половина урона игнорирует броню. */
    public static boolean castSoulSplit(ServerPlayerEntity p, SukunaState s) {
        if (needKatana(p)) return false;
        final boolean cloudV = holdsCloud(p);
        ServerWorld w = Fx.world(p);
        s.busyUntil = w.getTime() + SOUL_WINDUP + 12;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 1.0F, 1.5F);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.MAKI) return true;
                t++;
                if (t < SOUL_WINDUP) return false;
                Players.broadcastNamedAnim(p, ANIM_SLASH);
                double total = (cloudV ? 17.0 : 20.0) * s.power();
                int n = 0;
                Vec3d look = p.getRotationVec(1.0F);
                for (LivingEntity e : cone(p, cloudV ? 5.0 : 4.2, cloudV ? -1.0 : 0.45)) {
                    if (n++ >= (cloudV ? 6 : 4)) break;
                    Dmg.physical(p, e, total * 0.5);
                    Dmg.hit(p, e, total * 0.5);
                    push(p, e, 1.1, 0.35);
                    Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ()), look, Ink.WHITE);
                }
                Vec3d c = p.getEyePos().add(look.multiply(1.6));
                Ink.stroke(w, c.add(0.0, 1.0, -1.2).add(look.crossProduct(new Vec3d(0, 1, 0)).multiply(1.5)), c.add(look.crossProduct(new Vec3d(0, 1, 0)).multiply(-1.5)).add(0.0, -0.8, 0.0), Ink.WHITE, Ink.BLUE, 0.8F, true);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.6F, 0.6F);
                p.swingHand(Hand.MAIN_HAND, true);
                return true;
            }
        });
        Players.markCombat(p);
        return true;
    }

    /** B: Абсолютный разрез — подготовка 18 тиков (замедление, прерывается уроном), затем удар по линии. */
    public static boolean castAbsoluteSlash(ServerPlayerEntity p, SukunaState s) {
        if (needKatana(p)) return false;
        ServerWorld w = Fx.world(p);
        final long start = w.getTime();
        final boolean cloudB = holdsCloud(p);
        s.busyUntil = start + ABSOLUTE_WINDUP + 14;
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 2.0F, 1.1F);
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || s.form != SukunaForm.MAKI) return true;
                if (s.lastDamagedAt > start) {
                    p.sendMessage(Text.translatable("sukuna.msg.maki_interrupted"), true);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.2F, 0.8F);
                    s.busyUntil = w.getTime();
                    return true;
                }
                t++;
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 1, false, false));
                if (t < ABSOLUTE_WINDUP) {
                    if (t % 4 == 0) {
                        Vec3d focus = p.getEyePos().add(p.getRotationVec(1.0F).multiply(1.8));
                        Ink.ring(w, focus, 1.2 - 0.05 * t, Ink.WHITE, 0.4F);
                    }
                    return false;
                }
                strike();
                return true;
            }

            private void strike() {
                Players.broadcastNamedAnim(p, ANIM_ULTIMATE);
                Vec3d eye = p.getEyePos();
                Vec3d dir = p.getRotationVec(1.0F);
                double len = cloudB ? 10.0 : ABSOLUTE_LENGTH;
                BlockHitResult br = w.raycast(new RaycastContext(eye, eye.add(dir.multiply(len)), RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
                len = Math.min(len, eye.distanceTo(br.getPos()));
                Vec3d end = eye.add(dir.multiply(len));
                double total = (cloudB ? 34.0 : 40.0) * s.power();
                for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().stretch(dir.multiply(len)).expand(2.0), x -> Aim.valid(p, x))) {
                    Vec3d c = new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
                    double along = c.subtract(eye).dotProduct(dir);
                    if (along < 0.0 || along > len + 0.5) continue;
                    double perp = c.subtract(eye.add(dir.multiply(along))).length();
                    if (perp > (cloudB ? 2.4 : 1.4) + e.getWidth() * 0.5) continue;
                    double dmg = total;
                    if (e instanceof PlayerEntity) {
                        dmg = Math.min(dmg, e.getMaxHealth() * 0.55);
                    }
                    Dmg.physical(p, e, dmg * 0.5);
                    Dmg.hit(p, e, dmg * 0.5);
                    e.setVelocity(dir.multiply(2.0).add(0.0, 0.4, 0.0));
                    e.velocityDirty = true;
                    Ink.impact(w, c, dir, Ink.WHITE);
                }
                Ink.stroke(w, eye.add(0.0, -0.4, 0.0).add(dir.multiply(1.0)), end, Ink.WHITE, Ink.BLUE, 1.4F, true);
                Ink.ring(w, end, 1.6, Ink.WHITE, 0.7F);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.6F, 1.5F);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 2.0F, 0.5F);
                p.swingHand(Hand.MAIN_HAND, true);
            }
        });
        Players.markCombat(p);
        return true;
    }
}
