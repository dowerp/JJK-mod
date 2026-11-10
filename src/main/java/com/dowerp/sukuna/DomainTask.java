package com.dowerp.sukuna;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class DomainTask implements Task, ClashDomain {
    private static final List<DomainTask> ALL = new ArrayList<>();
    private static final int CAST = 0;
    private static final int BUILD = 1;
    private static final int ACTIVE = 2;
    private static final int RESTORE = 3;

    private final ServerWorld w;
    private final ServerPlayerEntity owner;
    private final SukunaState s;
    private final BlockPos origin;
    private final Vec3d center;
    private final int radius;
    private final boolean open;
    private final Vec3d shellCenter;
    private int shellCount;
    private final int duration;
    private final double power;
    private final int rot;
    private final Map<BlockPos, BlockState> originals = new LinkedHashMap<>();
    private List<ShrinePlan.Placement> plan;
    private int planIndex;
    private int phase = CAST;
    private int tick;
    private Iterator<Map.Entry<BlockPos, BlockState>> restoreIt;
    private boolean weatherSet;
    private boolean wasClear;

    private DomainTask(ServerPlayerEntity owner, SukunaState s, boolean open) {
        this.w = Fx.world(owner);
        this.owner = owner;
        this.s = s;
        this.origin = new BlockPos(MathHelper.floor(owner.getX()), MathHelper.floor(owner.getY()) - 1, MathHelper.floor(owner.getZ()));
        this.center = new Vec3d(owner.getX(), owner.getY(), owner.getZ());
        this.open = open;
        this.radius = open ? Math.max(64, SukunaMod.config.domainRadius) : SukunaMod.config.domainClosedRadius;
        this.shellCenter = new Vec3d(owner.getX(), owner.getY() + 5.0, owner.getZ());
        this.duration = SukunaMod.config.domainDurationTicks;
        this.power = s.power();
        Direction facing = owner.getHorizontalFacing();
        this.rot = switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    public static boolean hasActive(ServerPlayerEntity p) {
        for (DomainTask d : ALL) {
            if (d.owner.getUuid().equals(p.getUuid())) {
                return true;
            }
        }
        return false;
    }

    public static boolean cast(ServerPlayerEntity p, SukunaState s, boolean open) {
        for (DomainTask d : ALL) {
            if (d.owner == p) {
                return false;
            }
        }
        if (!p.isOnGround()) {
            p.sendMessage(Text.translatable("sukuna.msg.need_ground"), true);
            return false;
        }
        DomainTask t = new DomainTask(p, s, open);
        ALL.add(t);
        DomainClash.register(t);
        Tasks.add(t);
        s.busyUntil = Fx.world(p).getTime() + 70;
        return true;
    }

    public static boolean contains(ServerWorld world, double x, double y, double z) {
        for (DomainTask d : ALL) {
            if (d.w != world || d.phase != ACTIVE) {
                continue;
            }
            if (!d.open) {
                if (d.shellCenter.squaredDistanceTo(x, y, z) <= (double) d.radius * d.radius) {
                    return true;
                }
                continue;
            }
            double dx = x - d.center.x;
            double dz = z - d.center.z;
            if (dx * dx + dz * dz <= (double) d.radius * d.radius && Math.abs(y - d.center.y) <= 24.0) {
                return true;
            }
        }
        return false;
    }

    public static void restoreAll() {
        for (DomainTask d : new ArrayList<>(ALL)) {
            d.restoreNow();
        }
        ALL.clear();
        DomainClash.clear();
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
        return open ? center : shellCenter;
    }

    @Override
    public double clashRadius() {
        return radius;
    }

    @Override
    public boolean clashActive() {
        return phase == ACTIVE;
    }

    @Override
    public void collapse() {
        beginRestore();
    }

    private void restoreNow() {
        s.domainLeft = 0;
        for (Map.Entry<BlockPos, BlockState> e : originals.entrySet()) {
            if (SukunaMod.config.domainRestoresTerrain) {
                w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
            }
            DomainShield.remove(e.getKey());
        }
        if (weatherSet && wasClear) {
            w.setWeather(6000, 0, false, false);
        }
        originals.clear();
    }

    @Override
    public boolean tick() {
        if (phase < RESTORE && (owner.isRemoved() || !owner.isAlive() || owner.isDisconnected())) {
            beginRestore();
        }
        switch (phase) {
            case CAST -> castPhase();
            case BUILD -> buildPhase();
            case ACTIVE -> activePhase();
            default -> {
                return restorePhase();
            }
        }
        return false;
    }

    private void titles() {
        Text title = Text.literal("領域展開").formatted(Formatting.DARK_RED, Formatting.BOLD);
        Text sub = Text.translatable(open ? "sukuna.domain.subtitle_open" : "sukuna.domain.subtitle_closed").formatted(Formatting.GRAY);
        double reach = radius + 40.0;
        for (ServerPlayerEntity pl : w.getPlayers(x -> true)) {
            if (pl.squaredDistanceTo(center.x, center.y, center.z) > reach * reach) {
                continue;
            }
            pl.networkHandler.sendPacket(new TitleFadeS2CPacket(5, 50, 20));
            pl.networkHandler.sendPacket(new SubtitleS2CPacket(sub));
            pl.networkHandler.sendPacket(new TitleS2CPacket(title));
        }
    }

    private void castPhase() {
        if (tick == 0) {
            titles();
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.MASTER, 2.0F, 0.6F);
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.MASTER, 3.0F, 0.5F);
        }
        if (tick % 3 == 0) {
            owner.swingHand(tick % 6 == 0 ? Hand.MAIN_HAND : Hand.OFF_HAND, true);
        }
        if (tick % 4 == 0) {
            owner.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 8, 6, false, false));
        }
        double r = 1.5 + tick * (open ? 0.3 : 0.2);
        if (tick % 3 == 0) {
            Ink.ring(w, new Vec3d(center.x, center.y + 0.15, center.z), r, Ink.BLOOD, 0.8F);
        }
        Fx.far(w, ParticleTypes.ASH, center.x, center.y + 1.0, center.z, 10, r * 0.4, 1.0, r * 0.4, 0.02);
        if (tick == 20) {
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.MASTER, 3.0F, 0.5F);
        }
        tick++;
        if (tick >= 30) {
            phase = BUILD;
            tick = 0;
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_WITHER_SPAWN, SoundCategory.MASTER, 2.5F, 0.55F);
        }
    }

    private void buildPhase() {
        if (plan == null) {
            plan = new ArrayList<>();
            boolean blocks = SukunaMod.config.blockDamage;
            if (!open && blocks) {
                plan.addAll(shell());
                shellCount = plan.size();
            }
            if (open && blocks && SukunaMod.config.domainBuildsShrine) {
                plan.addAll(DomainShell.openArena(origin, Math.min(radius, 22), 12));
            }
            if (SukunaMod.config.domainBuildsShrine && blocks) {
                plan.addAll(new ShrinePlan(origin, rot).placements());
            }
            if (plan.isEmpty()) {
                enterActive();
                return;
            }
        }
        int changes = 0;
        while (planIndex < plan.size() && changes < 450) {
            ShrinePlan.Placement next = plan.get(planIndex++);
            if (place(next, next.state().isOf(Blocks.LIGHT))) {
                changes++;
            }
        }
        if (tick % 2 == 0) {
            Fx.far(w, ParticleTypes.ASH, center.x, center.y + 1.0, center.z, 20, radius * 0.4, 1.5, radius * 0.4, 0.2);
        }
        tick++;
        if (planIndex >= plan.size()) {
            enterActive();
        }
    }

    private List<ShrinePlan.Placement> shell() {
        return DomainShell.plan(shellCenter, radius, DomainShell.Style.SUKUNA, origin.getY());
    }

    private void darken() {
        for (ServerPlayerEntity pl : w.getPlayers(x -> x != owner)) {
            if (shellCenter.squaredDistanceTo(pl.getX(), pl.getY(), pl.getZ()) <= (double) radius * radius) {
                pl.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false));
            }
        }
    }

    private void ambient(ThreadLocalRandom rnd) {
        if (tick % 2 == 0) {
            for (int i = 0; i < 6; i++) {
                Vec3d d = new Vec3d(rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5, rnd.nextDouble() - 0.5).normalize().multiply(rnd.nextDouble() * (radius - 2));
                Vec3d p = shellCenter.add(d);
                Ink.dot(w, p, i % 2 == 0 ? Ink.RED : Ink.GOLD, 0.5F + rnd.nextFloat() * 0.5F);
            }
        }
        if (tick % 5 == 0) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double rr = rnd.nextDouble() * (radius - 3);
            Fx.far(w, ParticleTypes.CHERRY_LEAVES, shellCenter.x + Math.cos(a) * rr, shellCenter.y + radius * 0.5, shellCenter.z + Math.sin(a) * rr, 6, 1.5, 1.0, 1.5, 0.02);
        }
        if (tick % 40 == 0) {
            Ink.ring(w, new Vec3d(center.x, center.y + 0.15, center.z), 3.0 + (tick % 80) * 0.15, Ink.RED, 0.5F);
        }
    }

    private void beginRestore() {
        if (phase >= RESTORE) {
            return;
        }
        phase = RESTORE;
        DomainMusic.stop(w, open ? DomainMusic.HEIAN : DomainMusic.SUKUNA);
        s.domainLeft = 0;
        if (weatherSet && wasClear) {
            w.setWeather(6000, 0, false, false);
        }
        restoreIt = SukunaMod.config.domainRestoresTerrain ? new ArrayList<>(originals.entrySet()).iterator() : null;
        Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 3.0F, 0.6F);
    }

    private void enterActive() {
        phase = ACTIVE;
        DomainMusic.start(w, center, open ? DomainMusic.HEIAN : DomainMusic.SUKUNA);
        tick = 0;
        s.domainLeft = duration;

        if (SukunaMod.config.domainThunder && !w.isRaining() && !w.isThundering()) {
            wasClear = true;
            weatherSet = true;
            w.setWeather(0, duration, true, true);
        }

        Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_BELL_RESONATE,
            SoundCategory.MASTER, 2.0F, 0.55F);
        Ink.ring(w, new Vec3d(center.x, center.y + 0.2, center.z), radius * 0.65, Ink.RED, 0.8F);
    }

    private void activePhase() {
        if (tick == 0) {
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE,
                SoundCategory.MASTER, 1.8F, 0.65F);
        }

        s.domainLeft = Math.max(0, duration - tick);

        if (tick % 70 == 0) {
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.ENTITY_WARDEN_HEARTBEAT, SoundCategory.MASTER, 2.5F, 0.6F);
            Fx.sound(w, center.x, center.y, center.z, SoundEvents.BLOCK_FIRE_AMBIENT, SoundCategory.MASTER, 2.0F, 0.6F);
        }
        if (tick % 10 == 0) {
            damageTargets();
            if (!open) {
                darken();
            }
        }

        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        if (!open) {
            ambient(rnd);
        } else {
            if (tick % 4 == 0) {
                double a = rnd.nextDouble() * Math.PI * 2.0;
                double r = rnd.nextDouble() * radius;
                Vec3d p = new Vec3d(center.x + Math.cos(a) * r, center.y + 0.2,
                    center.z + Math.sin(a) * r);
                Ink.slashLite(w, p, new Vec3d(rnd.nextDouble() - 0.5, 0.1,
                    rnd.nextDouble() - 0.5), rnd.nextDouble() * Math.PI, 1.3 + rnd.nextDouble(), Ink.BLOOD);
            }
            if (tick % 20 == 0) {
                Ink.ring(w, new Vec3d(center.x, center.y + 0.15, center.z),
                    2.0 + (tick % 100) * 0.12, Ink.RED, 0.55F);
                cutTerrain(rnd);
            }
        }

        tick++;
        if (tick >= duration) {
            beginRestore();
        }
    }

    private void damageTargets() {
        Box area = open
            ? new Box(center.x - radius, center.y - 24.0, center.z - radius,
                     center.x + radius, center.y + 24.0, center.z + radius)
            : new Box(shellCenter, shellCenter).expand(radius);

        for (LivingEntity entity : w.getEntitiesByClass(LivingEntity.class, area,
                target -> Aim.valid(owner, target) && !Counters.nullifies(target))) {
            double dx = entity.getX() - (open ? center.x : shellCenter.x);
            double dy = entity.getY() + entity.getHeight() * 0.5 - (open ? center.y : shellCenter.y);
            double dz = entity.getZ() - (open ? center.z : shellCenter.z);
            boolean inside = open
                ? dx * dx + dz * dz <= (double) radius * radius && Math.abs(dy) <= 24.0
                : dx * dx + dy * dy + dz * dz <= (double) radius * radius;
            if (!inside) {
                continue;
            }

            Dmg.hit(owner, entity, 6.0 * power);
            Vec3d hit = new Vec3d(entity.getX(), entity.getY() + entity.getHeight() * 0.55, entity.getZ());
            Vec3d direction = new Vec3d(dx, dy, dz).multiply(-1.0).normalize();
            if (direction.lengthSquared() < 1.0E-6) {
                direction = new Vec3d(0.0, 1.0, 0.0);
            }
            Ink.slashLite(w, hit, direction, ThreadLocalRandom.current().nextDouble() * Math.PI,
                1.4 + ThreadLocalRandom.current().nextDouble() * 1.2, Ink.BLOOD);
        }
    }

    private void cutTerrain(ThreadLocalRandom rnd) {
        if (!SukunaMod.config.blockDamage || !open) {
            return;
        }

        for (int line = 0; line < 3; line++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double startRadius = rnd.nextDouble() * radius * 0.55;
            double startX = center.x + Math.cos(angle) * startRadius;
            double startZ = center.z + Math.sin(angle) * startRadius;
            double length = radius * (0.8 + rnd.nextDouble() * 0.8);
            double dx = Math.cos(angle + Math.PI / 2.0);
            double dz = Math.sin(angle + Math.PI / 2.0);

            for (double offset = -length * 0.5; offset <= length * 0.5; offset += 0.75) {
                int x = MathHelper.floor(startX + dx * offset);
                int z = MathHelper.floor(startZ + dz * offset);
                double fromOwnerX = x + 0.5 - owner.getX();
                double fromOwnerZ = z + 0.5 - owner.getZ();
                if (fromOwnerX * fromOwnerX + fromOwnerZ * fromOwnerZ <= 9.0) {
                    continue;
                }

                int y = Cut.surfaceY(w, x, z, MathHelper.floor(center.y));
                if (y == Integer.MIN_VALUE) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = w.getBlockState(pos);
                if (DomainShield.isProtected(pos) || state.hasBlockEntity() || !Cut.allowed(w, pos, state)) {
                    continue;
                }
                originals.putIfAbsent(pos.toImmutable(), state);
                w.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
            }
        }
    }

    private boolean place(ShrinePlan.Placement placement, boolean light) {
        BlockPos pos = placement.pos();
        if (!w.isPosLoaded(pos) || !w.isInBuildLimit(pos)) {
            return false;
        }

        BlockState next = placement.state();
        BlockState current = w.getBlockState(pos);

        if (light && !(current.isAir() || current.isReplaceable())) {
            return false;
        }
        if (current.hasBlockEntity()) {
            return false;
        }
        if (!current.isAir() && !current.isReplaceable() && !current.isLiquid()) {
            float hardness = current.getHardness(w, pos);
            if (hardness < 0.0F || (!SukunaMod.config.breakHardBlocks
                    && hardness > SukunaMod.config.maxHardness)) {
                return false;
            }
        }

        if (current.equals(next)) {
            if (next.isAir()) {
                return false;
            }
            if (!next.isAir()) {
                originals.putIfAbsent(pos.toImmutable(), current);
                DomainShield.add(pos);
            }
            return true;
        }

        originals.putIfAbsent(pos.toImmutable(), current);
        if (!w.setBlockState(pos, next, Block.NOTIFY_LISTENERS)) {
            return false;
        }
        if (next.isAir()) {
            DomainShield.remove(pos);
        } else {
            DomainShield.add(pos);
        }
        return true;
    }

    private boolean restorePhase() {
        if (restoreIt != null) {
            int n = 0;
            while (restoreIt.hasNext() && n < 700) {
                Map.Entry<BlockPos, BlockState> e = restoreIt.next();
                w.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS);
                DomainShield.remove(e.getKey());
                n++;
            }
            if (restoreIt.hasNext()) {
                return false;
            }
        } else {
            for (BlockPos pos : originals.keySet()) {
                DomainShield.remove(pos);
            }
        }
        originals.clear();
        ALL.remove(this);
        DomainClash.unregister(this);
        return true;
    }
}
