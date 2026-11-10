package com.dowerp.sukuna.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.world.World;

/** Нарисованный 2D-эффект техники: плоская текстура в мире (разрез, полоса, молния, кольцо, вспышка). */
public class SpriteFxEntity extends Entity {
    public static final int STREAK = 0;
    public static final int CRESCENT = 1;
    public static final int BOLT = 2;
    public static final int RING = 3;
    public static final int BURST = 4;
    public static final int DISTORT = 5;
    public static final int BLOB = 6;
    public static final int SPLASH = 7;
    public static final int TORRENT = 8;
    public static final int ARROW = 9;

    private static final TrackedData<Integer> KIND = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Integer> PALETTE = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Float> LENGTH = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> WIDTH = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> ROLL = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Integer> LIFE = DataTracker.registerData(SpriteFxEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public SpriteFxEntity(EntityType<? extends SpriteFxEntity> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public void configure(int kind, int palette, double length, double width, double roll, int life) {
        this.dataTracker.set(KIND, kind);
        this.dataTracker.set(PALETTE, palette);
        this.dataTracker.set(LENGTH, (float) length);
        this.dataTracker.set(WIDTH, (float) width);
        this.dataTracker.set(ROLL, (float) roll);
        this.dataTracker.set(LIFE, life);
    }

    public int kind() {
        return this.dataTracker.get(KIND);
    }

    public int palette() {
        return this.dataTracker.get(PALETTE);
    }

    public float length() {
        return this.dataTracker.get(LENGTH);
    }

    public float width() {
        return this.dataTracker.get(WIDTH);
    }

    public float roll() {
        return this.dataTracker.get(ROLL);
    }

    public int life() {
        return Math.max(1, this.dataTracker.get(LIFE));
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(KIND, 0);
        builder.add(PALETTE, 0);
        builder.add(LENGTH, 1.0F);
        builder.add(WIDTH, 1.0F);
        builder.add(ROLL, 0.0F);
        builder.add(LIFE, 8);
    }

    @Override
    protected void readCustomData(ReadView view) {
    }

    @Override
    protected void writeCustomData(WriteView view) {
    }

    @Override
    public boolean damage(ServerWorld world, DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean canHit() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.getEntityWorld().isClient() && this.age >= life()) {
            this.discard();
        }
    }
}
