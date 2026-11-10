package com.dowerp.sukuna;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.SkullBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ShrinePlan {
    record Placement(BlockPos pos, BlockState state) {
    }

    private final Map<Long, BlockState> blocks = new HashMap<>();
    private final BlockPos origin;
    private final int rot;

    ShrinePlan(BlockPos origin, int rot) {
        this.origin = origin;
        this.rot = rot;
        build();
    }

    private BlockRotation rotation() {
        return switch (rot) {
            case 1 -> BlockRotation.CLOCKWISE_90;
            case 2 -> BlockRotation.CLOCKWISE_180;
            case 3 -> BlockRotation.COUNTERCLOCKWISE_90;
            default -> BlockRotation.NONE;
        };
    }

    private void set(int x, int y, int z, BlockState s) {
        int rx;
        int rz;
        switch (rot) {
            case 1 -> {
                rx = -z;
                rz = x;
            }
            case 2 -> {
                rx = -x;
                rz = -z;
            }
            case 3 -> {
                rx = z;
                rz = -x;
            }
            default -> {
                rx = x;
                rz = z;
            }
        }
        BlockState st = rot == 0 ? s : s.rotate(rotation());
        blocks.put(BlockPos.asLong(origin.getX() + rx, origin.getY() + y, origin.getZ() + rz), st);
    }

    private static BlockState stairs(Block block, Direction facing) {
        return block.getDefaultState().with(StairsBlock.FACING, facing);
    }

    private void roofTier(int y, int hs) {
        Block stair = Blocks.BLACKSTONE_STAIRS;
        BlockState solid = Blocks.BLACKSTONE.getDefaultState();
        for (int i = -hs; i <= hs; i++) {
            set(i, y, -hs, stairs(stair, Direction.SOUTH));
            set(i, y, hs, stairs(stair, Direction.NORTH));
            set(-hs, y, i, stairs(stair, Direction.EAST));
            set(hs, y, i, stairs(stair, Direction.WEST));
        }
        for (int x = -hs + 1; x <= hs - 1; x++) {
            for (int z = -hs + 1; z <= hs - 1; z++) {
                set(x, y, z, solid);
            }
        }
    }

    private void build() {
        BlockState brick = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState cracked = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
        BlockState red = Blocks.RED_NETHER_BRICKS.getDefaultState();
        BlockState polished = Blocks.POLISHED_BLACKSTONE.getDefaultState();
        BlockState stem = Blocks.STRIPPED_CRIMSON_STEM.getDefaultState();
        BlockState planks = Blocks.CRIMSON_PLANKS.getDefaultState();
        BlockState bone = Blocks.BONE_BLOCK.getDefaultState();
        BlockState redConcrete = Blocks.RED_CONCRETE.getDefaultState();
        BlockState blackConcrete = Blocks.BLACK_CONCRETE.getDefaultState();
        BlockState crying = Blocks.CRYING_OBSIDIAN.getDefaultState();
        BlockState slab = Blocks.BLACKSTONE_SLAB.getDefaultState();
        BlockState campfire = Blocks.SOUL_CAMPFIRE.getDefaultState();
        BlockState lantern = Blocks.SOUL_LANTERN.getDefaultState();
        BlockState lanternHanging = lantern.with(LanternBlock.HANGING, true);
        BlockState skull = Blocks.SKELETON_SKULL.getDefaultState();
        BlockState witherSkull = Blocks.WITHER_SKELETON_SKULL.getDefaultState();

        for (int x = -10; x <= 10; x++) {
            for (int z = -10; z <= 10; z++) {
                int edge = Math.max(Math.abs(x), Math.abs(z));
                BlockState floor;
                if (edge == 10 || edge == 7) {
                    floor = red;
                } else if (((x * 31 + z * 17) & 7) == 0) {
                    floor = cracked;
                } else {
                    floor = brick;
                }
                set(x, -1, z, floor);
            }
        }
        for (int x = -6; x <= 6; x++) {
            for (int z = -6; z <= 6; z++) {
                set(x, 0, z, polished);
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                set(x, 0, z, crying);
            }
        }

        int[][] pillars = {{-5, -5}, {5, -5}, {-5, 5}, {5, 5}, {-5, 0}, {5, 0}, {0, 5}};
        for (int[] p : pillars) {
            for (int y = 1; y <= 6; y++) {
                set(p[0], y, p[1], stem);
            }
        }
        for (int i = -5; i <= 5; i++) {
            set(i, 7, -5, planks);
            set(i, 7, 5, planks);
            set(-5, 7, i, planks);
            set(5, 7, i, planks);
            set(i, 7, 0, planks);
            set(0, 7, i, planks);
        }
        set(0, 6, 0, lanternHanging);
        set(3, 6, 0, lanternHanging);
        set(-3, 6, 0, lanternHanging);
        set(0, 6, 3, lanternHanging);
        set(0, 6, -3, lanternHanging);

        for (int sx : new int[] {-3, 3}) {
            for (int sz : new int[] {-3, 3}) {
                set(sx, 1, sz, campfire);
            }
        }

        roofTier(8, 7);
        roofTier(9, 5);
        roofTier(10, 3);
        roofTier(11, 1);
        set(0, 12, 0, bone);
        set(0, 13, 0, bone);
        set(0, 14, 0, witherSkull);

        for (int sx : new int[] {-8, 8}) {
            for (int sz : new int[] {-8, 8}) {
                set(sx, 8, sz, slab);
                set(sx, 9, sz, bone);
                set(sx, 10, sz, bone);
                set(sx, 11, sz, bone);
                set(sx, 12, sz, witherSkull.with(SkullBlock.ROTATION, 0));
            }
        }
        for (int sx : new int[] {-7, 7}) {
            for (int sz : new int[] {-7, 7}) {
                set(sx, 9, sz, skull.with(SkullBlock.ROTATION, sx > 0 ? 4 : 12));
            }
        }

        for (int sx : new int[] {-3, 3}) {
            for (int y = 0; y <= 5; y++) {
                set(sx, y, -10, y == 0 ? blackConcrete : redConcrete);
            }
        }
        for (int x = -3; x <= 3; x++) {
            set(x, 4, -10, redConcrete);
        }
        for (int x = -5; x <= 5; x++) {
            set(x, 6, -10, blackConcrete);
        }
        set(-6, 6, -10, slab);
        set(6, 6, -10, slab);
        for (int x = -4; x <= 4; x += 2) {
            set(x, 7, -10, skull.with(SkullBlock.ROTATION, 8));
        }
        set(-1, 3, -10, lanternHanging);
        set(1, 3, -10, lanternHanging);

        for (int sx : new int[] {-9, 9}) {
            for (int sz : new int[] {-9, 9}) {
                set(sx, 0, sz, campfire);
            }
            set(sx, 0, 0, lantern);
        }
    }

    List<Placement> placements() {
        List<Placement> list = new ArrayList<>();
        for (int y = -1; y <= 15; y++) {
            for (int dx = -10; dx <= 10; dx++) {
                for (int dz = -10; dz <= 10; dz++) {
                    BlockPos p = new BlockPos(origin.getX() + dx, origin.getY() + y, origin.getZ() + dz);
                    BlockState s = blocks.get(p.asLong());
                    if (s == null) {
                        if (y < 0) {
                            continue;
                        }
                        s = Blocks.AIR.getDefaultState();
                    }
                    list.add(new Placement(p, s));
                }
            }
        }
        return list;
    }
}
