package com.dowerp.sukuna;

import com.dowerp.sukuna.net.AnimPayload;
import com.dowerp.sukuna.net.DomainMeterPayload;
import com.dowerp.sukuna.net.RemoteStatePayload;
import com.dowerp.sukuna.net.StatePayload;
import com.dowerp.sukuna.tasks.ChosoTasks;
import com.dowerp.sukuna.tasks.GojoTasks;
import com.dowerp.sukuna.tasks.TodoTasks;
import net.minecraft.entity.projectile.ProjectileEntity;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class Players {
    public static AttachmentType<Integer> FINGERS;
    public static AttachmentType<String> CHARACTER;
    private static final Map<UUID, SukunaState> STATES = new HashMap<>();

    private static final Identifier MOD_HP = SukunaMod.id("form_health");
    private static final Identifier MOD_ARMOR = SukunaMod.id("heian_armor");
    private static final Identifier MOD_YUJIMELEE = SukunaMod.id("yuji_melee");
    private static final Identifier MOD_SCALE = SukunaMod.id("heian_scale");
    private static final Identifier MOD_WING_SPEED = SukunaMod.id("choso_wing_speed");
    public static final Identifier MOD_TODO_SPEED = SukunaMod.id("todo_speed");
    public static final Identifier MOD_TODO_JUMP = SukunaMod.id("todo_jump");
    public static final double HEIAN_HEIGHT = 2.5;

    private static boolean reentry;

    private Players() {}

    public static SukunaState get(ServerPlayerEntity p) {
        return STATES.computeIfAbsent(p.getUuid(), id -> new SukunaState());
    }

    public static SukunaCharacter character(ServerPlayerEntity p) {
        SukunaCharacter c = SukunaCharacter.byId(p.getAttachedOrElse(CHARACTER, SukunaCharacter.ITADORI.id));
        return c.playable ? c : SukunaCharacter.ITADORI;
    }

    public static final int COMBAT_TICKS = 300;

    public static void markCombat(ServerPlayerEntity p) {
        SukunaState st = STATES.get(p.getUuid());
        if (st != null) {
            st.combatUntil = Fx.world(p).getTime() + COMBAT_TICKS;
        }
    }

    public static void select(ServerPlayerEntity p, int index) {
        SukunaCharacter[] all = SukunaCharacter.values();
        if (index < 0 || index >= all.length) return;
        SukunaCharacter c = all[index];
        if (!c.playable) {
            p.sendMessage(Text.translatable("sukuna.msg.char_locked", c.displayName()), true);
            return;
        }
        SukunaState s = get(p);
        if (s.combatUntil > Fx.world(p).getTime() && s.character != c) {
            p.sendMessage(Text.translatable("sukuna.msg.in_battle_locked"), true);
            return;
        }
        java.util.Arrays.fill(s.cooldowns, 0L);
        s.busyUntil = 0L;
        p.setAttached(CHARACTER, c.id);
        s.character = c;
        s.form = c.defaultForm();
        s.wheel = false;
        s.formUntil = 0L;
        s.hostileKills = 0;
        s.awakenedViaMeter = false;
        s.todoBuffUntil = 0L;
        ChosoTasks.stopAll(s);
        applyFormAttributes(p, s.form);
        s.ce = s.maxCe();
        p.sendMessage(Text.translatable("sukuna.msg.char_selected", c.displayName()), true);
        sync(p, s);
    }

    public static void init() {
        FINGERS = AttachmentRegistry.<Integer>create(SukunaMod.id("fingers"), builder -> builder.persistent(Codec.INT).copyOnDeath().initializer(() -> 1));
        CHARACTER = AttachmentRegistry.<String>create(SukunaMod.id("character"), builder -> builder.persistent(Codec.STRING).copyOnDeath().initializer(() -> SukunaCharacter.ITADORI.id));
        PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> !DomainShield.isProtected(pos));
        ServerTickEvents.END_SERVER_TICK.register(Players::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> STATES.remove(handler.player.getUuid()));
        ServerLivingEntityEvents.ALLOW_DAMAGE.register(Players::allowDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken, blocked) -> onDamaged(entity, source, damageTaken));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            SukunaState st = STATES.get(newPlayer.getUuid());
            if (st == null) return;
            applyFormAttributes(newPlayer, st.form);
            st.ce = st.maxCe();
            newPlayer.setHealth(newPlayer.getMaxHealth());
        });
        
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity p) {
                SukunaState s = STATES.get(p.getUuid());
                if (s != null) {
                    s.wheel = false;
                    s.adaptation.reset();
                    s.ce = 0.0;
                    s.charging = false;
                    s.domainLeft = 0;
                    s.busyUntil = 0L;
                    s.lowHpAwakenUsed = false;
                    s.domainMeter = 0.0;
                    s.awakenedViaMeter = false;
                    s.todoBuffUntil = 0L;
                    s.mahoragaSeen = false;
                    if (s.form == SukunaForm.HEIAN) {
                        s.form = SukunaForm.SUKUNA;
                        s.hostileKills = 0;
                        s.heianUnlocked = false;
                        s.formUntil = 0L;
                    } else if (s.form == SukunaForm.GOJO_AWAKENED) {
                        s.form = SukunaForm.GOJO;
                        s.formUntil = 0L;
                        s.formCooldownUntil = Fx.world(p).getTime() + SukunaState.GOJO_AWAKEN_COOLDOWN;
                    } else if (s.form == SukunaForm.CHOSO_WING) {
                        s.form = SukunaForm.CHOSO;
                        s.formUntil = 0L;
                        s.formCooldownUntil = Fx.world(p).getTime() + SukunaState.GOJO_AWAKEN_COOLDOWN;
                    } else if (s.form == SukunaForm.TODO_AWAKENED) {
                        s.form = SukunaForm.TODO;
                        s.formUntil = 0L;
                        s.formCooldownUntil = Fx.world(p).getTime() + SukunaState.GOJO_AWAKEN_COOLDOWN;
                    }
                    ChosoTasks.stopAll(s);
                }
            }

            if (source.getAttacker() instanceof ServerPlayerEntity mk && mk != entity) {
                if (entity instanceof ServerPlayerEntity) {
                    get(mk).addMeter(10.0);
                } else if (entity instanceof HostileEntity) {
                    get(mk).addMeter(2.0);
                }
            }

            int gain = 0;
            if (entity instanceof HostileEntity) {
                gain = 1;
            } else if (entity instanceof ServerPlayerEntity victim && source.getAttacker() instanceof ServerPlayerEntity k && k != victim) {
                gain = SukunaState.PLAYER_KILL_VALUE;
            }
            if (gain > 0 && source.getAttacker() instanceof ServerPlayerEntity killer) {
                SukunaState s = get(killer);
                if (s.form == SukunaForm.SUKUNA) {
                    int before = s.hostileKills;
                    s.hostileKills += gain;
                    if (before < SukunaState.OPEN_DOMAIN_KILLS && s.hostileKills >= SukunaState.OPEN_DOMAIN_KILLS && !(s.fingers >= SukunaState.OPEN_DOMAIN_FINGERS)) {
                        killer.sendMessage(Text.translatable("sukuna.msg.open_ready"), false);
                    }
                    if (before < SukunaState.HEIAN_KILLS && s.hostileKills >= SukunaState.HEIAN_KILLS) {
                        killer.sendMessage(Text.translatable("sukuna.msg.heian_ready"), false);
                        AnimeVfx.playHeianReady(Fx.world(killer), killer);
                    }
                }
            }
        });
    }

    private static void onDamaged(LivingEntity entity, DamageSource source, float taken) {
        if (taken <= 0.0F) return;
        if (entity instanceof ServerPlayerEntity victim) {
            SukunaState vs = STATES.get(victim.getUuid());
            if (vs != null) {
                vs.lastDamagedAt = Fx.world(victim).getTime();
                vs.addMeter(0.10 * taken);
            }
        }
        if (source.getAttacker() instanceof ServerPlayerEntity att && att != entity) {
            SukunaState as = STATES.get(att.getUuid());
            if (as != null) {
                as.addMeter(0.15 * taken);
            }
        }
    }

    private static void lowHpAwaken(ServerPlayerEntity p, SukunaState s, ServerWorld w) {
        if (!SukunaMod.config.lowHpAwaken || !s.meterOn() || s.lowHpAwakenUsed || !s.form.awakenable()) return;
        if (s.domainMeter < 50.0 || p.getHealth() >= p.getMaxHealth() * 0.4F) return;
        s.lowHpAwakenUsed = true;
        s.domainMeter = Math.max(0.0, s.domainMeter - 50.0);
        s.awakenedViaMeter = false;
        int ticks = SukunaMod.config.awakenDurationTicks;
        switch (s.form) {
            case GOJO -> GojoTasks.awaken(p, s, w, ticks);
            case CHOSO -> ChosoTasks.awaken(p, s, w, ticks);
            case TODO -> TodoTasks.awaken(p, s, w, ticks);
            default -> {
            }
        }
        p.sendMessage(Text.translatable("sukuna.msg.low_hp_awaken"), false);
    }

    public static void endHeian(ServerPlayerEntity p, SukunaState s, boolean manual) {
        s.form = SukunaForm.SUKUNA;
        s.hostileKills = 0;
        s.heianUnlocked = false;
        s.formUntil = 0L;
        s.ce = Math.min(s.ce, s.maxCe());
        applyFormAttributes(p, s.form);
        p.sendMessage(Text.translatable(manual ? "sukuna.msg.heian_left" : "sukuna.msg.heian_ended"), false);
    }

    public static void endAwakening(ServerPlayerEntity p, SukunaState s, long now) {
        boolean choso = s.form == SukunaForm.CHOSO_WING;
        boolean todo = s.form == SukunaForm.TODO_AWAKENED;
        s.form = choso ? SukunaForm.CHOSO : todo ? SukunaForm.TODO : SukunaForm.GOJO;
        s.formUntil = 0L;
        s.formCooldownUntil = s.awakenedViaMeter ? 0L : now + SukunaState.GOJO_AWAKEN_COOLDOWN;
        s.awakenedViaMeter = false;
        s.ce = Math.min(s.ce, s.maxCe() * 0.25);
        if (choso) {
            ChosoTasks.stopAll(s);
        }
        applyFormAttributes(p, s.form);
        p.sendMessage(Text.translatable(choso ? "sukuna.msg.choso_ended" : todo ? "sukuna.msg.todo_ended" : "sukuna.msg.gojo_ended"), false);
    }

    public static void applyFormAttributes(ServerPlayerEntity p, SukunaForm form) {
        EntityAttributeInstance maxHpAttr = p.getAttributeInstance(EntityAttributes.MAX_HEALTH);
        if (maxHpAttr != null) {
            maxHpAttr.removeModifier(MOD_HP);
            double bonusHp = form.targetHp - 20.0;
            maxHpAttr.addPersistentModifier(new EntityAttributeModifier(MOD_HP, bonusHp, EntityAttributeModifier.Operation.ADD_VALUE));
            if (p.getHealth() > p.getMaxHealth()) {
                p.setHealth(p.getMaxHealth());
            }
        }

        EntityAttributeInstance armorAttr = p.getAttributeInstance(EntityAttributes.ARMOR);
        if (armorAttr != null) {
            armorAttr.removeModifier(MOD_ARMOR);
            if (form == SukunaForm.HEIAN) {
                armorAttr.addPersistentModifier(new EntityAttributeModifier(MOD_ARMOR, 6.0, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        EntityAttributeInstance scaleAttr = p.getAttributeInstance(EntityAttributes.SCALE);
        if (scaleAttr != null) {
            scaleAttr.removeModifier(MOD_SCALE);
            if (form == SukunaForm.HEIAN) {
                scaleAttr.addPersistentModifier(new EntityAttributeModifier(MOD_SCALE, HEIAN_HEIGHT / 1.8 - 1.0, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }

        EntityAttributeInstance speedAttr = p.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.removeModifier(MOD_WING_SPEED);
            if (form == SukunaForm.CHOSO_WING) {
                speedAttr.addPersistentModifier(new EntityAttributeModifier(MOD_WING_SPEED, 0.45, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }

        EntityAttributeInstance todoSpeed = p.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
        if (todoSpeed != null) {
            todoSpeed.removeModifier(MOD_TODO_SPEED);
        }
        EntityAttributeInstance todoJump = p.getAttributeInstance(EntityAttributes.JUMP_STRENGTH);
        if (todoJump != null) {
            todoJump.removeModifier(MOD_TODO_JUMP);
        }

        EntityAttributeInstance attackAttr = p.getAttributeInstance(EntityAttributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(MOD_YUJIMELEE);
            if (form == SukunaForm.YUJI && !(p.getMainHandStack().getItem() instanceof RangedWeaponItem)) {
                attackAttr.addPersistentModifier(new EntityAttributeModifier(MOD_YUJIMELEE, 6.0, EntityAttributeModifier.Operation.ADD_VALUE));
            }
        }
    }

    private static void tick(MinecraftServer server) {
        Ink.resetBudget();
        Tasks.tick();
        DomainClash.tick();
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            SukunaState s = get(p);
            s.fingers = p.getAttachedOrElse(FINGERS, 1);
            s.character = character(p);
            ServerWorld w = Fx.world(p);

            if (!s.initialized) {
                s.form = s.character.defaultForm();
                applyFormAttributes(p, s.form);
                s.ce = s.maxCe();
                s.initialized = true;
            }

            s.inDomain = DomainTask.contains(w, p.getX(), p.getY(), p.getZ());

            if (p.isAlive()) {
                p.getHungerManager().setFoodLevel(20);
                p.getHungerManager().setSaturationLevel(0.0F);
            }

            long nowTick = w.getTime();
            if (p.isAlive() && s.formUntil > 0L && nowTick >= s.formUntil) {
                if (s.form == SukunaForm.HEIAN) {
                    endHeian(p, s, false);
                } else if (s.form.awakened()) {
                    endAwakening(p, s, nowTick);
                }
            }
            if (p.isAlive() && s.form.isChoso()) {
                ChosoTasks.tick(p, s, w);
            }
            if (p.isAlive() && s.form.isGojo()) {
                GojoTasks.infinityTick(p, s, w);
            }
            if (p.isAlive() && s.form.isTodo()) {
                TodoTasks.tick(p, s, w);
            }
            if (p.isAlive()) {
                if (s.meterOn()) {
                    if (s.combatUntil <= nowTick && s.domainMeter > 0.0) {
                        s.domainMeter = Math.max(0.0, s.domainMeter - 0.05);
                    }
                    lowHpAwaken(p, s, w);
                }
                if (p.age % 10 == 0 && s.mahoraga != null) {
                    Entity mh = w.getEntity(s.mahoraga);
                    if (mh != null && mh.isAlive()) {
                        s.mahoragaSeen = true;
                    } else if (s.mahoragaSeen) {
                        s.mahoragaSeen = false;
                        s.mahoraga = null;
                        s.adaptation.reset();
                        s.wheelAngle = 0.0F;
                        s.wheelTarget = 0.0F;
                    }
                }
            }

            if (p.isAlive()) {
                if (s.form == SukunaForm.YUJI && p.age % 60 == 0 && p.getHealth() < p.getMaxHealth()) {
                    p.heal(1.5F);
                    Fx.puff(w, ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY() + 1.0, p.getZ(), 3, 0.3, 0.4, 0.3, 0.0);
                }

                if (s.adaptation.total() > 0) {
                    List<String> broken = s.adaptation.expire(nowTick);
                    if (!broken.isEmpty()) {
                        s.wheelTarget = s.adaptation.total() * (360.0F / SukunaMod.config.adaptationSteps);
                        s.wheelAngle = Math.min(s.wheelAngle, s.wheelTarget);
                        Fx.far(w, ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 2.4, p.getZ(), 30, 0.5, 0.3, 0.5, 0.25);
                        Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.5F, 0.6F);
                        p.sendMessage(Text.translatable("sukuna.msg.adapt_broken"), true);
                    }
                }

                if ((s.form == SukunaForm.SUKUNA || s.form == SukunaForm.HEIAN) && p.age % 4 == 0) {
                    fugaFlame(p, w, s.form == SukunaForm.HEIAN);
                }

                double regen = (0.2 + 0.015 * s.statFingers()) * SukunaMod.config.ceRegenMultiplier;
                double regenBoost = 1.8;
                if (s.form == SukunaForm.HEIAN) regen *= 1.4;
                if (s.form == SukunaForm.GOJO) regen = 0.35 * SukunaMod.config.ceRegenMultiplier;
                if (s.form == SukunaForm.GOJO_AWAKENED) regen = 0.6 * SukunaMod.config.ceRegenMultiplier;
                if (s.form == SukunaForm.CHOSO) regen = 0.45 * SukunaMod.config.ceRegenMultiplier;
                if (s.form == SukunaForm.CHOSO_WING) regen = 1.1 * SukunaMod.config.ceRegenMultiplier;
                if (s.form == SukunaForm.TODO) regen = 0.4 * SukunaMod.config.ceRegenMultiplier;
                if (s.form == SukunaForm.TODO_AWAKENED) regen = 0.7 * SukunaMod.config.ceRegenMultiplier;
                s.ce = Math.min(s.maxCe(), s.ce + regen * regenBoost);
                if (SukunaMod.config.freeCursedEnergy) s.ce = s.maxCe();

                if (s.wheel) {
                    s.ce -= 0.12;
                    if (s.ce <= 0.0) {
                        s.ce = 0.0;
                        s.wheel = false;
                        p.sendMessage(Text.translatable("sukuna.msg.wheel_off"), true);
                    } else if (s.wheelAngle < s.wheelTarget) {
                        s.wheelAngle = Math.min(s.wheelTarget, s.wheelAngle + 9.0F);
                    }
                }
            }

            if (p.age % 5 == 0) sync(p, s);
        }
    }

    private static void sync(ServerPlayerEntity p, SukunaState s) {
        ServerWorld w = Fx.world(p);
        long now = w.getTime();

        int flags = 0;
        if (s.vessel()) flags |= StatePayload.VESSEL;
        if (s.wheel) flags |= StatePayload.WHEEL;
        if (s.domainLeft > 0) flags |= StatePayload.DOMAIN_CASTER;
        if (s.inDomain) flags |= StatePayload.IN_DOMAIN;
        if (s.charging) flags |= StatePayload.CHARGING;
        if (s.redScaleUntil > now) flags |= StatePayload.RED_SCALE;
        if (s.redScaleUntil > now && s.redScaleStack) flags |= StatePayload.SCALE_STACK;
        if (s.hardenUntil > now) flags |= StatePayload.HARDEN;
        if (s.openDomainReady()) flags |= StatePayload.OPEN_READY;
        if (s.kamutokeUntil > now) flags |= StatePayload.KAMUTOKE;
        if (s.piercingCharging) flags |= StatePayload.PIERCE_CHARGE;
        if (s.runUntil > now) flags |= StatePayload.RUNNING;
        if (s.combatUntil > now) flags |= StatePayload.IN_BATTLE;
        if (s.mahoraga != null) {
            Entity m = w.getEntity(s.mahoraga);
            if (m != null && m.isAlive()) flags |= StatePayload.MAHORAGA;
        }

        List<Integer> cooldowns = new ArrayList<>(s.cooldowns.length);
        for (long end : s.cooldowns) {
            cooldowns.add((int) Math.max(0L, end - now));
        }

        if (s.initialized) {
            RemoteStatePayload remote = new RemoteStatePayload(p.getId(), s.character.ordinal(), s.form.ordinal(), flags, s.headUntil > now ? s.headVictim : "");
            for (ServerPlayerEntity o : PlayerLookup.tracking(p)) {
                if (o != p && ServerPlayNetworking.canSend(o, RemoteStatePayload.ID)) {
                    ServerPlayNetworking.send(o, remote);
                }
            }
            if (ServerPlayNetworking.canSend(p, RemoteStatePayload.ID)) {
                ServerPlayNetworking.send(p, remote);
            }
        }

        int meterTenths = s.meterOn() ? (int) Math.round(Math.min(SukunaState.METER_MAX, s.domainMeter) * 10.0) : DomainMeterPayload.DISABLED;
        if (meterTenths != s.lastMeterSent && ServerPlayNetworking.canSend(p, DomainMeterPayload.ID)) {
            s.lastMeterSent = meterTenths;
            ServerPlayNetworking.send(p, new DomainMeterPayload(meterTenths));
        }

        if (ServerPlayNetworking.canSend(p, StatePayload.ID)) {
            ServerPlayNetworking.send(p, new StatePayload((int) s.ce, (int) s.maxCe(), flags, s.domainLeft, s.adaptation.total(), s.form.ordinal(), s.hostileKills, cooldowns, s.character.ordinal(), s.formUntil > now ? (int) (s.formUntil - now) : 0));
        }
    }

    private static void fugaFlame(ServerPlayerEntity p, ServerWorld w, boolean heian) {
        double yaw = Math.toRadians(p.getYaw());
        double k = heian ? HEIAN_HEIGHT / 1.8 : 1.0;
        Vec3d fwd = new Vec3d(-Math.sin(yaw), 0.0, Math.cos(yaw));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0.0, -Math.sin(yaw));
        Vec3d hand = new Vec3d(p.getX(), p.getY() + 1.05 * k, p.getZ()).add(right.multiply(0.45 * k)).add(fwd.multiply(0.4 * k));
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Fx.puff(w, ParticleTypes.FLAME, hand.x, hand.y, hand.z, 1, 0.07 * k, 0.1 * k, 0.07 * k, 0.012);
        Ink.dot(w, hand.add(0.0, 0.05, 0.0), Ink.FIRE, 0.9F);
        Ink.dot(w, hand.add((r.nextDouble() - 0.5) * 0.15, 0.12 + r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.15), Ink.FIRE_CORE, 0.6F);
        if (p.age % 16 == 0) {
            Fx.puff(w, ParticleTypes.SMALL_FLAME, hand.x, hand.y + 0.2, hand.z, 3, 0.12, 0.16, 0.12, 0.02);
        }
    }

    public static void broadcastNamedAnim(ServerPlayerEntity p, int code) {
        AnimPayload payload = new AnimPayload(p.getId(), Ability.values().length + code, p.getId());
        for (ServerPlayerEntity o : PlayerLookup.tracking(p)) {
            if (o != p && ServerPlayNetworking.canSend(o, AnimPayload.ID)) {
                ServerPlayNetworking.send(o, payload);
            }
        }
        if (ServerPlayNetworking.canSend(p, AnimPayload.ID)) {
            ServerPlayNetworking.send(p, payload);
        }
    }

    public static void broadcastAnim(ServerPlayerEntity p, Ability a, SukunaForm form) {
        AnimPayload payload = new AnimPayload(p.getId(), a.ordinal(), form.ordinal());
        for (ServerPlayerEntity o : PlayerLookup.tracking(p)) {
            if (o != p && ServerPlayNetworking.canSend(o, AnimPayload.ID)) {
                ServerPlayNetworking.send(o, payload);
            }
        }
    }

    private static boolean allowDamage(LivingEntity entity, DamageSource source, float amount) {
        Entity damager = source.getAttacker();
        if (damager instanceof ServerPlayerEntity dp && damager != entity) {
            markCombat(dp);
        }
        if (reentry || !(entity instanceof ServerPlayerEntity p)) return true;
        markCombat(p);
        SukunaState s = STATES.get(p.getUuid());
        if (s == null || !p.isAlive()) return true;
        if (source.isOf(DamageTypes.OUT_OF_WORLD) || source.isOf(DamageTypes.GENERIC_KILL)) return true;

        if (s.form.isGojo()) {
            ServerWorld gw = Fx.world(p);
            if (s.ce < 10.0 || gw.getTime() < s.infinityOffUntil) return true;
            Entity direct = source.getSource();
            Entity attacker = source.getAttacker();
            if (direct instanceof ProjectileEntity) {
                s.ce = Math.max(0.0, s.ce - 2.0);
                GojoTasks.infinityFlash(gw, p);
                return false;
            }
            boolean melee = source.isOf(DamageTypes.PLAYER_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK) || source.isOf(DamageTypes.MOB_ATTACK_NO_AGGRO);
            if (melee) {
                s.ce = Math.max(0.0, s.ce - Math.min(amount, 20.0F) * 0.3);
                GojoTasks.infinityFlash(gw, p);
                if (attacker instanceof LivingEntity le && le.squaredDistanceTo(p) < 25.0) {
                    Vec3d away = new Vec3d(le.getX() - p.getX(), 0.0, le.getZ() - p.getZ());
                    if (away.lengthSquared() > 1.0E-4) {
                        le.setVelocity(away.normalize().multiply(0.6).add(0.0, 0.2, 0.0));
                        le.velocityDirty = true;
                    }
                }
                return false;
            }
            if (attacker instanceof LivingEntity && direct == attacker) {
                float reduced = amount * (s.form == SukunaForm.GOJO_AWAKENED ? 0.15F : 0.25F);
                s.ce = Math.max(0.0, s.ce - amount * 0.5);
                GojoTasks.infinityFlash(gw, p);
                reentry = true;
                try {
                    p.damage(gw, source, reduced);
                } finally {
                    reentry = false;
                }
                return false;
            }
            return true;
        }

        if (!s.wheel) return true;

        ServerWorld w = Fx.world(p);
        Adaptation.Result r = s.adaptation.hit(source.getName(), w.getTime());
        if (r.stepped()) {
            s.wheelTarget += 360.0F / SukunaMod.config.adaptationSteps;
            Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 1.0F, 1.5F);
            if (r.completed()) {
                Fx.sound(w, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 0.6F);
                p.sendMessage(Text.translatable("sukuna.msg.adapted", Text.literal(source.getName())), true);
            }
        }
        if (r.multiplier() >= 0.999F) return true;
        if (r.multiplier() <= 0.001F) {
            Fx.puff(w, ParticleTypes.ENCHANTED_HIT, p.getX(), p.getY() + 1.0, p.getZ(), 10, 0.4, 0.7, 0.4, 0.08);
            return false;
        }

        reentry = true;
        try {
            p.damage(w, source, amount * r.multiplier());
        } finally {
            reentry = false;
        }
        return false;
    }
}
