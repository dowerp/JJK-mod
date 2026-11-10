package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record RemoteStatePayload(int entityId, int character, int form, int flags, String head) implements CustomPayload {
    public static final CustomPayload.Id<RemoteStatePayload> ID = new CustomPayload.Id<>(SukunaMod.id("remote_state"));
    public static final PacketCodec<RegistryByteBuf, RemoteStatePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, RemoteStatePayload::entityId,
        PacketCodecs.VAR_INT, RemoteStatePayload::character,
        PacketCodecs.VAR_INT, RemoteStatePayload::form,
        PacketCodecs.VAR_INT, RemoteStatePayload::flags,
        PacketCodecs.STRING, RemoteStatePayload::head,
        RemoteStatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
