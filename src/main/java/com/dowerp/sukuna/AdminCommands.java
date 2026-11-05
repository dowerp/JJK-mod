package com.dowerp.sukuna;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Arrays;

/** Админ-меню для операторов: /sukuna menu со ссылками-кнопками в чате. */
public final class AdminCommands {
    private AdminCommands() {
    }

    public static void init() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));
    }

    private static boolean allowed(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) return true;
        MinecraftServer server = src.getServer();
        if (server.getPlayerManager().isOperator(p.getPlayerConfigEntry())) return true;
        src.sendError(Text.translatable("sukuna.admin.denied"));
        return false;
    }

    private static void register(CommandDispatcher<ServerCommandSource> d) {
        d.register(CommandManager.literal("sukuna")
            .then(CommandManager.literal("menu").executes(c -> {
                if (!allowed(c.getSource())) return 0;
                ServerPlayerEntity p = c.getSource().getPlayer();
                if (p == null) return 0;
                p.sendMessage(Text.translatable("sukuna.admin.title").formatted(Formatting.GOLD, Formatting.BOLD), false);
                p.sendMessage(button("sukuna.admin.cd_self", "/sukuna cd").append(Text.literal(" ")).append(button("sukuna.admin.cd_all", "/sukuna cdall")), false);
                p.sendMessage(button("sukuna.admin.ce", "/sukuna ce").append(Text.literal(" ")).append(button("sukuna.admin.nocd", "/sukuna nocd")), false);
                return 1;
            }))
            .then(CommandManager.literal("cd").executes(c -> {
                if (!allowed(c.getSource())) return 0;
                ServerPlayerEntity p = c.getSource().getPlayer();
                if (p == null) return 0;
                clear(p);
                p.sendMessage(Text.translatable("sukuna.admin.done", p.getName()), true);
                return 1;
            }))
            .then(CommandManager.literal("cdall").executes(c -> {
                if (!allowed(c.getSource())) return 0;
                int n = 0;
                for (ServerPlayerEntity p : c.getSource().getServer().getPlayerManager().getPlayerList()) {
                    clear(p);
                    n++;
                }
                c.getSource().sendFeedback(() -> Text.translatable("sukuna.admin.done", "*"), true);
                return n;
            }))
            .then(CommandManager.literal("ce").executes(c -> {
                if (!allowed(c.getSource())) return 0;
                ServerPlayerEntity p = c.getSource().getPlayer();
                if (p == null) return 0;
                SukunaState s = Players.get(p);
                s.ce = s.maxCe();
                return 1;
            }))
            .then(CommandManager.literal("nocd").executes(c -> {
                if (!allowed(c.getSource())) return 0;
                ServerPlayerEntity p = c.getSource().getPlayer();
                if (p == null) return 0;
                SukunaState s = Players.get(p);
                s.noCooldown = !s.noCooldown;
                if (s.noCooldown) Arrays.fill(s.cooldowns, 0L);
                p.sendMessage(Text.translatable(s.noCooldown ? "sukuna.admin.nocd_on" : "sukuna.admin.nocd_off"), true);
                return 1;
            })));
    }

    private static void clear(ServerPlayerEntity p) {
        SukunaState s = Players.get(p);
        Arrays.fill(s.cooldowns, 0L);
        s.busyUntil = 0L;
    }

    private static MutableText button(String key, String command) {
        return Text.translatable(key).setStyle(Style.EMPTY.withColor(Formatting.AQUA).withUnderline(true).withClickEvent(new ClickEvent.RunCommand(command)));
    }
}
