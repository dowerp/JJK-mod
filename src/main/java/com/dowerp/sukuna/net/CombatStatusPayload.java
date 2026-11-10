package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Статус боя для HUD: штраф перегрева (%), серия Чёрной вспышки, секунды до конца зоны. */
public record CombatStatusPayload(int heatPercent, int streak, int zoneSeconds) implements CustomPayload {
    public static final CustomPayload.Id<CombatStatusPayload> ID = new CustomPayload.Id<>(SukunaMod.id("combat_status"));
    public static final PacketCodec<RegistryByteBuf, CombatStatusPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, CombatStatusPayload::heatPercent,
        PacketCodecs.VAR_INT, CombatStatusPayload::streak,
        PacketCodecs.VAR_INT, CombatStatusPayload::zoneSeconds,
        CombatStatusPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
