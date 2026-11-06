package com.dowerp.sukuna.item;

import com.dowerp.sukuna.SukunaMod;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Rarity;

import java.util.function.Function;

public final class ModItems {
    public static final Item SUKUNA_FINGER = register("sukuna_finger", FingerItem::new, new Item.Settings().maxCount(20).rarity(Rarity.EPIC));
    public static final Item SWORD_OF_EXTERMINATION = register("sword_of_extermination", Item::new, new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    public static final Item SOUL_SPLIT_KATANA = register("soul_split_katana", Item::new, new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    public static final Item PLAYFUL_CLOUD = register("playful_cloud", Item::new, new Item.Settings().maxCount(1).rarity(Rarity.EPIC));

    private ModItems() {
    }

    private static <T extends Item> T register(String path, Function<Item.Settings, T> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, SukunaMod.id(path));
        T item = factory.apply(settings.registryKey(key));
        return Registry.register(Registries.ITEM, key, item);
    }

    public static void init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> entries.add(SUKUNA_FINGER));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT).register(entries -> entries.add(SWORD_OF_EXTERMINATION));
    }
}
