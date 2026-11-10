package com.dowerp.sukuna;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

public interface ClashDomain {
    ServerPlayerEntity clashOwner();

    ServerWorld clashWorld();

    Vec3d clashCenter();

    double clashRadius();

    boolean clashActive();

    void collapse();
}
