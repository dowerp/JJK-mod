package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record WeaponPayload(int weapon) implements CustomPayload {
    public static final CustomPayload.Id<WeaponPayload> ID = new CustomPayload.Id<>(SukunaMod.id("weapon"));
    public static final PacketCodec<RegistryByteBuf, WeaponPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, WeaponPayload::weapon, WeaponPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
