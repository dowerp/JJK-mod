package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record HoldPayload(int mask) implements CustomPayload {
    public static final CustomPayload.Id<HoldPayload> ID = new CustomPayload.Id<>(SukunaMod.id("hold"));
    public static final PacketCodec<RegistryByteBuf, HoldPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, HoldPayload::mask, HoldPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
