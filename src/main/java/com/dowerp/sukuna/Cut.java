package com.dowerp.sukuna;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import java.util.Map;

public final class Cut {
    private Cut() {
    }

    public static boolean allowed(ServerWorld w, BlockPos pos, BlockState s) {
        if (!SukunaMod.config.blockDamage || s.isAir()) {
            return false;
        }
        if (s.isLiquid()) {
            return true;
        }
        if (s.hasBlockEntity() && !SukunaMod.config.cutBlockEntities) {
            return false;
        }
        float h = s.getHardness(w, pos);
        if (h < 0.0F) {
            return false;
        }
        return SukunaMod.config.breakHardBlocks || h <= SukunaMod.config.maxHardness;
    }

    public static boolean remove(ServerWorld w, BlockPos pos, Map<BlockPos, BlockState> record) {
        if (!w.isInBuildLimit(pos) || !w.isPosLoaded(pos)) {
            return false;
        }
        if (DomainShield.isProtected(pos)) {
            return false;
        }
        BlockState s = w.getBlockState(pos);
        if (!allowed(w, pos, s)) {
            return false;
        }
        if (record != null) {
            record.putIfAbsent(pos.toImmutable(), s);
        }
        w.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
        return true;
    }

    public static boolean removeSafe(ServerWorld w, BlockPos pos, net.minecraft.entity.Entity owner) {
        if (owner != null && pos.getY() < MathHelper.floor(owner.getY())) {
            double dx = pos.getX() + 0.5 - owner.getX();
            double dz = pos.getZ() + 0.5 - owner.getZ();
            if (dx * dx + dz * dz <= 9.0) {
                return false;
            }
        }
        return remove(w, pos, null);
    }

    public static int surfaceY(ServerWorld w, int x, int z, int refY) {
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int y = refY + 6; y >= refY - 10; y--) {
            m.set(x, y, z);
            if (!w.isPosLoaded(m)) {
                return Integer.MIN_VALUE;
            }
            BlockState s = w.getBlockState(m);
            if (s.isAir() || s.isLiquid()) {
                continue;
            }
            m.set(x, y + 1, z);
            BlockState up = w.getBlockState(m);
            if (up.isAir() || up.isReplaceable() || up.isLiquid()) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static void groove(ServerWorld w, double x0, double z0, double angle, double length, int refY, int depth, Map<BlockPos, BlockState> record) {
        if (!SukunaMod.config.blockDamage) {
            return;
        }
        double dx = Math.cos(angle);
        double dz = Math.sin(angle);
        for (double t = -length / 2.0; t <= length / 2.0; t += 0.5) {
            int bx = MathHelper.floor(x0 + dx * t);
            int bz = MathHelper.floor(z0 + dz * t);
            int y = surfaceY(w, bx, bz, refY);
            if (y == Integer.MIN_VALUE) {
                continue;
            }
            for (int d = 0; d < depth; d++) {
                remove(w, new BlockPos(bx, y - d, bz), record);
            }
        }
    }
}
