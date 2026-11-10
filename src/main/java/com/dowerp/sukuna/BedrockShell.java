package com.dowerp.sukuna;

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

public final class BedrockShell implements Task {
    private static final List<BedrockShell> LIVE = new ArrayList<>();
    private static final int BUILD = 0;
    private static final int HOLD = 1;
    private static final int RESTORE = 2;

    private final ServerWorld w;
    private final Vec3d center;
    private final int radius;
    private int hold;
    private int phase = BUILD;
    private int index;
    private List<BlockPos> plan;
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private Iterator<Map.Entry<BlockPos, BlockState>> restoreIt;

    private BedrockShell(ServerWorld w, Vec3d center, int radius, int holdTicks) {
        this.w = w;
        this.center = center;
        this.radius = radius;
        this.hold = holdTicks;
    }

    public static void start(ServerWorld w, Vec3d center, int radius, int holdTicks) {
        BedrockShell shell = new BedrockShell(w, center, radius, holdTicks);
        LIVE.add(shell);
        Tasks.add(shell);
    }

    public static void restoreAll() {
        for (BedrockShell s : new ArrayList<>(LIVE)) {
            for (Map.Entry<BlockPos, BlockState> e : s.originals.entrySet()) {
                s.w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
            }
            s.originals.clear();
        }
        LIVE.clear();
    }

    public static List<BlockPos> sphere(Vec3d center, int radius) {
        List<BlockPos> out = new ArrayList<>();
        int cx = MathHelper.floor(center.x);
        int cy = MathHelper.floor(center.y);
        int cz = MathHelper.floor(center.z);
        for (int y = -radius; y <= radius; y++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d > radius - 0.5 && d <= radius + 0.5) {
                        out.add(new BlockPos(cx + x, cy + y, cz + z));
                    }
                }
            }
        }
        out.sort((a, b) -> Integer.compare(a.getY(), b.getY()));
        return out;
    }

    @Override
    public boolean tick() {
        switch (phase) {
            case BUILD -> {
                if (plan == null) {
                    plan = SukunaMod.config.blockDamage ? sphere(center, radius) : new ArrayList<>();
                }
                BlockState bedrock = Blocks.BEDROCK.getDefaultState();
                int placed = 0;
                while (index < plan.size() && placed < 400) {
                    BlockPos bp = plan.get(index++);
                    if (!w.isPosLoaded(bp) || !w.isInBuildLimit(bp)) continue;
                    BlockState cur = w.getBlockState(bp);
                    if (cur.hasBlockEntity() || cur.getHardness(w, bp) < 0.0F) continue;
                    if (!(cur.isAir() || cur.isReplaceable() || !cur.getFluidState().isEmpty())) continue;
                    originals.putIfAbsent(bp.toImmutable(), cur);
                    w.setBlockState(bp, bedrock, Block.NOTIFY_LISTENERS);
                    placed++;
                }
                if (index >= plan.size()) {
                    phase = HOLD;
                }
                return false;
            }
            case HOLD -> {
                if (--hold <= 0) {
                    phase = RESTORE;
                }
                return false;
            }
            default -> {
                if (restoreIt == null) {
                    restoreIt = new ArrayList<>(originals.entrySet()).iterator();
                }
                int n = 0;
                while (restoreIt.hasNext() && n < 600) {
                    Map.Entry<BlockPos, BlockState> e = restoreIt.next();
                    w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
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
