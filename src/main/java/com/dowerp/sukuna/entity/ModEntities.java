package com.dowerp.sukuna.entity;

import com.dowerp.sukuna.SukunaMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

public final class ModEntities {
    public static final RegistryKey<EntityType<?>> MAHORAGA_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, SukunaMod.id("mahoraga"));
    public static final EntityType<MahoragaEntity> MAHORAGA = Registry.register(
        Registries.ENTITY_TYPE,
        MAHORAGA_KEY,
        EntityType.Builder.create(MahoragaEntity::new, SpawnGroup.MISC).dimensions(0.9F, 2.4F).maxTrackingRange(10).build(MAHORAGA_KEY)
    );

    public static final EntityType<TechniqueOrbEntity> ORB_BLUE = orb("orb_blue", 1.4F);
    public static final EntityType<TechniqueOrbEntity> ORB_RED = orb("orb_red", 1.4F);
    public static final EntityType<TechniqueOrbEntity> ORB_BLUE_SMALL = orb("orb_blue_small", 1.4F);
    public static final EntityType<TechniqueOrbEntity> ORB_RED_SMALL = orb("orb_red_small", 1.4F);
    public static final EntityType<TechniqueOrbEntity> ORB_PURPLE = orb("orb_purple", 4.0F);
    public static final EntityType<TechniqueOrbEntity> ORB_PURPLE_SMALL = orb("orb_purple_small", 1.4F);

    public static final EntityType<TechniqueOrbEntity> SHIKI_DOG = orb("shikigami_dog", 1.2F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_DOG_BLACK = orb("shikigami_dog_black", 1.2F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_NUE = orb("shikigami_nue", 1.6F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_TOAD = orb("shikigami_toad", 1.2F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_RABBIT = orb("shikigami_rabbit", 0.6F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_ELEPHANT = orb("shikigami_elephant", 3.0F);
    public static final EntityType<TechniqueOrbEntity> SHADOW_POOL = orb("shadow_pool", 2.0F);
    public static final EntityType<TechniqueOrbEntity> RIKA = orb("rika", 3.6F);
    public static final EntityType<TechniqueOrbEntity> RELEASE_S = orb("release_wave_s", 2.0F);
    public static final EntityType<TechniqueOrbEntity> RELEASE_M = orb("release_wave_m", 3.0F);
    public static final EntityType<TechniqueOrbEntity> RELEASE_L = orb("release_wave_l", 4.0F);
    public static final EntityType<TechniqueOrbEntity> WATER_SURGE = orb("water_surge", 3.0F);

    public static final EntityType<TechniqueOrbEntity> SHIKI_SERPENT = orb("shikigami_serpent", 2.0F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_DEER = orb("shikigami_deer", 1.6F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_OX = orb("shikigami_ox", 2.0F);
    public static final EntityType<TechniqueOrbEntity> SHIKI_TIGER = orb("shikigami_tiger", 1.8F);
    public static final EntityType<TechniqueOrbEntity> YUTA_SWORD = orb("yuta_sword", 0.8F);

    public static final EntityType<SpriteFxEntity> SPRITE = sprite();

    private ModEntities() {
    }

    private static EntityType<TechniqueOrbEntity> orb(String path, float size) {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, SukunaMod.id(path));
        return Registry.register(
            Registries.ENTITY_TYPE,
            key,
            EntityType.Builder.create(TechniqueOrbEntity::new, SpawnGroup.MISC)
                .dimensions(size, size)
                .maxTrackingRange(16)
                .trackingTickInterval(1)
                .disableSaving()
                .disableSummon()
                .build(key)
        );
    }

    private static EntityType<SpriteFxEntity> sprite() {
        RegistryKey<EntityType<?>> key = RegistryKey.of(RegistryKeys.ENTITY_TYPE, SukunaMod.id("sprite_fx"));
        return Registry.register(
            Registries.ENTITY_TYPE,
            key,
            EntityType.Builder.create(SpriteFxEntity::new, SpawnGroup.MISC)
                .dimensions(0.2F, 0.2F)
                .maxTrackingRange(12)
                .trackingTickInterval(2)
                .disableSaving()
                .disableSummon()
                .build(key)
        );
    }

    public static void init() {
        FabricDefaultAttributeRegistry.register(MAHORAGA, MahoragaEntity.createAttributes());
    }
}
