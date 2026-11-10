package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AbilityPayload(int ability) implements CustomPayload {
    public static final CustomPayload.Id<AbilityPayload> ID = new CustomPayload.Id<>(SukunaMod.id("ability"));
    public static final PacketCodec<RegistryByteBuf, AbilityPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, AbilityPayload::ability, AbilityPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
