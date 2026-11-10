package com.dowerp.sukuna.entity;

import com.dowerp.sukuna.Adaptation;
import com.dowerp.sukuna.Fx;
import com.dowerp.sukuna.Ink;
import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.SukunaState;
import com.dowerp.sukuna.item.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class MahoragaEntity extends PathAwareEntity implements GeoEntity {
    private static final TrackedData<Integer> ADAPT_STEPS = DataTracker.registerData(MahoragaEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.mahoraga.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.mahoraga.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.mahoraga.attack");
    private static final RawAnimation SUMMON = RawAnimation.begin().thenPlay("animation.mahoraga.summon");
    private static final RawAnimation WHEEL_SPIN = RawAnimation.begin().thenLoop("animation.mahoraga.wheel_spin");
    private static final RawAnimation ADAPT = RawAnimation.begin().thenPlay("animation.mahoraga.adapt");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private UUID owner;
    private int life = SukunaMod.config.mahoragaLifetimeTicks;
    private final Adaptation adaptation = new Adaptation();

    public MahoragaEntity(EntityType<? extends MahoragaEntity> type, World world) {
        super(type, world);
        this.equipStack(EquipmentSlot.MAINHAND, new ItemStack(ModItems.SWORD_OF_EXTERMINATION));
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
            .add(EntityAttributes.MAX_HEALTH, 240.0)
            .add(EntityAttributes.ATTACK_DAMAGE, 18.0)
            .add(EntityAttributes.MOVEMENT_SPEED, 0.34)
            .add(EntityAttributes.KNOCKBACK_RESISTANCE, 0.9)
            .add(EntityAttributes.FOLLOW_RANGE, 40.0)
            .add(EntityAttributes.ARMOR, 8.0)
            .add(EntityAttributes.SCALE, 1.35)
            .add(EntityAttributes.STEP_HEIGHT, 1.2)
            .add(EntityAttributes.ATTACK_KNOCKBACK, 1.2);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<MahoragaEntity>("main", 6, test -> test.setAndContinue(test.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<MahoragaEntity>("wheel", 0, test -> test.setAndContinue(WHEEL_SPIN))
            .triggerableAnim("adapt", ADAPT));
        controllers.add(new AnimationController<MahoragaEntity>("action", 0, test -> PlayState.STOP)
            .triggerableAnim("attack", ATTACK)
            .triggerableAnim("summon", SUMMON));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    @Override
    public void swingHand(Hand hand, boolean fromServerPlayer) {
        if (!this.getEntityWorld().isClient()) {
            this.triggerAnim("action", "attack");
        }
        super.swingHand(hand, fromServerPlayer);
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(ADAPT_STEPS, 0);
    }

    public int getAdaptSteps() {
        return this.dataTracker.get(ADAPT_STEPS);
    }

    public void setOwner(UUID id) {
        this.owner = id;
    }

    public boolean ownedBy(UUID id) {
        return owner != null && owner.equals(id);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.add(6, new LookAroundGoal(this));
        this.targetSelector.add(1, new RevengeGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, true));
    }

    @Override
    protected void mobTick(ServerWorld w) {
        super.mobTick(w);
        ServerPlayerEntity o = owner == null ? null : w.getServer().getPlayerManager().getPlayer(owner);
        life--;
        if (this.age % 10 == 0 && !adaptation.expire(w.getTime()).isEmpty()) {
            this.dataTracker.set(ADAPT_STEPS, adaptation.total());
            Fx.puff(w, ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 2.6, this.getZ(), 24, 0.5, 0.4, 0.5, 0.2);
            Fx.sound(w, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 1.4F, 0.6F);
        }
        if (this.age == 2) {
            this.triggerAnim("action", "summon");
        }
        if (o == null || !o.isAlive() || life <= 0) {
            vanish(w);
            return;
        }
        if (this.age % 10 == 0) {
            assist(o);
        }
    }

    private void assist(ServerPlayerEntity o) {
        LivingEntity t = this.getTarget();
        if (t == null || !t.isAlive()) {
            LivingEntity c = o.getAttacker();
            if (c == null || !c.isAlive()) {
                c = o.getAttacking();
            }
            if (c != null && c.isAlive() && c != this && !ownedBy(c.getUuid())) {
                this.setTarget(c);
            }
        }
        LivingEntity current = this.getTarget();
        if (current == null || !current.isAlive()) {
            double d = this.squaredDistanceTo(o);
            if (d > 100.0) {
                this.getNavigation().startMovingTo(o, 1.25);
            }
            if (d > 1600.0) {
                this.refreshPositionAndAngles(o.getX(), o.getY(), o.getZ(), this.getYaw(), 0.0F);
            }
        }
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        adaptation.reset();
        super.onDeath(damageSource);
    }

    private void vanish(ServerWorld w) {
        adaptation.reset();
        Fx.far(w, ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1.5, this.getZ(), 50, 0.6, 1.2, 0.6, 0.06);
        Fx.sound(w, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.HOSTILE, 1.2F, 1.6F);
        this.discard();
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        Entity attacker = source.getAttacker();
        if (attacker != null && ownedBy(attacker.getUuid())) {
            return false;
        }
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) {
            return super.damage(world, source, amount);
        }
        Adaptation.Result r = adaptation.hit(source.getName(), world.getTime());
        if (r.stepped()) {
            this.dataTracker.set(ADAPT_STEPS, this.dataTracker.get(ADAPT_STEPS) + 1);
            this.triggerAnim("wheel", "adapt");
            Fx.sound(world, this.getX(), this.getY(), this.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.HOSTILE, 1.2F, 1.5F);
        }
        if (r.multiplier() <= 0.001F) {
            Fx.puff(world, ParticleTypes.ENCHANTED_HIT, this.getX(), this.getY() + 1.5, this.getZ(), 12, 0.5, 0.8, 0.5, 0.1);
            return false;
        }
        return super.damage(world, source, amount * r.multiplier());
    }

    public static boolean summon(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        if (s.mahoraga != null) {
            Entity old = w.getEntity(s.mahoraga);
            s.mahoraga = null;
            if (old != null && old.isAlive()) {
                old.discard();
                return false;
            }
        }
        MahoragaEntity m = ModEntities.MAHORAGA.create(w, SpawnReason.MOB_SUMMONED);
        if (m == null) {
            return false;
        }
        Vec3d look = p.getRotationVec(1.0F);
        double x = p.getX() + look.x * 3.0;
        double z = p.getZ() + look.z * 3.0;
        m.refreshPositionAndAngles(x, p.getY(), z, p.getYaw() + 180.0F, 0.0F);
        m.setOwner(p.getUuid());
        m.getAttributeInstance(EntityAttributes.MAX_HEALTH).setBaseValue(240.0 + 12.0 * s.statFingers());
        m.setHealth(m.getMaxHealth());
        w.spawnEntity(m);
        s.mahoraga = m.getUuid();
        Fx.far(w, ParticleTypes.SCULK_SOUL, x, p.getY() + 1.5, z, 80, 0.8, 1.4, 0.8, 0.08);
        Ink.ring(w, new Vec3d(x, p.getY() + 0.1, z), 2.0, Ink.GOLD, 0.9F);
        Ink.splatter(w, new Vec3d(x, p.getY() + 1.2, z), 3.0, 16, Ink.GOLD);
        Fx.sound(w, x, p.getY(), z, SoundEvents.ENTITY_WARDEN_EMERGE, SoundCategory.PLAYERS, 2.0F, 0.6F);
        Fx.sound(w, x, p.getY(), z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.5F, 0.8F);
        return true;
    }
}
