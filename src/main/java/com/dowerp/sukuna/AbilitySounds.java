package com.dowerp.sukuna;

import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public final class AbilitySounds {
    private AbilitySounds() {
    }

    private static void snd(ServerWorld w, ServerPlayerEntity p, SoundEvent e, float v, float pitch) {
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), e, SoundCategory.PLAYERS, v, pitch);
    }

    private static void snd(ServerWorld w, ServerPlayerEntity p, RegistryEntry<SoundEvent> e, float v, float pitch) {
        Fx.sound(w, p.getX(), p.getY(), p.getZ(), e, SoundCategory.PLAYERS, v, pitch);
    }

    public static void play(ServerWorld w, ServerPlayerEntity p, SukunaForm form, Ability a) {
        if (form.isGojo()) {
            gojo(w, p, a);
        } else if (form.isChoso()) {
            choso(w, p, a);
        } else if (form.isTodo()) {
            todo(w, p, a);
        } else if (form.isMaki()) {
            maki(w, p, a);
        } else if (form.isMegumi() || form.isYuta()) {
            summoner(w, p, a);
        } else if (form == SukunaForm.YUJI) {
            yuji(w, p, a);
        } else {
            sukuna(w, p, a, form == SukunaForm.HEIAN);
        }
    }

    private static void yuji(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.2F, 0.8F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.3F, 0.8F);
            case UTILITY_TECHNIQUE -> {
                snd(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.9F);
                snd(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.0F, 1.6F);
            }
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0F, 1.7F);
            case DOMAIN_EXPANSION -> snd(w, p, SoundEvents.BLOCK_BELL_RESONATE, 2.0F, 0.6F);
            case REVERSE_CURSED -> snd(w, p, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 0.7F);
            default -> {
            }
        }
    }

    private static void sukuna(ServerWorld w, ServerPlayerEntity p, Ability a, boolean heian) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, heian ? 0.9F : 0.7F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.3F, 0.6F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.ENTITY_WITHER_SHOOT, 0.6F, 1.5F);
            case SPECIAL_ATTACK -> {
                snd(w, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.6F, 0.6F);
                snd(w, p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2F, 0.7F);
            }
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_EMERGE, 1.5F, 0.6F);
            case DOMAIN_EXPANSION -> {
                snd(w, p, SoundEvents.ENTITY_WITHER_SPAWN, 1.2F, 0.7F);
                snd(w, p, SoundEvents.BLOCK_BELL_RESONATE, 2.0F, 0.4F);
            }
            case OPEN_DOMAIN -> {
                snd(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.5F, 0.5F);
                snd(w, p, SoundEvents.ENTITY_WITHER_SPAWN, 1.0F, 0.5F);
            }
            case WHEEL_ADAPTATION -> snd(w, p, SoundEvents.BLOCK_BELL_RESONATE, 1.5F, 1.4F);
            case SUMMON_MAHORAGA -> {
                snd(w, p, SoundEvents.ENTITY_WARDEN_EMERGE, 1.5F, 0.8F);
                snd(w, p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 1.2F);
            }
            case REVERSE_CURSED -> snd(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.8F);
            case DISMANTLE_STORM -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.5F, 0.5F);
            case BLACK_FLASH -> snd(w, p, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, 1.2F, 1.5F);
            case FOUR_ARM_FLURRY -> {
                snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.4F, 1.1F);
                snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
            }
            default -> {
            }
        }
    }

    private static void gojo(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1.0F, 1.8F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ITEM_FIRECHARGE_USE, 1.2F, 0.5F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.3F);
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0F, 1.4F);
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.5F, 0.5F);
            case DOMAIN_EXPANSION -> snd(w, p, SoundEvents.BLOCK_BELL_RESONATE, 2.0F, 0.5F);
            case REVERSE_CURSED -> snd(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.2F);
            default -> {
            }
        }
    }

    private static void summoner(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.2F, 0.8F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0F, 1.2F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.5F);
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.6F);
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1.2F, 0.9F);
            default -> {
            }
        }
    }

    private static void maki(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.4F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ITEM_TRIDENT_RIPTIDE_1, 1.0F, 1.6F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0F, 1.8F);
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.ITEM_TRIDENT_THROW, 1.0F, 0.8F);
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1.0F, 1.1F);
            default -> {
            }
        }
    }

    private static void todo(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 1.5F, 1.8F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.3F, 0.7F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8F, 1.6F);
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0F, 1.6F);
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, 1.5F, 1.4F);
            case REVERSE_CURSED -> snd(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.9F);
            default -> {
            }
        }
    }

    private static void choso(ServerWorld w, ServerPlayerEntity p, Ability a) {
        switch (a) {
            case PRIMARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2F, 1.4F);
            case SECONDARY_ATTACK -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.8F, 1.5F);
            case UTILITY_TECHNIQUE -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
            case SPECIAL_ATTACK -> snd(w, p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0F, 1.5F);
            case DOMAIN_EXPANSION -> snd(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.2F, 0.7F);
            case REVERSE_CURSED -> snd(w, p, SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 0.6F);
            case ULTIMATE_SLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_HEARTBEAT, 1.5F, 0.6F);
            case OPEN_DOMAIN -> snd(w, p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.4F, 0.6F);
            case DISMANTLE_STORM -> snd(w, p, SoundEvents.ENTITY_WARDEN_EMERGE, 1.0F, 1.6F);
            case BLACK_FLASH -> snd(w, p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 2.0F, 1.0F);
            default -> {
            }
        }
    }
}
