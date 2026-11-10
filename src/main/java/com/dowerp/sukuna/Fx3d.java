package com.dowerp.sukuna;

import com.dowerp.sukuna.entity.TechniqueOrbEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

/** Помощники для 3D-эффектов (GeckoLib-сущности без ИИ, которыми управляют серверные задачи). */
public final class Fx3d {
    private Fx3d() {
    }

    public static TechniqueOrbEntity spawn(ServerWorld w, EntityType<TechniqueOrbEntity> type, Vec3d feet, float yaw, int life) {
        TechniqueOrbEntity e = type.create(w, SpawnReason.TRIGGERED);
        if (e == null) {
            return null;
        }
        e.refreshPositionAndAngles(feet.x, feet.y, feet.z, yaw, 0.0F);
        e.setLife(life);
        w.spawnEntity(e);
        return e;
    }

    public static void place(TechniqueOrbEntity e, Vec3d feet, float yaw) {
        if (e != null && !e.isRemoved()) {
            e.moveTo(feet.x, feet.y, feet.z, yaw);
        }
    }

    public static void drop(TechniqueOrbEntity e) {
        if (e != null && !e.isRemoved()) {
            e.discard();
        }
    }

    /** Курс (yaw Minecraft) от точки from к точке to. */
    public static float yawTo(Vec3d from, Vec3d to) {
        return (float) Math.toDegrees(Math.atan2(-(to.x - from.x), to.z - from.z));
    }

    public static boolean solid(ServerWorld w, Vec3d p) {
        BlockPos bp = BlockPos.ofFloored(p.x, p.y, p.z);
        return w.isPosLoaded(bp) && !w.getBlockState(bp).getCollisionShape(w, bp).isEmpty();
    }

    /**
     * Летящий 3D-импульс: модель движется по прямой, цели в радиусе попадания получают onHit (один раз за вызов
     * решает вызывающий). Останавливается на твёрдом блоке, если pierce=false.
     */
    public static void pulse(ServerWorld w, EntityType<TechniqueOrbEntity> type, ServerPlayerEntity owner, Vec3d start, Vec3d dir,
                             double speed, int ticks, double hitRadius, boolean pierce, Consumer<LivingEntity> onHit) {
        final TechniqueOrbEntity e = spawn(w, type, start, owner.getYaw(), ticks + 10);
        final Vec3d step = dir.normalize().multiply(speed);
        Tasks.add(new Task() {
            private int t = 0;
            private Vec3d pos = start;

            @Override
            public boolean tick() {
                if (owner.isRemoved() || e == null || e.isRemoved()) {
                    drop(e);
                    return true;
                }
                pos = pos.add(step);
                t++;
                if (!pierce && solid(w, pos)) {
                    drop(e);
                    return true;
                }
                place(e, pos.subtract(0.0, hitRadius * 0.5, 0.0), owner.getYaw());
                for (LivingEntity x : w.getEntitiesByClass(LivingEntity.class, new Box(pos, pos).expand(hitRadius), q -> Aim.valid(owner, q))) {
                    onHit.accept(x);
                }
                if (t >= ticks) {
                    drop(e);
                    return true;
                }
                return false;
            }
        });
    }
}
