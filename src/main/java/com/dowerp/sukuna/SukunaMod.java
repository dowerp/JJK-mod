package com.dowerp.sukuna;

import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.item.ModItems;
import com.dowerp.sukuna.net.AbilityPayload;
import com.dowerp.sukuna.net.AdminActionPayload;
import com.dowerp.sukuna.net.AnimPayload;
import com.dowerp.sukuna.net.CharacterPayload;
import com.dowerp.sukuna.net.CombatStatusPayload;
import com.dowerp.sukuna.net.DomainMeterPayload;
import com.dowerp.sukuna.net.HoldPayload;
import com.dowerp.sukuna.net.M1Payload;
import com.dowerp.sukuna.net.WeaponPayload;
import com.dowerp.sukuna.net.RemoteStatePayload;
import com.dowerp.sukuna.net.StatePayload;
import com.dowerp.sukuna.tasks.GojoTasks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SukunaMod implements ModInitializer {
    public static final String ID = "sukuna";
    public static final Logger LOGGER = LoggerFactory.getLogger(ID);
    public static SukunaConfig config = new SukunaConfig();

    public static Identifier id(String path) {
        return Identifier.of(ID, path);
    }

    @Override
    public void onInitialize() {
        config = SukunaConfig.load();
        com.dowerp.sukuna.block.ModBlocks.init();
        AdminCommands.init();
        ModItems.init();
        ModEntities.init();
        PayloadTypeRegistry.playC2S().register(AbilityPayload.ID, AbilityPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(CharacterPayload.ID, CharacterPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(HoldPayload.ID, HoldPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(StatePayload.ID, StatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RemoteStatePayload.ID, RemoteStatePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(AnimPayload.ID, AnimPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(DomainMeterPayload.ID, DomainMeterPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CombatStatusPayload.ID, CombatStatusPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(M1Payload.ID, M1Payload.CODEC);
        PayloadTypeRegistry.playC2S().register(AdminActionPayload.ID, AdminActionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(WeaponPayload.ID, WeaponPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HoldPayload.ID, (payload, context) -> Players.get(context.player()).heldMask = payload.mask());
        ServerPlayNetworking.registerGlobalReceiver(CharacterPayload.ID, (payload, context) -> Players.select(context.player(), payload.character()));
        ServerPlayNetworking.registerGlobalReceiver(AbilityPayload.ID, (payload, context) -> Abilities.handle(context.player(), payload.ability()));
        ServerPlayNetworking.registerGlobalReceiver(AdminActionPayload.ID, (payload, context) -> AdminCommands.perform(context.player(), payload.action()));
        ServerPlayNetworking.registerGlobalReceiver(WeaponPayload.ID, (payload, context) -> com.dowerp.sukuna.tasks.MakiTasks.equip(context.player(), payload.weapon()));
        ServerPlayNetworking.registerGlobalReceiver(M1Payload.ID, (payload, context) -> M1.handle(context.player()));
        Players.init();
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            DomainTask.restoreAll();
            VoidDomainTask.restoreAll();
            SphereShell.restoreAll();
            DomainShield.clear();
            GojoTasks.clearLingering();
            Tasks.clear();
        });
    }
}
