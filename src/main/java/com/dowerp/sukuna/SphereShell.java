package com.dowerp.sukuna;

import net.minecraft.sound.SoundEvents;
import net.minecraft.sound.SoundCategory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class SphereShell implements Task {
    private static final List<SphereShell> LIVE = new ArrayList<>();
    private static final int BUILD = 0;
    private static final int HOLD = 1;
    private static final int RESTORE = 2;

    private final ServerWorld w;
    private final Vec3d center;
    private final int radius;
    private final DomainShell.Style style;
    private int hold;
    private int phase = BUILD;
    private int index;
    private int tick;
    private List<ShrinePlan.Placement> plan;
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private Iterator<Map.Entry<BlockPos, BlockState>> restoreIt;

    private SphereShell(ServerWorld w, Vec3d center, int radius, int holdTicks, DomainShell.Style style) {
        this.w = w;
        this.center = center;
        this.radius = radius;
        this.hold = holdTicks;
        this.style = style;
    }

    public static void start(ServerWorld w, Vec3d center, int radius, int holdTicks, DomainShell.Style style) {
        SphereShell shell = new SphereShell(w, center, radius, holdTicks, style);
        LIVE.add(shell);
        Tasks.add(shell);
    }

    public static void restoreAll() {
        for (SphereShell s : new ArrayList<>(LIVE)) {
            for (Map.Entry<BlockPos, BlockState> e : s.originals.entrySet()) {
                s.w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
                DomainShield.remove(e.getKey());
            }
            s.originals.clear();
        }
        LIVE.clear();
    }

    @Override
    public boolean tick() {
        switch (phase) {
            case BUILD -> {
                if (plan == null) {
                    plan = SukunaMod.config.blockDamage ? DomainShell.plan(center, radius, style, MathHelper.floor(center.y) - 5) : new ArrayList<>();
                }
                int placed = 0;
                while (index < plan.size() && placed < 450) {
                    ShrinePlan.Placement pl = plan.get(index++);
                    BlockPos bp = pl.pos();
                    if (!w.isPosLoaded(bp) || !w.isInBuildLimit(bp)) continue;
                    BlockState cur = w.getBlockState(bp);
                    if (cur.equals(pl.state())) continue;
                    if (cur.hasBlockEntity() || cur.getHardness(w, bp) < 0.0F) continue;
                    if (pl.state().isOf(Blocks.LIGHT) && !(cur.isAir() || cur.isReplaceable())) continue;
                    originals.putIfAbsent(bp.toImmutable(), cur);
                    w.setBlockState(bp, pl.state(), Block.NOTIFY_LISTENERS);
                    if (!pl.state().isAir()) DomainShield.add(bp);
                    placed++;
                }
                if (index >= plan.size()) {
                    phase = HOLD;
                    DomainMusic.start(w, center, DomainMusic.YUJI);
                }
                return false;
            }
            case HOLD -> {
                ThreadLocalRandom r = ThreadLocalRandom.current();
                if (tick % 80 == 0) {
                    Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 1.6F, 0.8F);
                    Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.MASTER, 1.2F, 1.2F);
                }
                if (tick++ % 3 == 0) {
                    for (int i = 0; i < 8; i++) {
                        Vec3d d = new Vec3d(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().multiply(r.nextDouble() * (radius - 1));
                        Ink.dot(w, center.add(d), i % 2 == 0 ? Ink.WHITE : Ink.BLUE, 0.5F + r.nextFloat() * 0.5F);
                    }
                }
                if (--hold <= 0) {
                    phase = RESTORE;
                    DomainMusic.stop(w, DomainMusic.YUJI);
                }
                return false;
            }
            default -> {
                if (restoreIt == null) {
                    restoreIt = new ArrayList<>(originals.entrySet()).iterator();
                }
                int n = 0;
                while (restoreIt.hasNext() && n < 700) {
                    Map.Entry<BlockPos, BlockState> e = restoreIt.next();
                    w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
                    DomainShield.remove(e.getKey());
                    n++;
                }
                if (!restoreIt.hasNext()) {
                    originals.clear();
                    LIVE.remove(this);
                    return true;
                }
                return false;
            }
        }
    }
}
