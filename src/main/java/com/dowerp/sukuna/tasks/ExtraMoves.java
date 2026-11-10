package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import com.dowerp.sukuna.entity.ModEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Дополнительные боевые техники (двойное нажатие слота Z/X/C/V): рывок, волна, взрыв, подтягивание, усиление,
 * серия ударов, прыжок и блинк. Параметры каждой формы заданы в таблице ниже.
 */
public final class ExtraMoves {
    public record Move(String type, double dmg, double range, int cost, double cd) {
    }

    private static final Map<SukunaForm, Move[]> TABLE = new EnumMap<>(SukunaForm.class);

    private static void put(SukunaForm f, Move... m) {
        TABLE.put(f, m);
    }

    static {
        put(SukunaForm.YUJI, new Move("DASH", 16D, 7D, 20, 5D), new Move("BURST", 12D, 4D, 30, 9D), new Move("WAVE", 18D, 10D, 35, 8D), new Move("BUFF", 0D, 8D, 40, 20D));
        put(SukunaForm.GOJO, new Move("PULL", 10D, 8D, 20, 6D), new Move("BURST", 16D, 5D, 35, 10D), new Move("WAVE", 22D, 14D, 45, 12D), new Move("BUFF", 0D, 8D, 30, 18D));
        put(SukunaForm.GOJO_AWAKENED, new Move("PULL", 16D, 10D, 25, 5D), new Move("BURST", 26D, 6D, 40, 8D), new Move("WAVE", 34D, 18D, 55, 10D), new Move("BUFF", 0D, 10D, 35, 15D));
        put(SukunaForm.CHOSO, new Move("DASH", 15D, 7D, 20, 5D), new Move("BARRAGE", 6D, 9D, 30, 9D), new Move("WAVE", 20D, 12D, 35, 8D), new Move("BUFF", 0D, 8D, 40, 20D));
        put(SukunaForm.CHOSO_WING, null, null, null, new Move("BURST", 28D, 6D, 60, 16D));
        put(SukunaForm.HEIAN, null, null, null, new Move("BARRAGE", 10D, 10D, 50, 12D));
        put(SukunaForm.TODO, new Move("BURST", 10D, 4D, 20, 6D), new Move("DASH", 20D, 8D, 30, 10D), new Move("BARRAGE", 7D, 5D, 35, 9D), new Move("BUFF", 0D, 8D, 40, 20D));
        put(SukunaForm.TODO_AWAKENED, new Move("BURST", 16D, 5D, 25, 5D), new Move("DASH", 30D, 10D, 35, 8D), new Move("BARRAGE", 10D, 5D, 40, 7D), new Move("BUFF", 0D, 10D, 45, 16D));
        put(SukunaForm.MAKI, new Move("DASH", 12D, 5D, 0, 5D), new Move("WAVE", 15D, 12D, 0, 8D), new Move("LEAP", 22D, 3.5D, 0, 12D), new Move("BUFF", 0D, 8D, 0, 25D));
        put(SukunaForm.MEGUMI, new Move("BUFF", 0D, 8D, 25, 16D), new Move("BURST", 18D, 5D, 35, 10D), new Move("PULL", 12D, 10D, 30, 9D), new Move("BLINK", 14D, 9D, 35, 12D));
        put(SukunaForm.MEGUMI_BEASTS, new Move("DASH", 18D, 8D, 25, 6D), new Move("BURST", 20D, 5D, 40, 10D), new Move("WAVE", 22D, 12D, 40, 9D), new Move("BUFF", 0D, 8D, 35, 18D));
        put(SukunaForm.YUTA, new Move("DASH", 16D, 7D, 20, 5D), new Move("BURST", 14D, 5D, 35, 10D), new Move("WAVE", 20D, 12D, 40, 9D), new Move("BUFF", 0D, 8D, 45, 22D));
        put(SukunaForm.YUTA_RIKA, new Move("DASH", 24D, 8D, 25, 5D), new Move("BURST", 22D, 6D, 40, 9D), new Move("WAVE", 32D, 14D, 45, 8D), new Move("BUFF", 0D, 10D, 50, 20D));
    }

    private ExtraMoves() {
    }

    public static boolean has(SukunaForm f, int n) {
        Move[] m = TABLE.get(f);
        return m != null && n >= 1 && n <= m.length && m[n - 1] != null;
    }

    public static int cost(SukunaForm f, int n) {
        return has(f, n) ? TABLE.get(f)[n - 1].cost : 0;
    }

    public static double cooldownSeconds(SukunaForm f, int n) {
        return has(f, n) ? TABLE.get(f)[n - 1].cd : 10.0;
    }

    public static Move get(SukunaForm f, int n) {
        return has(f, n) ? TABLE.get(f)[n - 1] : null;
    }

    private static Vec3d feet(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY(), e.getZ());
    }

    private static Vec3d center(LivingEntity e) {
        return new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.5, e.getZ());
    }

    private static Vec3d flat(Vec3d v) {
        Vec3d f = new Vec3d(v.x, 0.0, v.z);
        return f.lengthSquared() > 1.0E-4 ? f.normalize() : new Vec3d(0.0, 0.0, 1.0);
    }

    private static void knock(LivingEntity e, Vec3d dir, double h, double up) {
        e.setVelocity(dir.multiply(h).add(0.0, up, 0.0));
        e.velocityDirty = true;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s, int n) {
        Move mv = get(s.form, n);
        if (mv == null) {
            return false;
        }
        final ServerWorld w = Fx.world(p);
        final double dmg = mv.dmg * s.power();
        final Vec3d look = p.getRotationVec(1.0F);
        boolean ok;
        switch (mv.type) {
            case "DASH" -> ok = dash(p, s, w, mv, dmg);
            case "BLINK" -> ok = blink(p, s, w, mv, dmg);
            case "WAVE" -> ok = wave(p, s, w, mv, dmg, look);
            case "BURST" -> ok = burst(p, s, w, mv, dmg);
            case "PULL" -> ok = pull(p, s, w, mv, dmg);
            case "BARRAGE" -> ok = barrage(p, s, w, mv, dmg);
            case "LEAP" -> ok = leap(p, s, w, mv, dmg, look);
            default -> ok = buff(p, s, w);
        }
        if (ok) {
            Players.markCombat(p);
        }
        return ok;
    }

    private static boolean noTarget(ServerPlayerEntity p) {
        p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
        return false;
    }

    private static boolean dash(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg) {
        LivingEntity t = Aim.target(p, mv.range);
        if (t == null || !p.canSee(t)) {
            return noTarget(p);
        }
        Vec3d start = feet(p);
        Vec3d dir = flat(feet(t).subtract(start));
        double travel = Math.max(0.0, Math.min(mv.range, feet(t).subtract(start).horizontalLength() - 1.4));
        Vec3d end = start;
        for (double d = 0.5; d <= travel; d += 0.5) {
            Vec3d next = start.add(dir.multiply(d));
            if (!MakiTasks.passable(w, next.add(0.0, 0.2, 0.0)) || !MakiTasks.passable(w, next.add(0.0, 1.5, 0.0))) break;
            end = next;
        }
        p.networkHandler.requestTeleport(end.x, end.y, end.z, p.getYaw(), p.getPitch());
        Ink.stroke(w, start.add(0.0, 1.0, 0.0), end.add(0.0, 1.0, 0.0), Ink.WHITE, Ink.BLUE, 0.5F, true);
        if (end.squaredDistanceTo(feet(t)) <= 3.6 * 3.6 && t.isAlive()) {
            Dmg.hit(p, t, dmg);
            knock(t, dir, 0.9, 0.3);
            Ink.impact(w, center(t), dir, Ink.WHITE);
        }
        Fx.sound(w, end.x, end.y, end.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.3F, 0.8F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 8;
        return true;
    }

    private static boolean blink(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg) {
        LivingEntity t = Aim.target(p, mv.range);
        if (t == null) {
            return noTarget(p);
        }
        Vec3d back = feet(t).subtract(flat(p.getRotationVec(1.0F)).multiply(-1.5));
        Vec3d to = feet(t).add(flat(feet(t).subtract(feet(p))).multiply(1.5));
        if (!MakiTasks.passable(w, to.add(0.0, 0.2, 0.0)) || !MakiTasks.passable(w, to.add(0.0, 1.5, 0.0))) {
            to = back;
        }
        Ink.ring(w, feet(p).add(0.0, 1.0, 0.0), 1.2, Ink.PURPLE, 0.6F);
        p.networkHandler.requestTeleport(to.x, to.y, to.z, Fx3d.yawTo(to, feet(t)), p.getPitch());
        Dmg.hit(p, t, dmg);
        Ink.impact(w, center(t), flat(feet(t).subtract(to)), Ink.PURPLE);
        Fx.sound(w, to.x, to.y, to.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 1.4F);
        s.busyUntil = w.getTime() + 8;
        return true;
    }

    private static boolean wave(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg, Vec3d look) {
        Vec3d start = p.getEyePos().add(look.multiply(1.2)).add(0.0, -0.4, 0.0);
        final Set<UUID> hit = new HashSet<>();
        double speed = 2.0;
        int ticks = (int) Math.ceil(mv.range / speed);
        Ink.stroke(w, start, start.add(look.multiply(mv.range)), Ink.WHITE, Ink.BLUE, 0.9F, true);
        Fx3d.pulse(w, ModEntities.RELEASE_S, p, start, look, speed, ticks, 1.6, false, e -> {
            if (!hit.add(e.getUuid())) return;
            Dmg.hit(p, e, dmg);
            knock(e, look, 1.1, 0.35);
            Ink.impact(w, center(e), look, Ink.WHITE);
        });
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 1.2F, 1.5F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 10;
        return true;
    }

    private static boolean burst(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg) {
        Vec3d c = feet(p);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(mv.range), x -> Aim.valid(p, x) && x.squaredDistanceTo(p) <= mv.range * mv.range)) {
            Dmg.hit(p, e, dmg);
            knock(e, flat(feet(e).subtract(c)), 1.3, 0.45);
            Ink.impact(w, center(e), flat(feet(e).subtract(c)), Ink.WHITE);
        }
        Ink.ring(w, c.add(0.0, 0.2, 0.0), mv.range * 0.9, Ink.WHITE, 0.9F);
        Ink.ring(w, c.add(0.0, 0.8, 0.0), mv.range * 0.6, Ink.BLUE, 0.7F);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5F, 1.3F);
        p.swingHand(Hand.MAIN_HAND, true);
        s.busyUntil = w.getTime() + 10;
        return true;
    }

    private static boolean pull(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg) {
        List<LivingEntity> list = w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(mv.range), x -> Aim.valid(p, x) && x.squaredDistanceTo(p) <= mv.range * mv.range);
        if (list.isEmpty()) {
            return noTarget(p);
        }
        int n = 0;
        for (LivingEntity e : list) {
            if (n++ >= 6) break;
            Vec3d to = feet(p).add(flat(p.getRotationVec(1.0F)).multiply(1.8)).subtract(feet(e));
            Dmg.hit(p, e, dmg);
            if (to.lengthSquared() > 1.0E-4) {
                knock(e, to.normalize(), 1.2, 0.2);
            }
            e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, false, true));
            Ink.stroke(w, center(p), center(e), Ink.PURPLE, Ink.WHITE, 0.35F, false);
        }
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 1.2F, 0.6F);
        s.busyUntil = w.getTime() + 10;
        return true;
    }

    private static boolean buff(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 160, 1, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 160, 0, false, true));
        p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 160, 0, false, true));
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 1.6, Ink.GOLD, 0.8F);
        Ink.ring(w, new Vec3d(p.getX(), p.getY() + 1.0, p.getZ()), 1.2, Ink.WHITE, 0.6F);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.2F, 1.4F);
        return true;
    }

    private static boolean barrage(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg) {
        if (MakiTasks.cone(p, mv.range, 0.5).isEmpty()) {
            return noTarget(p);
        }
        s.busyUntil = w.getTime() + 16;
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved()) return true;
                if (t % 4 == 0) {
                    Vec3d look = p.getRotationVec(1.0F);
                    int n = 0;
                    for (LivingEntity e : MakiTasks.cone(p, mv.range, 0.5)) {
                        if (n++ >= 3) break;
                        Dmg.hit(p, e, dmg);
                        Ink.impact(w, center(e), look, Ink.WHITE);
                        knock(e, look, t == 12 ? 1.1 : 0.15, t == 12 ? 0.3 : 0.0);
                    }
                    p.swingHand(t % 8 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.2F + 0.1F * (t / 4));
                }
                t++;
                return t > 12;
            }
        });
        return true;
    }

    private static boolean leap(ServerPlayerEntity p, SukunaState s, ServerWorld w, Move mv, double dmg, Vec3d look) {
        Vec3d dir = flat(look);
        p.setVelocity(dir.x * 1.3, 0.75, dir.z * 1.3);
        p.velocityDirty = true;
        p.networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(p));
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.2F, 0.8F);
        s.busyUntil = w.getTime() + 30;
        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved()) return true;
                t++;
                if ((t > 6 && p.isOnGround()) || t > 40) {
                    Vec3d c = feet(p);
                    for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(mv.range), x -> Aim.valid(p, x))) {
                        Dmg.hit(p, e, dmg);
                        knock(e, flat(feet(e).subtract(c)), 1.2, 0.5);
                    }
                    Ink.ring(w, c.add(0.0, 0.2, 0.0), mv.range * 1.1, Ink.WHITE, 0.9F);
                    Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.4F, 1.1F);
                    return true;
                }
                return false;
            }
        });
        return true;
    }
}
