package com.dowerp.sukuna;

import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class Dmg {
    private Dmg() {
    }

    public static void hitQuiet(ServerPlayerEntity source, LivingEntity target, double amount) {
        ServerWorld w = Fx.world(source);
        DamageSource ds = w.getDamageSources().indirectMagic(source, source);
        target.timeUntilRegen = 0;
        target.damage(w, ds, (float) amount);
    }

    public static void hit(ServerPlayerEntity source, LivingEntity target, double amount) {
        ServerWorld w = Fx.world(source);
        DamageSource ds = w.getDamageSources().indirectMagic(source, source);
        target.timeUntilRegen = 0;
        Players.markCombat(source);
        target.damage(w, ds, (float) amount);
        Fx.sound(w, target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 0.6F, 0.8F + (float) Math.min(0.5, 30.0 / (amount + 30.0)));
    }
}
