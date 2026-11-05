package com.dowerp.sukuna;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public final class CleaveTask implements Task {
    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final LivingEntity target;
    private final double total;
    private int tick;

    private CleaveTask(ServerPlayerEntity owner, LivingEntity target, double total) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.target = target;
        this.total = total;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s) {
        LivingEntity t = Aim.target(p, 28.0 + s.statFingers());
        if (t == null) {
            p.sendMessage(Text.translatable("sukuna.msg.no_target"), true);
            return false;
        }
        double hp = t.getMaxHealth();
        double total = (6.0 + 0.2 * hp + 0.6 * t.getArmor()) * s.power();
        if (hp > 120.0) {
            total = Math.min(total, hp * 0.3);
        }
        boolean boss = t instanceof WitherEntity || t instanceof EnderDragonEntity || t instanceof WardenEntity || hp >= 200.0;
        if (t instanceof PlayerEntity || boss) {
            total *= 1.6;
        }
        Tasks.add(new CleaveTask(p, t, total));
        p.swingHand(Hand.MAIN_HAND, true);
        return true;
    }

    @Override
    public boolean tick() {
        if (owner.isRemoved() || !target.isAlive()) {
            return true;
        }
        if (tick == 0 || tick == 3 || tick == 6) {
            strike();
        }
        tick++;
        return tick > 7;
    }

    private void strike() {
        Vec3d c = new Vec3d(target.getX(), target.getY() + target.getHeight() * 0.5, target.getZ());
        Vec3d dir = c.subtract(owner.getEyePos()).normalize();
        double roll = tick == 0 ? 0.6 : tick == 3 ? -0.6 : 1.57;
        Fx.crescent(w, c, dir, roll, 1.7, Fx.WHITE);
        Ink.cutMark(w, c, dir, roll, 2.6, Ink.BLOOD);
        Interact.cutProjectiles(w, owner, owner.getEyePos(), c, 2.5);
        Ink.impact(w, c, dir, Ink.BLOOD);
        Fx.sound(w, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, SoundCategory.PLAYERS, 1.2F, 0.8F + tick * 0.05F);
        Dmg.hit(owner, target, total / 3.0);
        owner.swingHand(Hand.MAIN_HAND, true);
    }
}
