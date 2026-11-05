package com.dowerp.sukuna.block;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {
    public static final Block[] VOID_WALL = new Block[4];
    public static final Block[] CITY_SKY = new Block[3];
    public static final Block[] SUKUNA_WALL = new Block[3];
    public static final Block VOID_FLOOR;
    public static final Block MIRROR_FLOOR;

    static {
        for (int i = 0; i < VOID_WALL.length; i++) {
            VOID_WALL[i] = register("void_wall_" + i, 15);
        }
        for (int i = 0; i < CITY_SKY.length; i++) {
            CITY_SKY[i] = register("city_sky_" + i, 12);
        }
        for (int i = 0; i < SUKUNA_WALL.length; i++) {
            SUKUNA_WALL[i] = register("sukuna_wall_" + i, 9);
        }
        VOID_FLOOR = register("void_floor", 6);
        MIRROR_FLOOR = register("mirror_floor", 0);
    }

    private ModBlocks() {
    }

    private static Block register(String name, int light) {
        RegistryKey<Block> key = RegistryKey.of(RegistryKeys.BLOCK, SukunaMod.id(name));
        AbstractBlock.Settings settings = AbstractBlock.Settings.create()
            .registryKey(key)
            .strength(-1.0F, 3600000.0F)
            .dropsNothing()
            .luminance(state -> light)
            .sounds(BlockSoundGroup.STONE);
        return Registry.register(Registries.BLOCK, key, new Block(settings));
    }

    public static void init() {
    }
}
