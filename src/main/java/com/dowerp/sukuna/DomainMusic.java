package com.dowerp.sukuna;

import net.minecraft.network.packet.s2c.play.StopSoundS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/** Музыка территории: ванильные пластинки играют вокруг территории и выключаются, когда она исчезает. */
public final class DomainMusic {
    public static final String SUKUNA = "music_disc.pigstep";
    public static final String HEIAN = "music_disc.relic";
    public static final String GOJO = "music_disc.otherside";
    public static final String YUJI = "music_disc.mall";

    private DomainMusic() {
    }

    private static SoundEvent find(String path) {
        return Registries.SOUND_EVENT.get(Identifier.ofVanilla(path));
    }

    public static void start(ServerWorld w, Vec3d c, String track) {
        SoundEvent e = find(track);
        if (e != null) {
            w.playSound(null, c.x, c.y, c.z, e, SoundCategory.RECORDS, 8.0F, 1.0F);
        }
    }

    public static void stop(ServerWorld w, String track) {
        SoundEvent e = find(track);
        if (e == null) {
            return;
        }
        Identifier id = Registries.SOUND_EVENT.getId(e);
        for (ServerPlayerEntity pl : w.getPlayers()) {
            pl.networkHandler.sendPacket(new StopSoundS2CPacket(id, SoundCategory.RECORDS));
        }
    }
}
