package com.dowerp.sukuna;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

public final class Seals {
    private static final int DURATION = 36;

    private Seals() {
    }

    public static boolean then(ServerPlayerEntity p, SukunaState s, Runnable onDone) {
        ServerWorld w = Fx.world(p);
        s.busyUntil = w.getTime() + DURATION + 2;
        final int core = s.form.isGojo() ? Ink.BLUE : s.form == SukunaForm.YUJI ? Ink.WHITE : Ink.BLOOD;
        final int accent = s.form.isGojo() ? Ink.PURPLE : s.form == SukunaForm.YUJI ? Ink.BLUE : Ink.RED;

        final int pal = Ink.palette(core, accent);

        Tasks.add(new Task() {
            private int t = 0;

            @Override
            public boolean tick() {
                if (!p.isAlive()) return true;
                t++;
                Vec3d look = p.getRotationVec(1.0F);
                Vec3d c = p.getEyePos().add(look.multiply(1.4)).add(0.0, -0.3, 0.0);
                if (t % 6 == 0 && t < DURATION) {
                    double size = 1.0 + t * 0.045;
                    Ink.sprite(w, c, look, com.dowerp.sukuna.entity.SpriteFxEntity.RING, pal, size, size, t * 0.35, 12);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, SoundCategory.PLAYERS, 1.0F, 0.9F + t * 0.02F);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9F, 0.6F + t * 0.03F);
                }
                if (t == DURATION / 2) {
                    Ink.impact(w, c, look, accent);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BELL_USE, SoundCategory.PLAYERS, 1.5F, 0.8F);
                }
                if (t >= DURATION) {
                    Ink.impact(w, c, look, accent);
                    Ink.ring(w, new Vec3d(p.getX(), p.getY() + 0.1, p.getZ()), 2.4, accent, 0.8F);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 2.5F, 0.6F);
                    Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 1.2F, 1.6F);
                    onDone.run();
                    return true;
                }
                return false;
            }
        });
        return true;
    }
}
