package com.dowerp.sukuna;

import com.dowerp.sukuna.entity.MahoragaEntity;
import com.dowerp.sukuna.tasks.ChosoTasks;
import com.dowerp.sukuna.tasks.BlackFlash;
import com.dowerp.sukuna.tasks.ExtraMoves;
import com.dowerp.sukuna.tasks.GojoTasks;
import com.dowerp.sukuna.tasks.HeianTasks;
import com.dowerp.sukuna.tasks.MakiTasks;
import com.dowerp.sukuna.tasks.MegumiTasks;
import com.dowerp.sukuna.tasks.YutaTasks;
import com.dowerp.sukuna.tasks.SukunaTasks;
import com.dowerp.sukuna.tasks.TodoTasks;
import com.dowerp.sukuna.tasks.YujiTasks;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.ArrayList;

public final class Abilities {
    private Abilities() {}

    public static void handle(ServerPlayerEntity p, int index) {
        Ability[] all = Ability.values();
        if (index < 0 || index >= all.length) return;
        Ability a = all[index];
        SukunaState s = Players.get(p);
        s.fingers = p.getAttachedOrElse(Players.FINGERS, 1);

        if (!p.isAlive() || p.isSpectator()) return;

        s.character = Players.character(p);
        if (!s.character.playable) {
            p.sendMessage(Text.translatable("sukuna.msg.char_locked", s.character.displayName()), true);
            return;
        }

        ServerWorld w = Fx.world(p);
        long now = w.getTime();

        if (a == Ability.TRANSFORM) {
            handleTransform(p, s, w);
            return;
        }

        if (BlackFlash.tryPress(p, s, now)) {
            return;
        }

        if (!a.availableIn(s.form)) {
            p.sendMessage(Text.translatable("sukuna.msg.not_available"), true);
            return;
        }

        if (a == Ability.INFINITY_TOGGLE) {
            s.infinityToggledOff = !s.infinityToggledOff;
            p.sendMessage(Text.translatable(s.infinityToggledOff ? "sukuna.msg.infinity_disabled" : "sukuna.msg.infinity_enabled"), true);
            return;
        }

        if (now < s.cooldowns[index]) {
            long seconds = (s.cooldowns[index] - now + 19L) / 20L;
            p.sendMessage(Text.translatable("sukuna.msg.cooldown", seconds), true);
            return;
        }

        if (a != Ability.WHEEL_ADAPTATION && a != Ability.REVERSE_CURSED && !a.isCounter() && now < s.busyUntil) {
            p.sendMessage(Text.translatable("sukuna.msg.busy"), true);
            return;
        }

        boolean realDomain = a == Ability.DOMAIN_EXPANSION && !s.form.isChoso();
        boolean meterDomain = realDomain && s.meterOn();
        if (realDomain) {
            if (s.sealing) {
                p.sendMessage(Text.translatable("sukuna.msg.busy"), true);
                return;
            }
            if (meterDomain && !s.meterFull()) {
                p.sendMessage(Text.translatable("sukuna.msg.domain_meter_required"), true);
                return;
            }
        }
        boolean openDomain = s.openDomainReady();
        if (!SukunaMod.config.freeCursedEnergy && !meterDomain && s.ce < a.costFor(s.form, openDomain)) {
            p.sendMessage(Text.translatable("sukuna.msg.no_energy"), true);
            return;
        }

        s.deferCharge = false;
        boolean comboNow = Combos.pending(s, a, now);
        s.comboMul = comboNow ? Combos.DAMAGE_MUL : 1.0;
        boolean heatable = SukunaMod.config.techniqueHeat && (a == Ability.PRIMARY_ATTACK || a == Ability.SECONDARY_ATTACK
            || a == Ability.UTILITY_TECHNIQUE || a == Ability.SPECIAL_ATTACK);
        double prevHeatMul = s.heatMul;
        int nextHeatStacks = 0;
        if (heatable) {
            nextHeatStacks = s.heatAbility == a.ordinal() && now <= s.heatUntil ? s.heatStacks + 1 : 0;
            s.heatMul = Math.max(0.4, 1.0 - 0.12 * nextHeatStacks);
        } else {
            s.heatMul = 1.0;
        }
        boolean ok = a.isCounter() ? dispatchCounter(a, p, s) : a.isExtra() ? ExtraMoves.cast(p, s, a.extraIndex()) : a.isRika() ? YutaTasks.castRikaAbility(a, p, s) : switch (s.form) {
            case YUJI -> dispatchYuji(a, p, s, w, now);
            case SUKUNA -> dispatchSukuna(a, p, s, w, now);
            case HEIAN -> dispatchHeian(a, p, s, w, now);
            case GOJO, GOJO_AWAKENED -> dispatchGojo(a, p, s, w);
            case CHOSO, CHOSO_WING -> dispatchChoso(a, p, s, w);
            case TODO, TODO_AWAKENED -> dispatchTodo(a, p, s, w);
            case MAKI -> dispatchMaki(a, p, s);
            case MEGUMI, MEGUMI_BEASTS -> dispatchMegumi(a, p, s);
            case YUTA, YUTA_RIKA -> dispatchYuta(a, p, s, w);
        };

        s.comboMul = 1.0;
        if (heatable) {
            if (ok) {
                s.heatAbility = a.ordinal();
                s.heatStacks = nextHeatStacks;
                s.heatUntil = now + SukunaMod.config.heatWindowTicks;
                if (nextHeatStacks > 0) {
                    p.sendMessage(Text.translatable("sukuna.msg.heat", Math.round((1.0 - s.heatMul) * 100.0)), true);
                }
            } else {
                s.heatMul = prevHeatMul;
            }
        }

        if (ok && s.deferCharge) {
            s.deferCharge = false;
            AbilitySounds.play(w, p, s.form, a);
            Players.broadcastAnim(p, a, s.form);
            return;
        }
        if (ok) {
            AbilitySounds.play(w, p, s.form, a);
            if (meterDomain) {
                s.domainMeter = Math.max(0.0, s.domainMeter - SukunaState.METER_MAX);
            } else {
                s.ce = Math.max(0.0, s.ce - a.costFor(s.form, openDomain));
            }
            s.cooldowns[index] = now + a.cooldownFor(s.form, openDomain);
            if (s.noCooldown) {
                s.cooldowns[index] = 0L;
            }
            Players.broadcastAnim(p, a, s.form);
            if (a == Ability.ULTIMATE_SLASH && !s.form.isGojo() && !s.form.isTodo()) {
                for (int i = 0; i < s.cooldowns.length; i++) {
                    if (i != index) s.cooldowns[i] = 0L;
                }
                p.sendMessage(Text.translatable("sukuna.msg.ultimate_refresh"), true);
            }
            if (a != Ability.WHEEL_ADAPTATION && a != Ability.REVERSE_CURSED) {
                s.busyUntil = Math.max(s.busyUntil, now + 18L);
            }
            if (comboNow) {
                Combos.trigger(p, s, a, w, now);
            }
            Combos.remember(s, a, now);
        }
    }

    private static boolean dispatchGojo(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        boolean max = s.form == SukunaForm.GOJO_AWAKENED;
        if (!a.availableIn(s.form)) {
            p.sendMessage(Text.translatable("sukuna.msg.gojo_no_technique"), true);
            return false;
        }
        return switch (a) {
            case PRIMARY_ATTACK -> GojoTasks.castBlue(p, s, max);
            case SECONDARY_ATTACK -> GojoTasks.castRed(p, s, max);
            case UTILITY_TECHNIQUE -> GojoTasks.castCombo(p, s);
            case SPECIAL_ATTACK -> GojoTasks.castStepKick(p, s);
            case ULTIMATE_SLASH -> GojoTasks.castPurple(p, s);
            case DOMAIN_EXPANSION -> max ? deferDomain(p, s, false, () -> VoidDomainTask.cast(p, s)) : GojoTasks.castInstantDomain(p, s);
            case REVERSE_CURSED -> reverse(p, s, w, max ? 30.0F : 16.0F);
            default -> {
                p.sendMessage(Text.translatable("sukuna.msg.gojo_no_technique"), true);
                yield false;
            }
        };
    }

    private static boolean dispatchCounter(Ability a, ServerPlayerEntity p, SukunaState s) {
        return a == Ability.SIMPLE_DOMAIN ? Counters.castSimpleDomain(p, s) : Counters.castFallingBlossom(p, s);
    }

    private static boolean domainActive(ServerPlayerEntity p, SukunaState s) {
        return DomainTask.hasActive(p) || VoidDomainTask.isActive(p) || s.domainLeft > 0;
    }

    /** Территория с печатями: проверка заранее, оплата и КД только после реального создания. */
    private static boolean deferDomain(ServerPlayerEntity p, SukunaState s, boolean open, java.util.function.BooleanSupplier cast) {
        if (domainActive(p, s)) {
            p.sendMessage(Text.translatable("sukuna.msg.domain_active"), true);
            return false;
        }
        if ((s.form == SukunaForm.SUKUNA || s.form == SukunaForm.HEIAN) && !p.isOnGround()) {
            p.sendMessage(Text.translatable("sukuna.msg.need_ground"), true);
            return false;
        }
        boolean started = Seals.thenCast(p, s, cast, () -> commitDomain(p, s, open));
        if (started) {
            s.deferCharge = true;
        }
        return started;
    }

    private static void commitDomain(ServerPlayerEntity p, SukunaState s, boolean open) {
        long now = Fx.world(p).getTime();
        Ability a = Ability.DOMAIN_EXPANSION;
        if (s.meterOn()) {
            s.domainMeter = 0.0;
        } else {
            s.ce = Math.max(0.0, s.ce - a.costFor(s.form, open));
        }
        s.cooldowns[a.ordinal()] = s.noCooldown ? 0L : now + a.cooldownFor(s.form, open);
    }

    private static boolean dispatchMegumi(Ability a, ServerPlayerEntity p, SukunaState s) {
        if (s.form == SukunaForm.MEGUMI_BEASTS) {
            return switch (a) {
                case PRIMARY_ATTACK -> MegumiTasks.castSerpent(p, s);
                case SECONDARY_ATTACK -> MegumiTasks.castOx(p, s);
                case UTILITY_TECHNIQUE -> MegumiTasks.castDeer(p, s);
                case SPECIAL_ATTACK -> MegumiTasks.castTiger(p, s, false);
                case ULTIMATE_SLASH -> MegumiTasks.castTiger(p, s, true);
                default -> false;
            };
        }
        return switch (a) {
            case PRIMARY_ATTACK -> MegumiTasks.castDivineDogs(p, s);
            case SECONDARY_ATTACK -> MegumiTasks.castNue(p, s);
            case UTILITY_TECHNIQUE -> MegumiTasks.castToad(p, s);
            case SPECIAL_ATTACK -> MegumiTasks.castRabbits(p, s);
            case ULTIMATE_SLASH -> MegumiTasks.castElephant(p, s);
            default -> false;
        };
    }

    private static boolean dispatchYuta(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        return switch (a) {
            case REVERSE_CURSED -> reverse(p, s, w, 14.0F);
            case PRIMARY_ATTACK -> YutaTasks.castSlash(p, s);
            case SECONDARY_ATTACK -> YutaTasks.castSpeech(p, s);
            case UTILITY_TECHNIQUE -> YutaTasks.castStep(p, s);
            case SPECIAL_ATTACK -> YutaTasks.castBoost(p, s);
            case ULTIMATE_SLASH -> s.form == SukunaForm.YUTA_RIKA && YutaTasks.castRelease(p, s);
            case DOMAIN_EXPANSION -> s.form == SukunaForm.YUTA_RIKA && deferDomain(p, s, false, () -> YutaTasks.castDomain(p, s));
            default -> false;
        };
    }

    private static boolean dispatchMaki(Ability a, ServerPlayerEntity p, SukunaState s) {
        return switch (a) {
            case PRIMARY_ATTACK -> MakiTasks.castCombo(p, s);
            case SECONDARY_ATTACK -> MakiTasks.castDashSlash(p, s);
            case UTILITY_TECHNIQUE -> MakiTasks.castSense(p, s);
            case SPECIAL_ATTACK -> MakiTasks.castSoulSplit(p, s);
            case ULTIMATE_SLASH -> MakiTasks.castAbsoluteSlash(p, s);
            default -> false;
        };
    }

    private static boolean dispatchTodo(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        if (!a.availableIn(s.form)) {
            p.sendMessage(Text.translatable("sukuna.msg.todo_no_technique"), true);
            return false;
        }
        return switch (a) {
            case PRIMARY_ATTACK -> TodoTasks.castBoogieWoogie(p, s);
            case SECONDARY_ATTACK -> TodoTasks.castDashStrike(p, s);
            case UTILITY_TECHNIQUE -> TodoTasks.castClapDash(p, s);
            case SPECIAL_ATTACK -> TodoTasks.castBoost(p, s);
            case ULTIMATE_SLASH -> TodoTasks.castMaximum(p, s);
            case REVERSE_CURSED -> reverse(p, s, w, 14.0F);
            default -> {
                p.sendMessage(Text.translatable("sukuna.msg.todo_no_technique"), true);
                yield false;
            }
        };
    }

    private static boolean dispatchChoso(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        boolean blood = switch (a) {
            case PRIMARY_ATTACK, SECONDARY_ATTACK, UTILITY_TECHNIQUE, ULTIMATE_SLASH, OPEN_DOMAIN, DISMANTLE_STORM, BLACK_FLASH -> true;
            default -> false;
        };
        boolean boosted = blood && ChosoTasks.convergenceReady(s, w.getTime());
        if (boosted) {
            s.convergenceMul = 1.5;
        }
        boolean ok;
        try {
            ok = chosoInner(a, p, s, w);
        } finally {
            s.convergenceMul = 1.0;
        }
        if (boosted && ok) {
            ChosoTasks.consumeConvergence(p, s);
        }
        return ok;
    }

    private static boolean chosoInner(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        boolean wing = s.form == SukunaForm.CHOSO_WING;
        if (!a.availableIn(s.form)) {
            p.sendMessage(Text.translatable("sukuna.msg.choso_no_technique"), true);
            return false;
        }
        return switch (a) {
            case PRIMARY_ATTACK -> ChosoTasks.castPiercing(p, s, wing);
            case SECONDARY_ATTACK -> ChosoTasks.castMeteorite(p, s, wing);
            case UTILITY_TECHNIQUE -> ChosoTasks.castBloodEdge(p, s);
            case SPECIAL_ATTACK -> ChosoTasks.toggleScale(p, s, wing);
            case DOMAIN_EXPANSION -> ChosoTasks.toggleHardening(p, s);
            case REVERSE_CURSED -> ChosoTasks.castCoagulation(p, s, wing);
            case ULTIMATE_SLASH -> ChosoTasks.castSupernova(p, s);
            case OPEN_DOMAIN -> ChosoTasks.castSlicingExorcism(p, s);
            case DISMANTLE_STORM -> ChosoTasks.castWingBarrage(p, s);
            case BLACK_FLASH -> ChosoTasks.castBloodTorrent(p, s);
            default -> {
                p.sendMessage(Text.translatable("sukuna.msg.choso_no_technique"), true);
                yield false;
            }
        };
    }

    private static boolean dispatchYuji(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w, long now) {
        return switch (a) {
            case PRIMARY_ATTACK -> YujiTasks.castDivergentFist(p, s);
            case SECONDARY_ATTACK -> YujiTasks.castCursedKick(p, s);
            case UTILITY_TECHNIQUE -> YujiTasks.triggerFocusBlackFlash(p, s);
            case SPECIAL_ATTACK -> YujiTasks.castReinforcement(p, s);
            case ULTIMATE_SLASH -> YujiTasks.castBlackFlashBarrage(p, s);
            case OPEN_DOMAIN, DISMANTLE_STORM, BLACK_FLASH, FOUR_ARM_FLURRY -> {
                p.sendMessage(Text.translatable("sukuna.msg.not_in_yuji"), true);
                yield false;
            }
            case DOMAIN_EXPANSION -> deferDomain(p, s, false, () -> YujiTasks.castYujiDomain(p, s));
            case WHEEL_ADAPTATION, SUMMON_MAHORAGA -> {
                p.sendMessage(Text.translatable("sukuna.msg.no_shadows_yuji"), true);
                yield false;
            }
            case REVERSE_CURSED -> reverse(p, s, w, 10.0F);
            default -> false;
        };
    }

    private static boolean dispatchSukuna(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w, long now) {
        return switch (a) {
            case PRIMARY_ATTACK -> DismantleTask.cast(p, s);
            case SECONDARY_ATTACK -> CleaveTask.cast(p, s);
            case UTILITY_TECHNIQUE -> WebTask.cast(p, s);
            case SPECIAL_ATTACK -> FugaTask.cast(p, s);
            case ULTIMATE_SLASH -> WorldSlashTask.cast(p, s, false);
            case DOMAIN_EXPANSION -> {
                boolean open = s.openDomainReady();
                yield deferDomain(p, s, open, () -> DomainTask.cast(p, s, open));
            }
            case OPEN_DOMAIN -> {
                p.sendMessage(Text.translatable("sukuna.msg.open_replaces_closed"), true);
                yield false;
            }
            case DISMANTLE_STORM -> SukunaTasks.dismantleStorm(p, s);
            case BLACK_FLASH -> SukunaTasks.blackFlash(p, s);
            case FOUR_ARM_FLURRY -> {
                p.sendMessage(Text.translatable("sukuna.msg.heian_only"), true);
                yield false;
            }
            case WHEEL_ADAPTATION -> toggleWheel(p, s, w);
            case SUMMON_MAHORAGA -> MahoragaEntity.summon(p, s);
            case REVERSE_CURSED -> reverse(p, s, w, 16.0F);
            default -> false;
        };
    }

    private static boolean dispatchHeian(Ability a, ServerPlayerEntity p, SukunaState s, ServerWorld w, long now) {
        return switch (a) {
            case PRIMARY_ATTACK -> HeianTasks.castKamaitachi(p, s);
            case SECONDARY_ATTACK -> HeianTasks.castKamutoke(p, s);
            case UTILITY_TECHNIQUE -> WebTask.cast(p, s);
            case SPECIAL_ATTACK -> FugaTask.cast(p, s);
            case ULTIMATE_SLASH -> HeianTasks.castTrueWorldSlash(p, s);
            case DOMAIN_EXPANSION -> deferDomain(p, s, true, () -> DomainTask.cast(p, s, true));
            case OPEN_DOMAIN -> {
                p.sendMessage(Text.translatable("sukuna.msg.heian_domain_open"), true);
                yield false;
            }
            case DISMANTLE_STORM -> SukunaTasks.dismantleStorm(p, s);
            case BLACK_FLASH -> SukunaTasks.blackFlash(p, s);
            case FOUR_ARM_FLURRY -> HeianTasks.castFourArmFlurry(p, s);
            case WHEEL_ADAPTATION, SUMMON_MAHORAGA -> {
                p.sendMessage(Text.translatable("sukuna.msg.heian_no_mahoraga"), true);
                yield false;
            }
            case REVERSE_CURSED -> reverse(p, s, w, 28.0F);
            default -> false;
        };
    }

    private static void handleTransform(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        long now = w.getTime();
        if (s.form == SukunaForm.MAKI) {
            p.sendMessage(Text.translatable("sukuna.msg.no_transform"), true);
            return;
        }
        if (s.form.isMegumi()) {
            MegumiTasks.transform(p, s, w);
            return;
        }
        if (s.form.isYuta()) {
            YutaTasks.transform(p, s, w);
            return;
        }
        if (s.form.isGojo()) {
            GojoTasks.transform(p, s, w);
            return;
        }
        if (s.form.isChoso()) {
            ChosoTasks.transform(p, s, w);
            return;
        }
        if (s.form.isTodo()) {
            TodoTasks.transform(p, s, w);
            return;
        }
        if (s.form == SukunaForm.YUJI) {
            s.form = SukunaForm.SUKUNA;
            p.sendMessage(Text.translatable("sukuna.msg.switched_sukuna"), false);
            AnimeVfx.playSukunaAwakening(w, p);
        } else if (s.form == SukunaForm.SUKUNA) {
            if (s.hostileKills >= SukunaState.HEIAN_KILLS) {
                s.form = SukunaForm.HEIAN;
                s.formUntil = now + SukunaState.HEIAN_DURATION_TICKS;
                s.wheel = false;
                p.sendMessage(Text.translatable("sukuna.msg.switched_heian"), false);
                AnimeVfx.playHeianIncarnation(w, p);
            } else {
                s.form = SukunaForm.YUJI;
                s.wheel = false;
                p.sendMessage(Text.translatable("sukuna.msg.switched_yuji"), false);
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
            }
        } else {
            Players.endHeian(p, s, true);
            return;
        }
        Players.applyFormAttributes(p, s.form);
    }

    private static boolean toggleWheel(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        s.wheel = !s.wheel;
        if (s.wheel) {
            s.wheelAngle = 0.0F;
            s.wheelTarget = 0.0F;
            s.adaptation.reset();
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.2F, 1.3F);
            AnimeVfx.ringShockwave(w, p.getEyePos().add(0, 0.8, 0), 1.2, Fx.GOLD);
            p.sendMessage(Text.translatable("sukuna.msg.wheel_on"), true);
        } else {
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 1.0F, 1.2F);
            p.sendMessage(Text.translatable("sukuna.msg.wheel_off"), true);
        }
        return true;
    }

    private static boolean reverse(ServerPlayerEntity p, SukunaState s, ServerWorld w, float baseHeal) {
        for (StatusEffectInstance e : new ArrayList<>(p.getStatusEffects())) {
            if (e.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) {
                p.removeStatusEffect(e.getEffectType());
            }
        }
        float hpScale = (float) (s.form.targetHp / 60.0);
        p.heal((baseHeal + (float) (s.statFingers() * 0.8)) * hpScale);
        Fx.far(w, ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.4, 0.6, 0.4, 0.02);
        Fx.far(w, ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 20, 0.5, 0.8, 0.5, 0.05);
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8F, 0.7F);
        return true;
    }
}
