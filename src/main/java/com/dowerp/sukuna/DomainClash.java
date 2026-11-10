package com.dowerp.sukuna;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class DomainClash {
    private static final int MAX_TICKS = 200;
    private static final List<ClashDomain> DOMAINS = new ArrayList<>();
    private static final List<Clash> CLASHES = new ArrayList<>();

    private DomainClash() {
    }

    private static final class Clash {
        final ClashDomain a;
        final ClashDomain b;
        int ticks;

        Clash(ClashDomain a, ClashDomain b) {
            this.a = a;
            this.b = b;
        }
    }

    public static void register(ClashDomain d) {
        if (!DOMAINS.contains(d)) {
            DOMAINS.add(d);
        }
    }

    public static void unregister(ClashDomain d) {
        DOMAINS.remove(d);
        CLASHES.removeIf(c -> c.a == d || c.b == d);
    }

    public static void clear() {
        DOMAINS.clear();
        CLASHES.clear();
    }

    public static boolean clashing(ClashDomain d) {
        for (Clash c : CLASHES) {
            if (c.a == d || c.b == d) {
                return true;
            }
        }
        return false;
    }

    public static void tick() {
        if (DOMAINS.size() < 2 && CLASHES.isEmpty()) {
            return;
        }
        for (int i = 0; i < DOMAINS.size(); i++) {
            ClashDomain a = DOMAINS.get(i);
            if (!a.clashActive()) continue;
            for (int j = i + 1; j < DOMAINS.size(); j++) {
                ClashDomain b = DOMAINS.get(j);
                if (!b.clashActive() || a.clashOwner() == b.clashOwner() || a.clashWorld() != b.clashWorld()) continue;
                if (clashing(a) || clashing(b)) continue;
                double reach = a.clashRadius() + b.clashRadius();
                if (a.clashCenter().squaredDistanceTo(b.clashCenter()) < reach * reach) {
                    start(a, b);
                }
            }
        }

        Iterator<Clash> it = CLASHES.iterator();
        while (it.hasNext()) {
            Clash c = it.next();
            if (!c.a.clashActive() || !c.b.clashActive() || !c.a.clashOwner().isAlive() || !c.b.clashOwner().isAlive()) {
                it.remove();
                continue;
            }
            if (step(c)) {
                it.remove();
            }
        }
    }

    private static void start(ClashDomain a, ClashDomain b) {
        CLASHES.add(new Clash(a, b));
        ServerWorld w = a.clashWorld();
        Vec3d mid = a.clashCenter().add(b.clashCenter()).multiply(0.5);
        Text title = Text.translatable("sukuna.clash.title").formatted(Formatting.GOLD, Formatting.BOLD);
        Text sub = Text.translatable("sukuna.clash.subtitle").formatted(Formatting.GRAY);
        for (ServerPlayerEntity pl : w.getPlayers(x -> true)) {
            if (pl.squaredDistanceTo(mid.x, mid.y, mid.z) > 90.0 * 90.0) continue;
            pl.networkHandler.sendPacket(new TitleFadeS2CPacket(3, 30, 10));
            pl.networkHandler.sendPacket(new SubtitleS2CPacket(sub));
            pl.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
        Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 4.0F, 0.6F);
        Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 4.0F, 0.5F);
    }

    private static double strength(ClashDomain d) {
        SukunaState s = Players.get(d.clashOwner());
        return Math.max(1.0, s.ce) * s.power();
    }

    private static boolean step(Clash c) {
        c.ticks++;
        ServerWorld w = c.a.clashWorld();
        SukunaState sa = Players.get(c.a.clashOwner());
        SukunaState sb = Players.get(c.b.clashOwner());
        double pa = strength(c.a);
        double pb = strength(c.b);

        double drainA = Math.max(0.5, Math.min(2.5, pb / pa));
        double drainB = Math.max(0.5, Math.min(2.5, pa / pb));
        if (!SukunaMod.config.freeCursedEnergy) {
            sa.ce = Math.max(0.0, sa.ce - drainA * 0.9);
            sb.ce = Math.max(0.0, sb.ce - drainB * 0.9);
        }

        Vec3d ca = c.a.clashCenter();
        Vec3d cb = c.b.clashCenter();
        Vec3d mid = ca.add(cb).multiply(0.5);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Vec3d axis = cb.subtract(ca);
        Vec3d dir = axis.lengthSquared() < 1.0E-4 ? new Vec3d(1, 0, 0) : axis.normalize();
        if (c.ticks % 2 == 0) {
            Vec3d[] bs = Fx.basis(dir);
            double a = r.nextDouble() * Math.PI * 2.0;
            double rr = 2.0 + r.nextDouble() * 6.0;
            Vec3d p = mid.add(bs[0].multiply(Math.cos(a) * rr)).add(bs[1].multiply(Math.sin(a) * rr));
            Ink.bolt(w, mid, p, Ink.WHITE, c.ticks % 4 == 0 ? Ink.RED : Ink.BLUE, 0.5F);
        }
        if (c.ticks % 10 == 0) {
            Ink.ring(w, mid, 3.0 + (c.ticks % 40) * 0.2, Ink.GOLD, 0.7F);
            Ink.impact(w, mid, dir, Ink.GOLD);
            Fx.sound(w, mid.x, mid.y, mid.z, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 2.0F, 0.5F + r.nextFloat() * 0.5F);
        }
        if (c.ticks % 5 == 0) {
            c.a.clashOwner().sendMessage(Text.translatable("sukuna.clash.bar", (int) sa.ce, (int) sb.ce), true);
            c.b.clashOwner().sendMessage(Text.translatable("sukuna.clash.bar", (int) sb.ce, (int) sa.ce), true);
        }

        boolean aOut = sa.ce <= 0.0 && !SukunaMod.config.freeCursedEnergy;
        boolean bOut = sb.ce <= 0.0 && !SukunaMod.config.freeCursedEnergy;
        if (!aOut && !bOut && c.ticks < MAX_TICKS) {
            return false;
        }
        ClashDomain loser;
        if (aOut != bOut) {
            loser = aOut ? c.a : c.b;
        } else {
            loser = pa < pb ? c.a : c.b;
        }
        ClashDomain winner = loser == c.a ? c.b : c.a;
        lose(loser, winner);
        return true;
    }

    private static void lose(ClashDomain loser, ClashDomain winner) {
        ServerWorld w = loser.clashWorld();
        ServerPlayerEntity lo = loser.clashOwner();
        ServerPlayerEntity wo = winner.clashOwner();
        loser.collapse();
        lo.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 3, false, true));
        lo.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 160, 1, false, true));
        Dmg.hit(wo, lo, 12.0);
        Vec3d at = loser.clashCenter();
        Ink.ring(w, at, loser.clashRadius() * 0.5, Ink.WHITE, 0.9F);
        Ink.splatter(w, at, loser.clashRadius() * 0.6, 40, Ink.GOLD);
        Fx.sound(w, at.x, at.y, at.z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.MASTER, 4.0F, 0.5F);
        lo.sendMessage(Text.translatable("sukuna.clash.lost"), false);
        wo.sendMessage(Text.translatable("sukuna.clash.won"), false);
    }
}
