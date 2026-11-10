package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record DomainMeterPayload(int tenths) implements CustomPayload {
    public static final int DISABLED = -1;

    public static final CustomPayload.Id<DomainMeterPayload> ID = new CustomPayload.Id<>(SukunaMod.id("domain_meter"));
    public static final PacketCodec<RegistryByteBuf, DomainMeterPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, DomainMeterPayload::tenths, DomainMeterPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
