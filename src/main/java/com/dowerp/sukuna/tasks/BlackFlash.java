package com.dowerp.sukuna.tasks;

import com.dowerp.sukuna.*;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

/**
 * Чёрная вспышка как серия. После подготовки звучит ритм из трёх сигналов (такты 2, 7, 12), удар приходится на третий.
 * Нажатие (любая клавиша техники или ЛКМ) в окне 0,1 с вокруг удара даёт настоящую вспышку: бонус урона,
 * серию и «зону» на 10 с. Промах даёт слабую вспышку и сбрасывает серию.
 */
public final class BlackFlash {
    public static final int CUE_TICKS = 12;
    public static final int ZONE_TICKS = 200;

    private BlackFlash() {
    }

    public static boolean isOpen(SukunaState s, long now) {
        return s.bfStrikeAt > 0L && now <= s.bfStrikeAt + SukunaMod.config.blackFlashLatencyTicks;
    }

    /** Принимает нажатие игрока. Засчитывается только первое нажатие за окно, спам не помогает. */
    public static boolean tryPress(ServerPlayerEntity p, SukunaState s, long now) {
        if (!isOpen(s, now)) return false;
        if (s.bfPress == SukunaState.NO_PRESS) {
            s.bfPress = now;
        }
        return true;
    }

    public static void open(ServerPlayerEntity p, SukunaState s, LivingEntity target, double base, ServerWorld w, Ability source) {
        if (s.bfStrikeAt > 0L) return;
        final long start = w.getTime();
        s.bfStrikeAt = start + CUE_TICKS;
        s.bfPress = SukunaState.NO_PRESS;
        s.bfSource = source.ordinal();
        s.busyUntil = Math.max(s.busyUntil, s.bfStrikeAt + SukunaMod.config.blackFlashLatencyTicks + 4);
        p.sendMessage(Text.translatable("sukuna.msg.bf_cue"), true);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive() || p.isRemoved() || !target.isAlive() || target.isRemoved() || target.getEntityWorld() != w) {
                    s.bfStrikeAt = 0L;
                    s.bfPress = SukunaState.NO_PRESS;
                    return true;
                }
                t++;
                long cur = w.getTime();
                Vec3d at = target.getEyePos();
                if (t == 2 || t == 7 || t == CUE_TICKS) {
                    float pitch = t == 2 ? 1.0F : t == 7 ? 1.35F : 2.0F;
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.6F, pitch);
                    Ink.dot(w, at, Ink.RED, t == CUE_TICKS ? 1.6F : 0.8F);
                } else if (t % 3 == 0) {
                    Ink.bolt(w, at.add(0.0, 0.5, 0.0), at, Ink.BLACK, Ink.RED, 0.3F);
                }
                if (cur >= s.bfStrikeAt + SukunaMod.config.blackFlashLatencyTicks) {
                    resolve(p, s, target, base, w, cur);
                    return true;
                }
                return false;
            }
        });
    }

    private static void resolve(ServerPlayerEntity p, SukunaState s, LivingEntity target, double base, ServerWorld w, long cur) {
        int win = SukunaMod.config.blackFlashWindowTicks;
        int lat = SukunaMod.config.blackFlashLatencyTicks;
        boolean perfect = s.bfPress >= s.bfStrikeAt - win && s.bfPress <= s.bfStrikeAt + lat;
        boolean inZone = s.bfZoneUntil > cur;
        Vec3d eye = target.getEyePos();
        Vec3d push = new Vec3d(target.getX() - p.getX(), 0.0, target.getZ() - p.getZ());

        if (perfect) {
            s.bfStreak++;
            s.bfZoneUntil = cur + ZONE_TICKS;
            double mul = 1.5 + 0.25 * Math.min(s.bfStreak - 1, 4);
            AnimeVfx.blackFlash(w, eye);
            Dmg.hit(p, target, base * mul);
            if (push.lengthSquared() > 1.0E-4) {
                target.setVelocity(push.normalize().multiply(1.7).add(0.0, 0.4, 0.0));
                target.velocityDirty = true;
            }
            p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 140, 1, false, true));
            if (s.form == SukunaForm.YUJI) {
                p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 140, 1, false, true));
                s.ce = Math.min(s.maxCe(), s.ce + 25.0);
            }
            if (inZone) {
                refund(s, Ability.BLACK_FLASH, cur);
                refund(s, Ability.UTILITY_TECHNIQUE, cur);
            }
            p.sendMessage(Text.translatable(inZone ? "sukuna.msg.bf_zone" : "sukuna.msg.bf_perfect", s.bfStreak), true);
        } else {
            Ink.impact(w, eye, push.lengthSquared() > 1.0E-4 ? push.normalize() : new Vec3d(1, 0, 0), Ink.RED);
            Dmg.hit(p, target, base * 0.7);
            if (s.bfStreak > 0 || inZone) {
                p.sendMessage(Text.translatable("sukuna.msg.bf_lost"), true);
            } else {
                p.sendMessage(Text.translatable("sukuna.msg.bf_basic"), true);
            }
            s.bfStreak = 0;
            s.bfZoneUntil = 0L;
        }
        s.focusUntil = 0L;
        s.bfStrikeAt = 0L;
        s.bfPress = SukunaState.NO_PRESS;
        s.busyUntil = cur;
        p.swingHand(Hand.MAIN_HAND, true);
    }

    private static void refund(SukunaState s, Ability a, long now) {
        long left = s.cooldowns[a.ordinal()] - now;
        if (left > 0L) {
            s.cooldowns[a.ordinal()] = now + left / 4L;
        }
    }

    /** Тик зоны: аура и завершение. */
    public static void zoneTick(ServerPlayerEntity p, SukunaState s, ServerWorld w, long now) {
        if (s.bfZoneUntil <= 0L) return;
        if (now > s.bfZoneUntil) {
            s.bfZoneUntil = 0L;
            s.bfStreak = 0;
            p.sendMessage(Text.translatable("sukuna.msg.bf_zone_end"), true);
            return;
        }
        if (p.age % 10 == 0) {
            Vec3d hand = p.getEyePos().add(p.getRotationVec(1.0F).multiply(0.8)).add(0.0, -0.4, 0.0);
            Ink.dot(w, hand, Ink.RED, 0.7F);
            Ink.bolt(w, hand, hand.add(0.3, 0.3, 0.0), Ink.BLACK, Ink.RED, 0.25F);
        }
    }
}
