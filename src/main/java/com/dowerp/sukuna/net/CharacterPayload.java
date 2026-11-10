package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record CharacterPayload(int character) implements CustomPayload {
    public static final CustomPayload.Id<CharacterPayload> ID = new CustomPayload.Id<>(SukunaMod.id("character"));
    public static final PacketCodec<RegistryByteBuf, CharacterPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, CharacterPayload::character, CharacterPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
