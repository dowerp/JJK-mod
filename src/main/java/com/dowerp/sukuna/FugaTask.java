package com.dowerp.sukuna;

import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public final class FugaTask implements Task {
    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final SukunaState s;
    private final double power;
    private final int fingers;
    private int charge = 16;
    private Vec3d pos;
    private Vec3d dir;
    private double left = 110.0;

    private FugaTask(ServerPlayerEntity owner, SukunaState s) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.s = s;
        this.power = s.power();
        this.fingers = s.fingers;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s) {
        s.charging = true;
        s.busyUntil = Fx.world(p).getTime() + 90;
        Tasks.add(new FugaTask(p, s));
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 1.5F, 0.5F);
        return true;
    }

    @Override
    public boolean tick() {
        if (owner.isRemoved() || !owner.isAlive()) {
            s.charging = false;
            return true;
        }
        if (charge > 0) {
            charge--;
            Vec3d look = owner.getRotationVec(1.0F);
            Vec3d hand = owner.getEyePos().add(look.multiply(1.4)).add(0.0, -0.3, 0.0);
            double rr = 0.9 - charge * 0.04;
            Ink.ring(w, hand, Math.max(0.2, rr), Ink.FIRE, 0.5F);
            Ink.dot(w, hand, Ink.FIRE_CORE, 0.9F + (16 - charge) * 0.05F);
            if (charge % 4 == 0) {
                owner.swingHand(Hand.MAIN_HAND, true);
                Fx.sound(w, hand.x, hand.y, hand.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.8F, 0.6F + (16 - charge) * 0.05F);
            }
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 3, false, false));
            if (charge == 0) {
                pos = hand;
                dir = look;
                Fx.sound(w, hand.x, hand.y, hand.z, SoundEvents.ENTITY_WITHER_SHOOT, SoundCategory.PLAYERS, 1.6F, 0.6F);
            }
            return false;
        }
        double step = 0.5;
        for (int i = 0; i < 6; i++) {
            Vec3d next = pos.add(dir.multiply(step));
            BlockPos bp = BlockPos.ofFloored(next.x, next.y, next.z);
            if (!w.isPosLoaded(bp)) {
                explode(pos);
                return true;
            }
            BlockState st = w.getBlockState(bp);
            if (!st.getCollisionShape(w, bp).isEmpty()) {
                explode(pos);
                return true;
            }
            Box b = new Box(next, next).expand(s.form == SukunaForm.HEIAN ? 2.4 : 1.1);
            if (!w.getEntitiesByClass(LivingEntity.class, b, x -> Aim.valid(owner, x)).isEmpty()) {
                explode(next);
                return true;
            }
            pos = next;
            left -= step;
            if (i == 0) {
                boolean big = s.form == SukunaForm.HEIAN;
                Ink.fireArrow(w, pos.subtract(dir.multiply(big ? 2.4 : 1.6)), dir, big ? 7.5 : 4.6, big ? 3.2 : 1.9, 3);
            }
            if (left <= 0.0) {
                explode(pos);
                return true;
            }
        }
        return false;
    }

    private void explode(Vec3d at) {
        s.charging = false;
        owner.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 12, 4, false, false));
        World.ExplosionSourceType type = SukunaMod.config.blockDamage ? World.ExplosionSourceType.MOB : World.ExplosionSourceType.NONE;
        boolean heian = s.form == SukunaForm.HEIAN;
        float blast = heian ? (float) (11.0 + 0.14 * fingers) : (float) (4.0 + 0.08 * fingers);
        w.createExplosion(owner, at.x, at.y, at.z, blast, true, type);
        double r = heian ? 26.0 : 9.0;
        double dmgBase = heian ? 60.0 : 26.0;
        Box box = new Box(at.x - r, at.y - r, at.z - r, at.x + r, at.y + r, at.z + r);
        for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, box, x -> Aim.valid(owner, x))) {
            double d = Math.sqrt(e.squaredDistanceTo(at.x, at.y, at.z));
            if (d > r) {
                continue;
            }
            double falloff = 1.0 - d / (r * 1.4);
            Dmg.hit(owner, e, dmgBase * power * falloff);
            e.setOnFireFor(8.0F);
        }
        Ink.ring(w, at, heian ? 11.0 : 3.0, Ink.FIRE, 1.0F);
        Ink.ring(w, at.add(0, 0.3, 0), heian ? 19.0 : 5.5, Ink.FIRE_CORE, 0.8F);
        for (int k = 0; k < 14; k++) {
            double a = k * Math.PI * 2.0 / 14.0;
            Vec3d d = new Vec3d(Math.cos(a), 0.25 + (k % 3) * 0.25, Math.sin(a)).normalize();
            Ink.stroke(w, at.add(d.multiply(0.6)), at.add(d.multiply(3.5 + (k % 4))), k % 2 == 0 ? Ink.FIRE_CORE : Ink.FIRE, Ink.BLACK, 0.8F, true);
        }
        Ink.splatter(w, at.add(0, 1.0, 0), heian ? 9.0 : 4.0, 20, Ink.FIRE);
        Fx.sound(w, at.x, at.y, at.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 3.0F, 0.9F);
    }
}
