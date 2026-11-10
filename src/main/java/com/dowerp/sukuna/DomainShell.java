package com.dowerp.sukuna;

import com.dowerp.sukuna.block.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class DomainShell {
    public enum Style { SUKUNA, VOID, YUJI }

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final int CITY_PERIOD = 7;

    private DomainShell() {
    }

    public static List<ShrinePlan.Placement> plan(Vec3d center, int radius, Style style, int floorY) {
        int cx = MathHelper.floor(center.x);
        int cy = MathHelper.floor(center.y);
        int cz = MathHelper.floor(center.z);
        List<ShrinePlan.Placement> carve = new ArrayList<>();
        List<ShrinePlan.Placement> floor = new ArrayList<>();
        List<ShrinePlan.Placement> shell = new ArrayList<>();
        List<ShrinePlan.Placement> decor = new ArrayList<>();

        double inner = radius - 1.5;
        for (int y = floorY - 1 - cy; y <= radius; y++) {
            int absY = cy + y;
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double d = Math.sqrt(x * x + y * y + z * z);
                    if (d > radius + 0.5) continue;
                    BlockPos pos = new BlockPos(cx + x, absY, cz + z);
                    int h = (x * 73856093) ^ (absY * 19349663) ^ (z * 83492791);
                    if (d > inner) {
                        if (absY >= floorY) {
                            shell.add(new ShrinePlan.Placement(pos, shellBlock(style, d <= radius - 0.5, h, absY - floorY, radius * 2)));
                        }
                    } else if (absY > floorY) {
                        carve.add(new ShrinePlan.Placement(pos, AIR));
                    } else if (absY >= floorY - 1) {
                        floor.add(new ShrinePlan.Placement(pos, floorBlock(style, x, z, absY == floorY, h)));
                    }
                }
            }
        }
        carve.sort((a, b) -> Integer.compare(b.pos().getY(), a.pos().getY()));
        shell.sort((a, b) -> Integer.compare(a.pos().getY(), b.pos().getY()));

        if (style == Style.YUJI) {
            city(decor, cx, cy, cz, radius, floorY);
        }

        List<ShrinePlan.Placement> out = new ArrayList<>(carve.size() + floor.size() + shell.size() + decor.size() + 64);
        out.addAll(carve);
        out.addAll(floor);
        out.addAll(shell);
        out.addAll(decor);

        int level = switch (style) {
            case SUKUNA -> 11;
            case VOID -> 12;
            case YUJI -> 15;
        };
        BlockState light = Blocks.LIGHT.getDefaultState().with(LightBlock.LEVEL_15, level);
        int inside = radius - 3;
        for (int y = -inside; y <= inside; y += 6) {
            for (int x = -inside; x <= inside; x += 6) {
                for (int z = -inside; z <= inside; z += 6) {
                    if (x * x + y * y + z * z <= inside * inside && cy + y > floorY + 1) {
                        out.add(new ShrinePlan.Placement(new BlockPos(cx + x, cy + y, cz + z), light));
                    }
                }
            }
        }
        return out;
    }

    /** Чёрный зеркальный пол и очищенная арена вокруг открытой территории Сукуны. */
    public static List<ShrinePlan.Placement> openArena(BlockPos origin, int radius, int clearHeight) {
        List<ShrinePlan.Placement> out = new ArrayList<>();
        BlockState mirror = ModBlocks.MIRROR_FLOOR.getDefaultState();
        for (int h = clearHeight; h >= 1; h--) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x * x + z * z <= radius * radius) {
                        out.add(new ShrinePlan.Placement(origin.add(x, h, z), AIR));
                    }
                }
            }
        }
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                if (x * x + z * z <= radius * radius) {
                    out.add(new ShrinePlan.Placement(origin.add(x, 0, z), mirror));
                    out.add(new ShrinePlan.Placement(origin.add(x, -1, z), mirror));
                }
            }
        }
        return out;
    }

    private static BlockState shellBlock(Style style, boolean innerLayer, int hash, int heightFromFloor, int diameter) {
        int r = Math.floorMod(hash, 100);
        return switch (style) {
            case SUKUNA -> innerLayer
                ? ModBlocks.SUKUNA_WALL[Math.floorMod(hash >> 3, ModBlocks.SUKUNA_WALL.length)].getDefaultState()
                : Blocks.BLACK_CONCRETE.getDefaultState();
            case VOID -> innerLayer
                ? ModBlocks.VOID_WALL[Math.floorMod(hash >> 3, ModBlocks.VOID_WALL.length)].getDefaultState()
                : Blocks.BLACK_CONCRETE.getDefaultState();
            case YUJI -> {
                if (!innerLayer) yield Blocks.BLACK_CONCRETE.getDefaultState();
                int band = Math.min(ModBlocks.CITY_SKY.length - 1, heightFromFloor * ModBlocks.CITY_SKY.length / Math.max(1, diameter / 2 + 4));
                yield ModBlocks.CITY_SKY[band].getDefaultState();
            }
        };
    }

    private static BlockState floorBlock(Style style, int x, int z, boolean top, int hash) {
        return switch (style) {
            case SUKUNA -> ModBlocks.MIRROR_FLOOR.getDefaultState();
            case VOID -> ModBlocks.VOID_FLOOR.getDefaultState();
            case YUJI -> {
                if (!top) yield Blocks.GRAY_CONCRETE.getDefaultState();
                int mx = Math.floorMod(x + 1, CITY_PERIOD);
                int mz = Math.floorMod(z + 1, CITY_PERIOD);
                boolean streetX = mx <= 2;
                boolean streetZ = mz <= 2;
                if (!streetX && !streetZ) yield Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
                if (streetX && streetZ) yield Blocks.BLACK_CONCRETE.getDefaultState();
                boolean line = streetX ? (mx == 1 && Math.floorMod(z, 2) == 0) : (mz == 1 && Math.floorMod(x, 2) == 0);
                yield (line ? Blocks.WHITE_CONCRETE : Blocks.BLACK_CONCRETE).getDefaultState();
            }
        };
    }

    /** Ночной город: сетка улиц, дома с окнами разной высоты. Владелец стоит на перекрёстке. */
    private static void city(List<ShrinePlan.Placement> out, int cx, int cy, int cz, int radius, int floorY) {
        double inner = radius - 2.0;
        BlockState[] walls = {
            Blocks.LIGHT_GRAY_CONCRETE.getDefaultState(), Blocks.GRAY_CONCRETE.getDefaultState(),
            Blocks.WHITE_CONCRETE.getDefaultState(), Blocks.STONE_BRICKS.getDefaultState()
        };
        BlockState glass = Blocks.LIGHT_BLUE_STAINED_GLASS.getDefaultState();
        BlockState lit = Blocks.GLOWSTONE.getDefaultState();
        BlockState roof = Blocks.GRAY_CONCRETE.getDefaultState();
        int span = radius + CITY_PERIOD;
        for (int lx = Math.floorDiv(-span + 1, CITY_PERIOD); lx <= Math.floorDiv(span + 1, CITY_PERIOD); lx++) {
            for (int lz = Math.floorDiv(-span + 1, CITY_PERIOD); lz <= Math.floorDiv(span + 1, CITY_PERIOD); lz++) {
                int hash = lx * 92837111 ^ lz * 689287499;
                int x0 = lx * CITY_PERIOD + 2;
                int z0 = lz * CITY_PERIOD + 2;
                int height = 5 + Math.floorMod(hash >> 4, 9);
                BlockState wall = walls[Math.floorMod(hash >> 9, walls.length)];
                for (int x = x0; x < x0 + 4; x++) {
                    for (int z = z0; z < z0 + 4; z++) {
                        double rho2 = (double) x * x + (double) z * z;
                        if (rho2 >= inner * inner) continue;
                        double top = cy + Math.sqrt(inner * inner - rho2);
                        int maxH = (int) Math.floor(top - floorY) - 2;
                        int h = Math.min(height, maxH);
                        if (h < 3) continue;
                        boolean edge = x == x0 || x == x0 + 3 || z == z0 || z == z0 + 3;
                        boolean corner = (x == x0 || x == x0 + 3) && (z == z0 || z == z0 + 3);
                        for (int k = 1; k <= h; k++) {
                            BlockState st;
                            if (k == h) {
                                st = roof;
                            } else if (!edge) {
                                continue;
                            } else if (!corner && k % 3 == 2) {
                                st = Math.floorMod(hash + x * 31 + z * 17 + k * 7, 5) == 0 ? lit : glass;
                            } else {
                                st = wall;
                            }
                            out.add(new ShrinePlan.Placement(new BlockPos(cx + x, floorY + k, cz + z), st));
                        }
                    }
                }
            }
        }
    }
}
