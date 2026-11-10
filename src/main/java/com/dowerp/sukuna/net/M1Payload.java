package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record M1Payload() implements CustomPayload {
    public static final M1Payload INSTANCE = new M1Payload();
    public static final CustomPayload.Id<M1Payload> ID = new CustomPayload.Id<>(SukunaMod.id("m1"));
    public static final PacketCodec<RegistryByteBuf, M1Payload> CODEC = PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
