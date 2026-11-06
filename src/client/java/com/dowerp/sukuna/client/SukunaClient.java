package com.dowerp.sukuna.client;

import com.dowerp.sukuna.Ability;
import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.client.model.DharmaWheelModel;
import com.dowerp.sukuna.client.model.ExtraArmsModel;
import com.dowerp.sukuna.entity.ModEntities;
import com.dowerp.sukuna.net.AbilityPayload;
import com.dowerp.sukuna.net.AnimPayload;
import com.dowerp.sukuna.net.DomainMeterPayload;
import com.dowerp.sukuna.net.M1Payload;
import com.dowerp.sukuna.net.HoldPayload;
import com.dowerp.sukuna.net.RemoteStatePayload;
import com.dowerp.sukuna.net.StatePayload;
import com.dowerp.sukuna.client.model.GearModels;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class SukunaClient implements ClientModInitializer {
    private static int defaultKey(Ability a) {
        boolean legacy = SukunaMod.config.legacyBindings;
        return switch (a) {
            case DOMAIN_EXPANSION -> legacy ? GLFW.GLFW_KEY_R : GLFW.GLFW_KEY_G;
            case ULTIMATE_SLASH -> legacy ? GLFW.GLFW_KEY_G : GLFW.GLFW_KEY_B;
            case TRANSFORM -> GLFW.GLFW_KEY_U;
            case PRIMARY_ATTACK -> legacy ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_Z;
            case SECONDARY_ATTACK -> legacy ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_X;
            case UTILITY_TECHNIQUE -> legacy ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_C;
            case SPECIAL_ATTACK -> legacy ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_V;
            case REVERSE_CURSED -> legacy ? GLFW.GLFW_KEY_UNKNOWN : GLFW.GLFW_KEY_Y;
            default -> GLFW.GLFW_KEY_UNKNOWN;
        };
    }

    public static Ability doubleTapFor(SukunaForm form, Ability slot) {
        Ability extra = null;
        if (form == SukunaForm.SUKUNA) {
            extra = switch (slot) {
                case PRIMARY_ATTACK -> Ability.DISMANTLE_STORM;
                case SECONDARY_ATTACK -> Ability.BLACK_FLASH;
                case UTILITY_TECHNIQUE -> Ability.WHEEL_ADAPTATION;
                case SPECIAL_ATTACK -> Ability.SUMMON_MAHORAGA;
                default -> null;
            };
        } else if (form == SukunaForm.HEIAN) {
            extra = switch (slot) {
                case PRIMARY_ATTACK -> Ability.DISMANTLE_STORM;
                case SECONDARY_ATTACK -> Ability.FOUR_ARM_FLURRY;
                case UTILITY_TECHNIQUE -> Ability.BLACK_FLASH;
                default -> null;
            };
        } else if (form == SukunaForm.CHOSO_WING) {
            extra = switch (slot) {
                case PRIMARY_ATTACK -> Ability.BLACK_FLASH;
                case SECONDARY_ATTACK -> Ability.DISMANTLE_STORM;
                case UTILITY_TECHNIQUE -> Ability.OPEN_DOMAIN;
                default -> null;
            };
        }
        return extra != null && extra.availableIn(form) ? extra : null;
    }

    public static String shortName(SukunaForm form, Ability a, boolean open) {
        if (form.isGojo()) {
            return switch (a) {
                case PRIMARY_ATTACK -> "Blue";
                case SECONDARY_ATTACK -> "Red";
                case UTILITY_TECHNIQUE -> "Combo";
                case SPECIAL_ATTACK -> "Kick";
                case DOMAIN_EXPANSION -> form == SukunaForm.GOJO_AWAKENED ? "Void" : "0.2s";
                case ULTIMATE_SLASH -> "Purple";
                case REVERSE_CURSED -> "RCT";
                default -> a.name().substring(0, 3);
            };
        }
        if (form.isMaki()) {
            return switch (a) {
                case PRIMARY_ATTACK -> "Combo";
                case SECONDARY_ATTACK -> "Dash";
                case UTILITY_TECHNIQUE -> "Sense";
                case SPECIAL_ATTACK -> "Katana";
                case ULTIMATE_SLASH -> "Cut";
                default -> a.name().substring(0, 3);
            };
        }
        if (form.isTodo()) {
            return switch (a) {
                case PRIMARY_ATTACK -> "Boogie";
                case SECONDARY_ATTACK -> "Slam";
                case UTILITY_TECHNIQUE -> "Clap";
                case SPECIAL_ATTACK -> "Boost";
                case ULTIMATE_SLASH -> "Max";
                case REVERSE_CURSED -> "RCT";
                default -> a.name().substring(0, 3);
            };
        }
        if (form.isChoso()) {
            return switch (a) {
                case PRIMARY_ATTACK -> "Pierce";
                case SECONDARY_ATTACK -> "Meteor";
                case UTILITY_TECHNIQUE -> "Blade";
                case SPECIAL_ATTACK -> "Scale";
                case DOMAIN_EXPANSION -> "Harden";
                case REVERSE_CURSED -> "Coag";
                case ULTIMATE_SLASH -> "Nova";
                case OPEN_DOMAIN -> "Exorc";
                case DISMANTLE_STORM -> "Wings";
                case BLACK_FLASH -> "Flood";
                default -> a.name().substring(0, 3);
            };
        }
        if (form == SukunaForm.YUJI) {
            return switch (a) {
                case PRIMARY_ATTACK -> "Fist";
                case SECONDARY_ATTACK -> "Kick";
                case UTILITY_TECHNIQUE -> "Flash";
                case SPECIAL_ATTACK -> "Boost";
                case DOMAIN_EXPANSION -> "Domain";
                case REVERSE_CURSED -> "RCT";
                case ULTIMATE_SLASH -> "Combo";
                default -> a.name().substring(0, 3);
            };
        }
        boolean heian = form == SukunaForm.HEIAN;
        return switch (a) {
            case PRIMARY_ATTACK -> heian ? "Kamai" : "Dismtl";
            case SECONDARY_ATTACK -> heian ? "Kamut" : "Cleave";
            case UTILITY_TECHNIQUE -> "Web";
            case SPECIAL_ATTACK -> "Fuga";
            case ULTIMATE_SLASH -> "World";
            case DOMAIN_EXPANSION -> "Shrine";
            case WHEEL_ADAPTATION -> "Wheel";
            case SUMMON_MAHORAGA -> "Mahor";
            case REVERSE_CURSED -> "RCT";
            case OPEN_DOMAIN -> "Open";
            case DISMANTLE_STORM -> "Storm";
            case BLACK_FLASH -> "Flash";
            case FOUR_ARM_FLURRY -> "Flurry";
            default -> a.name().substring(0, 3);
        };
    }

    public static java.util.List<Ability> slots(SukunaForm form) {
        java.util.List<Ability> out = new java.util.ArrayList<>();
        if (!SukunaMod.config.legacyBindings) {
            for (Ability a : new Ability[] {Ability.PRIMARY_ATTACK, Ability.SECONDARY_ATTACK, Ability.UTILITY_TECHNIQUE, Ability.SPECIAL_ATTACK}) {
                if (a.availableIn(form)) out.add(a);
            }
            return out;
        }
        for (Ability a : Ability.values()) {
            if (a == Ability.DOMAIN_EXPANSION || a == Ability.ULTIMATE_SLASH || a == Ability.TRANSFORM) continue;
            if (a.availableIn(form)) out.add(a);
        }
        return out;
    }

    private static KeyBinding selectKey;
    private static KeyBinding adminKey;
    private static int heldMask;
    private static final int DOUBLE_TAP_TICKS = 6;
    private static final int M1_INTERVAL_TICKS = 10;
    private static final int[] PENDING_TAP = new int[Ability.values().length];
    private static int lastM1 = -1000;

    static {
        java.util.Arrays.fill(PENDING_TAP, -1);
    }
    private static final KeyBinding[] KEYS = new KeyBinding[Ability.values().length];

    public static String keyLabel(Ability a) {
        int idx = SukunaMod.config.legacyBindings ? slots(ClientState.form).indexOf(a) : -1;
        if (idx >= 0) return String.valueOf(idx + 1);
        KeyBinding k = KEYS[a.ordinal()];
        if (k == null) return "?";
        String key = k.getBoundKeyTranslationKey();
        String tail = key.substring(key.lastIndexOf('.') + 1);
        if (key.startsWith("key.mouse")) return "M" + tail;
        return tail.length() <= 2 ? tail.toUpperCase(java.util.Locale.ROOT) : tail.substring(0, 1).toUpperCase(java.util.Locale.ROOT) + tail.substring(1);
    }

    private static void infoLine(net.minecraft.client.MinecraftClient client, String key, Ability a, SukunaForm form, boolean open) {
        int cd = a.cooldownFor(form, open) / 20;
        net.minecraft.text.MutableText line = net.minecraft.text.Text.literal(key).formatted(net.minecraft.util.Formatting.YELLOW)
            .append(net.minecraft.text.Text.literal(shortName(form, a, open) + " ").formatted(net.minecraft.util.Formatting.AQUA))
            .append(net.minecraft.text.Text.translatable(a.nameKey(form, open)).formatted(net.minecraft.util.Formatting.WHITE))
            .append(net.minecraft.text.Text.literal("  ").append(net.minecraft.text.Text.translatable("sukuna.info.cost", a.costFor(form, open), cd)).formatted(net.minecraft.util.Formatting.GRAY));
        client.player.sendMessage(line, false);
    }

    private static void showCharacterInfo(net.minecraft.client.MinecraftClient client) {
        if (client.player == null) return;
        com.dowerp.sukuna.SukunaCharacter[] chars = com.dowerp.sukuna.SukunaCharacter.values();
        int ci = Math.max(0, Math.min(chars.length - 1, ClientState.character));
        com.dowerp.sukuna.SukunaCharacter c = chars[ci];
        SukunaForm form = ClientState.form;
        boolean open = ClientState.has(StatePayload.OPEN_READY);
        client.player.sendMessage(net.minecraft.text.Text.literal("----------").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
        client.player.sendMessage(net.minecraft.text.Text.translatable("sukuna.info.character", c.displayName(), form.displayName()).formatted(net.minecraft.util.Formatting.GOLD), false);
        client.player.sendMessage(net.minecraft.text.Text.translatable("sukuna.info." + c.id).formatted(net.minecraft.util.Formatting.GRAY), false);
        java.util.List<Ability> slots = slots(form);
        boolean legacy = SukunaMod.config.legacyBindings;
        for (int i = 0; i < slots.size(); i++) {
            infoLine(client, "[" + keyLabel(slots.get(i)) + "] ", slots.get(i), form, open);
            Ability extra = legacy ? null : doubleTapFor(form, slots.get(i));
            if (extra != null) {
                infoLine(client, "[" + keyLabel(slots.get(i)) + "x2] ", extra, form, open);
            }
        }
        if (Ability.DOMAIN_EXPANSION.availableIn(form)) {
            infoLine(client, "[" + keyLabel(Ability.DOMAIN_EXPANSION) + "] ", Ability.DOMAIN_EXPANSION, form, open);
        }
        if (Ability.ULTIMATE_SLASH.availableIn(form)) {
            infoLine(client, "[" + keyLabel(Ability.ULTIMATE_SLASH) + "] ", Ability.ULTIMATE_SLASH, form, open);
        }
        if (!legacy && Ability.REVERSE_CURSED.availableIn(form)) {
            infoLine(client, "[Shift+" + keyLabel(Ability.REVERSE_CURSED) + "] ", Ability.REVERSE_CURSED, form, open);
        }
        client.player.sendMessage(net.minecraft.text.Text.translatable(legacy ? "sukuna.info.keys" : "sukuna.info.keys_new").formatted(net.minecraft.util.Formatting.DARK_GRAY), false);
    }

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(SukunaMod.id("abilities"));
        Ability[] all = Ability.values();
        for (int i = 0; i < all.length; i++) {
            KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyBinding(all[i].keyName(), InputUtil.Type.KEYSYM, defaultKey(all[i]), category));
        }

        selectKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sukuna.select_character", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, category));
        adminKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.sukuna.admin_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_GRAVE_ACCENT, category));

        ClientPlayNetworking.registerGlobalReceiver(StatePayload.ID, (payload, context) -> ClientState.apply(payload));
        ClientPlayNetworking.registerGlobalReceiver(DomainMeterPayload.ID, (payload, context) -> ClientState.meterTenths = payload.tenths());
        ClientPlayNetworking.registerGlobalReceiver(RemoteStatePayload.ID, (payload, context) -> RemoteFx.apply(payload));
        ClientPlayNetworking.registerGlobalReceiver(AnimPayload.ID, (payload, context) -> {
            net.minecraft.client.MinecraftClient mc = context.client();
            if (mc.world == null) return;
            net.minecraft.entity.Entity e = mc.world.getEntityById(payload.entityId());
            Ability[] abilities = Ability.values();
            SukunaForm[] forms = SukunaForm.values();
            if (e instanceof net.minecraft.client.network.AbstractClientPlayerEntity pl && payload.ability() >= abilities.length) {
                CastAnimations.playNamed(pl, payload.ability() - abilities.length);
                return;
            }
            if (e instanceof net.minecraft.client.network.AbstractClientPlayerEntity pl
                && payload.ability() >= 0 && payload.ability() < abilities.length
                && payload.form() >= 0 && payload.form() < forms.length) {
                CastAnimations.playOn(pl, abilities[payload.ability()], forms[payload.form()]);
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientState.reset();
            RemoteFx.reset();
            heldMask = 0;
            java.util.Arrays.fill(PENDING_TAP, -1);
        });
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (client.player == null || client.currentScreen != null || !SukunaHud.active()) return;
            java.util.List<Ability> slots = slots(ClientState.form);
            if (SukunaMod.config.legacyBindings) {
                for (int i = 0; i < 9; i++) {
                    while (client.options.hotbarKeys[i].wasPressed()) {
                        if (i < slots.size()) {
                            ClientPlayNetworking.send(new AbilityPayload(slots.get(i).ordinal()));
                        }
                    }
                }
            }
            while (client.options.inventoryKey.wasPressed()) {
                showCharacterInfo(client);
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientState.tickClient();
            if (client.player == null) {
                return;
            }
            while (adminKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new AdminScreen());
                }
            }
            while (selectKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new CharacterSelectScreen());
                }
            }
            int mask = 0;
            for (int i = 0; i < KEYS.length; i++) {
                if (KEYS[i].isPressed()) mask |= 1 << i;
            }
            if (SukunaMod.config.legacyBindings && client.currentScreen == null && SukunaHud.active()) {
                java.util.List<Ability> slots = slots(ClientState.form);
                for (int i = 0; i < 9 && i < slots.size(); i++) {
                    if (client.options.hotbarKeys[i].isPressed()) mask |= 1 << slots.get(i).ordinal();
                }
            }
            if (mask != heldMask) {
                heldMask = mask;
                ClientPlayNetworking.send(new HoldPayload(mask));
            }
            boolean legacy = SukunaMod.config.legacyBindings;
            int now = ClientState.clientTick;
            for (int i = 0; i < KEYS.length; i++) {
                while (KEYS[i].wasPressed()) {
                    Ability a = all[i];
                    if (!legacy && a == Ability.REVERSE_CURSED && !client.player.isSneaking()) {
                        continue;
                    }
                    Ability extra = legacy ? null : doubleTapFor(ClientState.form, a);
                    if (extra != null) {
                        if (PENDING_TAP[i] >= 0 && now - PENDING_TAP[i] <= DOUBLE_TAP_TICKS) {
                            PENDING_TAP[i] = -1;
                            ClientPlayNetworking.send(new AbilityPayload(extra.ordinal()));
                        } else {
                            PENDING_TAP[i] = now;
                        }
                        continue;
                    }
                    ClientPlayNetworking.send(new AbilityPayload(i));
                }
                if (PENDING_TAP[i] >= 0 && now - PENDING_TAP[i] > DOUBLE_TAP_TICKS) {
                    PENDING_TAP[i] = -1;
                    ClientPlayNetworking.send(new AbilityPayload(i));
                }
            }
            if (!legacy && client.currentScreen == null && SukunaHud.active() && client.options.attackKey.isPressed()
                && (client.player.getMainHandStack().isEmpty() || ClientState.form == SukunaForm.MAKI) && now - lastM1 >= M1_INTERVAL_TICKS) {
                lastM1 = now;
                ClientPlayNetworking.send(M1Payload.INSTANCE);
            }
            if (client.player.age % 100 == 0) {
                RemoteFx.prune();
            }
        });

        CastAnimations.init();
        EntityModelLayerRegistry.registerModelLayer(MahoragaRenderer.WHEEL_LAYER, DharmaWheelModel::getTexturedModelData);
        EntityModelLayerRegistry.registerModelLayer(MahoragaRenderer.ARMS_LAYER, ExtraArmsModel::getTexturedModelData);

        EntityModelLayerRegistry.registerModelLayer(GearModels.WINGS_LAYER, GearModels::wings);
        EntityModelLayerRegistry.registerModelLayer(GearModels.SKULL_LAYER, GearModels::skull);
        EntityModelLayerRegistry.registerModelLayer(GearModels.HAIR_LAYER, GearModels::hair);
        EntityModelLayerRegistry.registerModelLayer(GearModels.HOOD_LAYER, GearModels::hood);
        EntityModelLayerRegistry.registerModelLayer(GearModels.BLINDFOLD_LAYER, GearModels::blindfold);
        EntityModelLayerRegistry.registerModelLayer(GearModels.CLOAK_LAYER, GearModels::cloak);
        EntityModelLayerRegistry.registerModelLayer(GearModels.KAMUTOKE_LAYER, GearModels::kamutoke);

        EntityRendererRegistry.register(ModEntities.MAHORAGA, MahoragaRenderer::new);
        EntityRendererRegistry.register(ModEntities.SPRITE, SpriteFxRenderer::new);
        EntityRendererRegistry.register(ModEntities.ORB_BLUE, ctx -> new OrbRenderer(ctx, "orb_blue"));
        EntityRendererRegistry.register(ModEntities.ORB_BLUE_SMALL, ctx -> new OrbRenderer(ctx, "orb_blue_small"));
        EntityRendererRegistry.register(ModEntities.ORB_RED_SMALL, ctx -> new OrbRenderer(ctx, "orb_red_small"));
        EntityRendererRegistry.register(ModEntities.ORB_RED, ctx -> new OrbRenderer(ctx, "orb_red"));
        EntityRendererRegistry.register(ModEntities.ORB_PURPLE, ctx -> new OrbRenderer(ctx, "orb_purple"));
        EntityRendererRegistry.register(ModEntities.ORB_PURPLE_SMALL, ctx -> new OrbRenderer(ctx, "orb_purple_small"));

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, renderer, helper, ctx) -> {
            if (renderer instanceof PlayerEntityRenderer playerRenderer) {
                helper.register(new WheelFeatureRenderer(playerRenderer, new DharmaWheelModel(ctx.getPart(MahoragaRenderer.WHEEL_LAYER))));
                helper.register(new HeianArmsFeatureRenderer(playerRenderer, new ExtraArmsModel(ctx.getPart(MahoragaRenderer.ARMS_LAYER))));
                helper.register(new GearFeatureRenderer(playerRenderer,
                    ctx.getPart(GearModels.HOOD_LAYER), ctx.getPart(GearModels.BLINDFOLD_LAYER),
                    ctx.getPart(GearModels.CLOAK_LAYER), ctx.getPart(GearModels.KAMUTOKE_LAYER), ctx.getPart(GearModels.HAIR_LAYER), ctx.getPart(GearModels.SKULL_LAYER), ctx.getPart(GearModels.WINGS_LAYER)));
            }
        });
        HudElementRegistry.addLast(SukunaMod.id("hud"), SukunaHud::render);

        for (net.minecraft.util.Identifier id : new net.minecraft.util.Identifier[] {
            VanillaHudElements.HOTBAR, VanillaHudElements.HEALTH_BAR, VanillaHudElements.FOOD_BAR,
            VanillaHudElements.ARMOR_BAR, VanillaHudElements.INFO_BAR, VanillaHudElements.EXPERIENCE_LEVEL,
            VanillaHudElements.HELD_ITEM_TOOLTIP}) {
            HudElementRegistry.replaceElement(id, original -> (ctx, tickCounter) -> {
                if (!SukunaHud.active()) {
                    original.render(ctx, tickCounter);
                }
            });
        }
    }
}
