package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * Комбо способностей: если вторую технику из пары применить в течение 1,5 с после первой,
 * она усиливается (×1,35 урона), вокруг игрока бьёт ударная волна, а перезарядка второй техники сокращается на 25%.
 * Пары: слот 1 + слот 3 (Z + C) и слот 2 + слот 4 (X + V), в любом порядке.
 */
public final class Combos {
    public static final int WINDOW_TICKS = 30;
    public static final double DAMAGE_MUL = 1.35;

    private Combos() {
    }

    public static int slot(Ability a) {
        return switch (a) {
            case PRIMARY_ATTACK -> 1;
            case SECONDARY_ATTACK -> 2;
            case UTILITY_TECHNIQUE -> 3;
            case SPECIAL_ATTACK -> 4;
            default -> 0;
        };
    }

    public static boolean isPair(int a, int b) {
        return a != 0 && b != 0 && a != b && ((a + b) == 4 || (a + b) == 6) && a % 2 == b % 2;
    }

    /** Будет ли это применение комбо. Вызывается до диспетчеризации. */
    public static boolean pending(SukunaState s, Ability a, long now) {
        return isPair(s.comboLastSlot, slot(a)) && now - s.comboLastAt <= WINDOW_TICKS;
    }

    /** Эффекты комбо после успешного каста второй техники (КД уже назначен). */
    public static void trigger(ServerPlayerEntity p, SukunaState s, Ability a, ServerWorld w, long now) {
        int idx = a.ordinal();
        long left = s.cooldowns[idx] - now;
        if (left > 0L) {
            s.cooldowns[idx] = now + left * 3L / 4L;
        }
        Vec3d c = new Vec3d(p.getX(), p.getY(), p.getZ());
        double dmg = 8.0 * s.power();
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(4.0), x -> Aim.valid(p, x) && x.squaredDistanceTo(p) <= 16.0)) {
            Dmg.hit(p, e, dmg);
            Vec3d d = new Vec3d(e.getX() - c.x, 0.0, e.getZ() - c.z);
            if (d.lengthSquared() > 1.0E-4) {
                e.setVelocity(d.normalize().multiply(0.9).add(0.0, 0.3, 0.0));
                e.velocityDirty = true;
            }
        }
        Ink.ring(w, c.add(0.0, 0.2, 0.0), 3.6, Ink.GOLD, 0.9F);
        Ink.ring(w, c.add(0.0, 1.0, 0.0), 2.4, Ink.WHITE, 0.7F);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.6F, 0.8F);
        p.sendMessage(Text.translatable("sukuna.msg.combo", slot(a) == 0 ? "?" : (s.comboLastSlot + "+" + slot(a))), true);
    }

    public static void remember(SukunaState s, Ability a, long now) {
        int sl = slot(a);
        if (sl != 0) {
            s.comboLastSlot = sl;
            s.comboLastAt = now;
        }
    }
}
