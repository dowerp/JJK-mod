package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

public record AdminActionPayload(int action) implements CustomPayload {
    public static final int HEAL_ALL = 0;
    public static final int CLEAR_COMBAT = 1;
    public static final int CD_SELF = 2;
    public static final int CD_ALL = 3;
    public static final int CE_SELF = 4;
    public static final int NOCD_TOGGLE = 5;

    public static final CustomPayload.Id<AdminActionPayload> ID = new CustomPayload.Id<>(SukunaMod.id("admin_action"));
    public static final PacketCodec<RegistryByteBuf, AdminActionPayload> CODEC = PacketCodec.tuple(PacketCodecs.VAR_INT, AdminActionPayload::action, AdminActionPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
