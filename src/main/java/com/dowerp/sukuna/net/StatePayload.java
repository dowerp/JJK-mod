package com.dowerp.sukuna.net;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

import java.util.List;

public record StatePayload(int ce, int maxCe, int flags, int domainLeft, int adaptation, int form, int kills, List<Integer> cooldowns, int character, int formLeft) implements CustomPayload {
    public static final int VESSEL = 1;
    public static final int WHEEL = 2;
    public static final int DOMAIN_CASTER = 4;
    public static final int IN_DOMAIN = 8;
    public static final int CHARGING = 16;
    public static final int MAHORAGA = 32;
    public static final int RED_SCALE = 64;
    public static final int SCALE_STACK = 128;
    public static final int HARDEN = 256;
    public static final int OPEN_READY = 512;
    public static final int KAMUTOKE = 1024;
    public static final int PIERCE_CHARGE = 2048;
    public static final int RUNNING = 4096;
    public static final int IN_BATTLE = 8192;
    public static final int INFINITY_OFF = 16384;

    public static final CustomPayload.Id<StatePayload> ID = new CustomPayload.Id<>(SukunaMod.id("state"));
    public static final PacketCodec<RegistryByteBuf, StatePayload> CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT, StatePayload::ce,
        PacketCodecs.VAR_INT, StatePayload::maxCe,
        PacketCodecs.VAR_INT, StatePayload::flags,
        PacketCodecs.VAR_INT, StatePayload::domainLeft,
        PacketCodecs.VAR_INT, StatePayload::adaptation,
        PacketCodecs.VAR_INT, StatePayload::form,
        PacketCodecs.VAR_INT, StatePayload::kills,
        PacketCodecs.VAR_INT.collect(PacketCodecs.toList()), StatePayload::cooldowns,
        PacketCodecs.VAR_INT, StatePayload::character,
        PacketCodecs.VAR_INT, StatePayload::formLeft,
        StatePayload::new
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
