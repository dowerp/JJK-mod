package com.dowerp.sukuna.client;

import com.dowerp.sukuna.Ability;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.SukunaMod;
import com.zigythebird.playeranim.animation.PlayerAnimationController;
import com.zigythebird.playeranim.api.PlayerAnimationAccess;
import com.zigythebird.playeranim.api.PlayerAnimationFactory;
import com.zigythebird.playeranimcore.animation.layered.IAnimation;
import com.zigythebird.playeranimcore.enums.PlayState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.util.Identifier;

public final class CastAnimations {
    private static final Identifier LAYER = SukunaMod.id("cast");

    private CastAnimations() {
    }

    public static void init() {
        PlayerAnimationFactory.ANIMATION_DATA_FACTORY.registerFactory(LAYER, 42,
            avatar -> new PlayerAnimationController(avatar, (controller, data, setter) -> PlayState.STOP));
    }

    public static void play(Ability a, SukunaForm form) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        playOn(client.player, a, form);
    }

    public static void playOn(AbstractClientPlayerEntity player, Ability a, SukunaForm form) {
        if (form == SukunaForm.HEIAN && a != Ability.TRANSFORM) {
            RemoteFx.markCast(player.getId());
        }
        String name = animationFor(a, form);
        if (name == null) return;
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
        if (layer instanceof PlayerAnimationController controller) {
            controller.triggerAnimation(SukunaMod.id(name));
        }
    }

    private static final String[] NAMED = {"cast_runstart", "cast_holdhead", "cast_punch", "cast_kick", "cast_slash"};

    public static void playNamed(AbstractClientPlayerEntity player, int code) {
        if (code < 0 || code >= NAMED.length) return;
        IAnimation layer = PlayerAnimationAccess.getPlayerAnimationLayer(player, LAYER);
        if (layer instanceof PlayerAnimationController controller) {
            controller.triggerAnimation(SukunaMod.id(NAMED[code]));
        }
    }

    private static String animationFor(Ability a, SukunaForm form) {
        if (form.isGojo()) {
            return switch (a) {
                case PRIMARY_ATTACK -> "cast_point";
                case SECONDARY_ATTACK -> "cast_red_side";
                case UTILITY_TECHNIQUE -> "cast_punch";
                case SPECIAL_ATTACK -> "cast_kick";
                case DOMAIN_EXPANSION -> form == SukunaForm.GOJO_AWAKENED ? "cast_seal_gojo" : "cast_clap";
                case ULTIMATE_SLASH -> "cast_purple";
                case REVERSE_CURSED -> "cast_charge";
                default -> null;
            };
        }
        if (form.isTodo()) {
            return switch (a) {
                case PRIMARY_ATTACK, UTILITY_TECHNIQUE, ULTIMATE_SLASH -> "cast_clap";
                case SECONDARY_ATTACK -> "cast_punch";
                case SPECIAL_ATTACK, REVERSE_CURSED -> "cast_charge";
                default -> null;
            };
        }
        if (form.isChoso()) {
            return switch (a) {
                case PRIMARY_ATTACK, SECONDARY_ATTACK, BLACK_FLASH, ULTIMATE_SLASH, OPEN_DOMAIN, DISMANTLE_STORM -> "cast_palms";
                case UTILITY_TECHNIQUE -> "cast_slash";
                case SPECIAL_ATTACK, DOMAIN_EXPANSION, REVERSE_CURSED -> "cast_charge";
                default -> null;
            };
        }
        if (form == SukunaForm.YUJI) {
            return switch (a) {
                case PRIMARY_ATTACK -> "cast_punch";
                case SECONDARY_ATTACK -> "cast_kick";
                case DOMAIN_EXPANSION -> "cast_seal_yuji";
                case UTILITY_TECHNIQUE -> "cast_uppercut";
                case SPECIAL_ATTACK, REVERSE_CURSED -> "cast_charge";
                default -> null;
            };
        }
        return switch (a) {
            case PRIMARY_ATTACK -> "cast_slash";
            case SECONDARY_ATTACK -> form == SukunaForm.HEIAN ? "cast_weapon" : "cast_slash";
            case DISMANTLE_STORM -> "cast_spin";
            case UTILITY_TECHNIQUE -> "cast_thrust";
            case SPECIAL_ATTACK -> "cast_point";
            case DOMAIN_EXPANSION, OPEN_DOMAIN -> "cast_seal_sukuna";
            case SUMMON_MAHORAGA -> "cast_domain";
            case ULTIMATE_SLASH -> "cast_cross";
            case WHEEL_ADAPTATION, REVERSE_CURSED -> "cast_charge";
            case BLACK_FLASH -> "cast_uppercut";
            case FOUR_ARM_FLURRY -> "cast_punch";
            default -> null;
        };
    }
}
