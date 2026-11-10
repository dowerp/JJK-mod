package com.dowerp.sukuna;

import com.dowerp.sukuna.tasks.GojoTasks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class VoidDomainTask implements Task, ClashDomain {
    private static final Set<UUID> ACTIVE = new HashSet<>();
    private static final List<VoidDomainTask> LIVE = new ArrayList<>();
    private static final int BUILD = 0;
    private static final int HOLD = 1;
    private static final int RESTORE = 2;
    private static final int RADIUS = 16;
    private static final int DURATION = 200;

    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final double power;
    private final Vec3d center;
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private final Map<UUID, LivingEntity> victims = new HashMap<>();
    private final Map<UUID, Vec3d> anchors = new HashMap<>();
    private List<ShrinePlan.Placement> plan;
    private int index;
    private int phase = BUILD;
    private int tick;
    private Iterator<Map.Entry<BlockPos, BlockState>> restoreIt;

    private VoidDomainTask(ServerPlayerEntity owner, SukunaState s) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.power = s.power();
        this.center = new Vec3d(owner.getX(), owner.getY() + 4.0, owner.getZ());
    }

    public static boolean isActive(ServerPlayerEntity p) {
        return ACTIVE.contains(p.getUuid());
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s) {
        if (!ACTIVE.add(p.getUuid())) {
            return false;
        }
        s.busyUntil = Fx.world(p).getTime() + 60;
        VoidDomainTask task = new VoidDomainTask(p, s);
        LIVE.add(task);
        DomainClash.register(task);
        Tasks.add(task);
        p.sendMessage(Text.translatable("sukuna.msg.void_domain"), false);
        Fx.sound(Fx.world(p), p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 2.5F, 0.7F);
        return true;
    }

    public static void restoreAll() {
        for (VoidDomainTask t : new ArrayList<>(LIVE)) {
            t.releaseAll();
            for (Map.Entry<BlockPos, BlockState> e : t.originals.entrySet()) {
                t.w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
                DomainShield.remove(e.getKey());
            }
            t.originals.clear();
            ACTIVE.remove(t.owner.getUuid());
        }
        LIVE.clear();
    }

    @Override
    public ServerPlayerEntity clashOwner() {
        return owner;
    }

    @Override
    public ServerWorld clashWorld() {
        return w;
    }

    @Override
    public Vec3d clashCenter() {
        return center;
    }

    @Override
    public double clashRadius() {
        return RADIUS;
    }

    @Override
    public boolean clashActive() {
        return phase == HOLD;
    }

    @Override
    public void collapse() {
        releaseAll();
        phase = RESTORE;
        DomainMusic.stop(w, DomainMusic.GOJO);
    }

    @Override
    public boolean tick() {
        if (phase != RESTORE && (owner.isRemoved() || !owner.isAlive())) {
            releaseAll();
            phase = RESTORE;
        DomainMusic.stop(w, DomainMusic.GOJO);
        }
        return switch (phase) {
            case BUILD -> build();
            case HOLD -> hold();
            default -> restore();
        };
    }

    private boolean build() {
        if (plan == null) {
            plan = SukunaMod.config.blockDamage ? DomainShell.plan(center, RADIUS, DomainShell.Style.VOID, MathHelper.floor(center.y) - 5) : new ArrayList<>();
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
        Ink.splatter(w, center, RADIUS * 1.2, 6, Ink.WHITE);
        tick++;
        if (index >= plan.size()) {
            phase = HOLD;
            DomainMusic.start(w, center, DomainMusic.GOJO);
            tick = 0;
        }
        return false;
    }

    private boolean hold() {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        double r2 = (double) RADIUS * RADIUS;

        if (tick % 5 == 0) {
            for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(RADIUS), x -> Aim.valid(owner, x) && !Counters.nullifies(x))) {
                if (center.squaredDistanceTo(e.getX(), e.getY(), e.getZ()) <= r2 && !victims.containsKey(e.getUuid())) {
                    victims.put(e.getUuid(), e);
                    anchors.put(e.getUuid(), new Vec3d(e.getX(), e.getY(), e.getZ()));
                }
            }
        }

        Iterator<Map.Entry<UUID, LivingEntity>> it = victims.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, LivingEntity> en = it.next();
            LivingEntity e = en.getValue();
            if (!e.isAlive() || e.isRemoved()) {
                GojoTasks.release(e);
                it.remove();
                continue;
            }
            GojoTasks.hold(e, anchors.get(en.getKey()));
            if (tick % 20 == 0) {
                Dmg.hit(owner, e, 5.0 * power);
                e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, false, false));
                if (e instanceof ServerPlayerEntity) {
                    e.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 100, 0, false, false));
                }
                Ink.impact(w, new Vec3d(e.getX(), e.getY() + e.getHeight() * 0.6, e.getZ()), new Vec3d(0, 1, 0), Ink.WHITE);
            }
        }

        if (tick % 3 == 0) {
            for (int i = 0; i < 10; i++) {
                Vec3d d = new Vec3d(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().multiply(r.nextDouble() * (RADIUS - 1));
                Ink.dot(w, center.add(d), i % 3 == 0 ? Ink.PURPLE : i % 2 == 0 ? Ink.WHITE : Ink.BLUE, 0.5F + r.nextFloat() * 0.6F);
            }
        }
        if (tick % 10 == 0) {
            Vec3d a = center.add(new Vec3d(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().multiply(r.nextDouble() * (RADIUS - 4)));
            Vec3d dir = new Vec3d(r.nextDouble() - 0.5, r.nextDouble() - 0.5, r.nextDouble() - 0.5).normalize().multiply(5.0);
            Ink.stroke(w, a, a.add(dir), Ink.WHITE, Ink.BLUE, 0.4F, true);
        }
        if (tick % 40 == 0) {
            Ink.ring(w, new Vec3d(center.x, center.y - 3.8, center.z), 2.0 + (tick % 120) * 0.1, Ink.PURPLE, 0.5F);
        }
        for (int i = 0; i < 3; i++) {
            double a = r.nextDouble() * Math.PI * 2.0;
            double h = (r.nextDouble() - 0.35) * (RADIUS - 3);
            double far = RADIUS - 2.5;
            double near = 2.0 + r.nextDouble() * 5.0;
            Vec3d from = new Vec3d(center.x + Math.cos(a) * far, center.y + h, center.z + Math.sin(a) * far);
            Vec3d to = new Vec3d(center.x + Math.cos(a) * near, center.y + h * 0.4, center.z + Math.sin(a) * near);
            double along = ((tick * 3 + i * 7) % 20) / 20.0;
            Vec3d s0 = from.add(to.subtract(from).multiply(along));
            Vec3d s1 = from.add(to.subtract(from).multiply(Math.min(1.0, along + 0.18)));
            Ink.stroke(w, s0, s1, i % 2 == 0 ? Ink.WHITE : Ink.BLUE, Ink.DEEP_PURPLE, 0.35F, true);
        }
        if (tick % 90 == 0) {
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_AMBIENT, SoundCategory.MASTER, 3.0F, 0.5F);
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_END_PORTAL_SPAWN, SoundCategory.MASTER, 0.6F, 1.8F);
        }
        if (tick % 4 == 0) {
            Fx.far(w, net.minecraft.particle.ParticleTypes.END_ROD, center.x, center.y, center.z, 4, RADIUS * 0.6, RADIUS * 0.4, RADIUS * 0.6, 0.01);
        }

        tick++;
        if (tick >= DURATION) {
            releaseAll();
            phase = RESTORE;
        DomainMusic.stop(w, DomainMusic.GOJO);
        }
        return false;
    }

    private void releaseAll() {
        for (LivingEntity e : victims.values()) {
            GojoTasks.release(e);
        }
        victims.clear();
        anchors.clear();
    }

    private boolean restore() {
        if (restoreIt == null) {
            restoreIt = new ArrayList<>(originals.entrySet()).iterator();
        }
        int n = 0;
        while (restoreIt.hasNext() && n < 700) {
            Map.Entry<BlockPos, BlockState> en = restoreIt.next();
            w.setBlockState(en.getKey(), en.getValue(), Block.NOTIFY_LISTENERS);
            DomainShield.remove(en.getKey());
            n++;
        }
        if (!restoreIt.hasNext()) {
            ACTIVE.remove(owner.getUuid());
            LIVE.remove(this);
            DomainClash.unregister(this);
            return true;
        }
        return false;
    }
}
