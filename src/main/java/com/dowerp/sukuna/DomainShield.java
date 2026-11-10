package com.dowerp.sukuna;

import net.minecraft.util.math.BlockPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class DomainShield {
    private static final Set<Long> PROTECTED = ConcurrentHashMap.newKeySet();

    private DomainShield() {
    }

    public static void add(BlockPos p) {
        PROTECTED.add(p.asLong());
    }

    public static void remove(BlockPos p) {
        PROTECTED.remove(p.asLong());
    }

    public static boolean isProtected(BlockPos p) {
        return !PROTECTED.isEmpty() && PROTECTED.contains(p.asLong());
    }

    public static void clear() {
        PROTECTED.clear();
    }
}
