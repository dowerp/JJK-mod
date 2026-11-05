package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AnimPayload(int entityId, int ability, int form) implements CustomPayload {
    public static final CustomPayload.Id<AnimPayload> ID = new CustomPayload.Id<>(SukunaMod.id("anim"));
    public static final PacketCodec<RegistryByteBuf, AnimPayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, AnimPayload::entityId,
        PacketCodecs.VAR_INT, AnimPayload::ability,
        PacketCodecs.VAR_INT, AnimPayload::form,
        AnimPayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
