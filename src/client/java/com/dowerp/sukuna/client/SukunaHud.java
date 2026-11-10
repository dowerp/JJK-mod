package com.dowerp.sukuna.client;

import com.dowerp.sukuna.Ability;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.SukunaState;
import com.dowerp.sukuna.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SukunaHud {
    private SukunaHud() {}

    public static boolean active() {
        return ClientState.has(StatePayload.VESSEL);
    }

    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();

        if (ClientState.has(StatePayload.IN_DOMAIN)) {
            ctx.fill(0, 0, w, h, 0x30200000);
            ctx.fillGradient(0, 0, w, h / 4, 0x90000000, 0x00000000);
            ctx.fillGradient(0, h - h / 4, w, h, 0x00000000, 0x90000000);
        }

        if (!active() || mc.player == null) return;

        TextRenderer tr = mc.textRenderer;
        SukunaForm form = ClientState.form;

        List<Ability> abilities = new ArrayList<>(SukunaClient.slots(form));
        if (Ability.DOMAIN_EXPANSION.availableIn(form)) abilities.add(Ability.DOMAIN_EXPANSION);
        if (Ability.ULTIMATE_SLASH.availableIn(form)) abilities.add(Ability.ULTIMATE_SLASH);
        if (Ability.SIMPLE_DOMAIN.availableIn(form)) abilities.add(Ability.SIMPLE_DOMAIN);
        if (Ability.FALLING_BLOSSOM.availableIn(form)) abilities.add(Ability.FALLING_BLOSSOM);
        if (Ability.INFINITY_TOGGLE.availableIn(form)) abilities.add(Ability.INFINITY_TOGGLE);
        int n = abilities.size() + 1;
        int gap = 2;
        int slotW = Math.max(16, Math.min(22, (w - 24 - (n - 1) * gap) / n));
        int slotH = 20;
        int total = n * slotW + (n - 1) * gap;
        int x0 = (w - total) / 2;
        int y0 = h - slotH - 4;
        Ability charging = chargingAbility();

        for (int i = 0; i < n; i++) {
            int x = x0 + i * (slotW + gap);
            boolean isTransform = i == n - 1;
            Ability a = isTransform ? Ability.TRANSFORM : abilities.get(i);
            renderSlot(ctx, tr, form, a, isTransform, x, y0, slotW, slotH, charging);
        }

        renderRightText(ctx, tr, form, x0 + total + 8, y0, slotH, w);
        if (form == SukunaForm.YUTA_RIKA) {
            renderRikaBar(ctx, tr, form, w, h, charging);
        }

        int barW = Math.min(240, total);
        int bx = (w - barW) / 2;
        int hpY = y0 - 14;
        float hp = mc.player.getHealth();
        float maxHp = Math.max(1.0F, mc.player.getMaxHealth());
        float hpFrac = Math.max(0.0F, Math.min(1.0F, hp / maxHp));
        boolean low = hpFrac < 0.25F;
        int hpColor = low && (ClientState.clientTick / 4) % 2 == 0 ? 0xFFFF3D00 : 0xFFB71C1C;
        bar(ctx, bx, hpY, barW, 11, hpFrac, 0xFF1A0A0A, hpColor, 0xFFE57373);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("HP " + Math.round(hp) + " / " + Math.round(maxHp)), w / 2, hpY + 2, 0xFFFFFFFF);

        int ceY = hpY - 12;
        float ceFrac = ClientState.maxCe > 0 ? Math.min(1.0F, (float) ClientState.ce / ClientState.maxCe) : 0.0F;
        int ceColor = switch (form) {
            case YUJI, GOJO -> 0xFF1B82FF;
            case GOJO_AWAKENED -> 0xFF9B30FF;
            case TODO, TODO_AWAKENED -> 0xFFE08A00;
            case MEGUMI -> 0xFF2D4FBF;
            case MEGUMI_BEASTS -> 0xFF3F7BFF;
            case YUTA -> 0xFFE8E8F0;
            case YUTA_RIKA -> 0xFF4FC3F7;
            default -> 0xFFC4182A;
        };
        boolean hasCe = ClientState.maxCe > 0;
        if (hasCe) {
            bar(ctx, bx, ceY, barW, 9, ceFrac, 0xFF0A0A1A, ceColor, 0xFFFFFFFF);
            ctx.drawCenteredTextWithShadow(tr, Text.literal("CE " + ClientState.ce + " / " + ClientState.maxCe), w / 2, ceY + 1, 0xFFFFFFFF);
        }

        if (ClientState.meterTenths >= 0) {
            float mf = Math.max(0.0F, Math.min(1.0F, ClientState.meterTenths / 1000.0F));
            int meterY = (hasCe ? ceY : hpY) - 9;
            boolean full = mf >= 0.999F;
            int mColor = full && (ClientState.clientTick / 5) % 2 == 0 ? 0xFFD05CFF : 0xFF8E24AA;
            bar(ctx, bx, meterY, barW, 5, mf, 0xFF14081A, mColor, 0xFFE1BEE7);
            ctx.drawCenteredTextWithShadow(tr, Text.translatable("sukuna.hud.domain_meter", Math.round(mf * 100.0F)), w / 2, meterY - 9, 0xFFE1BEE7);
        }
    }

    private static void bar(DrawContext ctx, int x, int y, int w, int h, float frac, int back, int fill, int shine) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF000000);
        ctx.fill(x, y, x + w, y + h, back);
        int fw = (int) (w * frac);
        if (fw > 0) {
            ctx.fill(x, y, x + fw, y + h, fill);
            ctx.fill(x, y, x + fw, y + 1, shine & 0x80FFFFFF | 0x80000000);
        }
    }

    private static void renderSlot(DrawContext ctx, TextRenderer tr, SukunaForm form, Ability a, boolean isTransform,
                                   int x, int y, int w, int h, Ability charging) {
        boolean awakenedNow = isTransform && form.awakened();
        int left = awakenedNow ? 0 : ClientState.cooldownLeft(a);
        boolean open = ClientState.has(StatePayload.OPEN_READY);
        int cd = isTransform ? (form.awakenable() ? SukunaState.FORM_CHARGE_TICKS : Math.max(left, 1)) : a.cooldownFor(form, open);
        boolean active = awakenedNow || (!isTransform && isActive(a, charging));
        boolean noEnergy = !isTransform && ClientState.ce < a.costFor(form, open);
        boolean locked = false;

        // Без рамок слотов: только клавиша, название и тонкая полоса под ними.
        int barY = y + h - 2;
        int trackColor = isTransform ? 0x607A5CC8 : 0x50808090;
        ctx.fill(x, barY, x + w, barY + 2, trackColor);
        if (active) {
            ctx.fill(x, barY, x + w, barY + 2, 0xFFF2C14E);
        } else if (left > 0 && cd > 0) {
            int fw = (int) (w * Math.min(1.0F, left / (float) cd));
            ctx.fill(x, barY, x + fw, barY + 2, 0xFFE03040);
        } else if (noEnergy) {
            ctx.fill(x, barY, x + w, barY + 2, 0xFFAA2222);
        }

        String key = "[" + SukunaClient.keyLabel(a) + "]";
        String keyShort = SukunaClient.keyLabel(a);
        if (keyShort.length() > 3) keyShort = keyShort.substring(0, 3);
        int keyColor = (left > 0 || locked) ? 0xFF888888 : 0xFFF2C14E;
        ctx.drawTextWithShadow(tr, keyShort, x + (w - tr.getWidth(keyShort)) / 2, y, keyColor);

        String line2;
        int line2Color;
        if (awakenedNow) {
            int secs = ClientState.formSecondsLeft();
            line2 = secs > 0 ? secs + "s" : "*";
            line2Color = 0xFFF2C14E;
        } else if (isTransform && left <= 0) {
            line2 = form.isMaki() ? "Wpn" : form.awakenable() ? "Awk" : "Form";
            line2Color = 0xFFB9A6FF;
        } else if (active) {
            line2 = "*";
            line2Color = 0xFFF2C14E;
        } else if (left > 0) {
            line2 = left >= 200 ? String.valueOf((left + 19) / 20) : String.format(Locale.ROOT, "%.1f", left / 20.0F);
            line2Color = 0xFFFF6060;
        } else {
            String nm = SukunaClient.shortName(form, a, open);
            while (nm.length() > 1 && tr.getWidth(nm) > w - 2) nm = nm.substring(0, nm.length() - 1);
            line2 = nm;
            line2Color = noEnergy ? 0xFFFF7777 : 0xFFD8D8E8;
        }
        ctx.drawTextWithShadow(tr, line2, x + Math.max(0, (w - tr.getWidth(line2)) / 2), y + 9, line2Color);
    }

    private static String abbreviation(String name, int slotW) {
        int len = slotW >= 24 ? 3 : 2;
        return name.length() <= len ? name : name.substring(0, len);
    }

    /** Тексты справа от ряда способностей: бой, убийства, таймеры, колесо, территория, перегрев, зона. */
    private static void renderRightText(DrawContext ctx, TextRenderer tr, SukunaForm form, int x, int rowY, int rowH, int screenW) {
        List<Text> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (ClientState.has(StatePayload.IN_BATTLE)) {
            lines.add(Text.translatable("sukuna.battle"));
            colors.add(0xFFFF5050);
        }
        if (form == SukunaForm.SUKUNA && ClientState.kills < SukunaState.HEIAN_KILLS) {
            lines.add(Text.translatable("sukuna.hud.kills", ClientState.kills, SukunaState.HEIAN_KILLS));
            colors.add(0xFFFFAA00);
        }
        int timer = ClientState.formSecondsLeft();
        if (timer > 0) {
            lines.add(Text.translatable("sukuna.hud.timer", timer));
            colors.add(0xFFFFD27A);
        }
        if (ClientState.has(StatePayload.WHEEL)) {
            lines.add(Text.translatable("sukuna.hud.wheel", ClientState.adaptation));
            colors.add(0xFFF2C14E);
        }
        if (ClientState.has(StatePayload.DOMAIN_CASTER)) {
            lines.add(Text.translatable("sukuna.hud.domain", ClientState.domainLeft / 20));
            colors.add(0xFFE0303C);
        }
        if (ClientState.bfZoneSeconds > 0) {
            lines.add(Text.translatable("sukuna.hud.bf_zone", ClientState.bfStreak, ClientState.bfZoneSeconds));
            colors.add(0xFFFF3B3B);
        }
        if (ClientState.heatPercent > 0) {
            lines.add(Text.translatable("sukuna.hud.heat", ClientState.heatPercent));
            colors.add(0xFFFFA726);
        }
        if (lines.isEmpty()) return;
        int y = rowY + rowH - lines.size() * 10;
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawTextWithShadow(tr, lines.get(i), x, y + i * 10, colors.get(i));
        }
    }

    /** Отдельная небольшая вертикальная панель способностей Рики у правого края экрана. */
    private static void renderRikaBar(DrawContext ctx, TextRenderer tr, SukunaForm form, int w, int h, Ability charging) {
        Ability[] rika = {Ability.RIKA_FIST, Ability.RIKA_GRAB, Ability.RIKA_GUARD, Ability.RIKA_RUSH};
        int slot = 20;
        int gap = 4;
        int total = rika.length * slot + (rika.length - 1) * gap;
        int x = w - slot - 8;
        int y = h / 2 - total / 2;
        ctx.drawCenteredTextWithShadow(tr, Text.translatable("sukuna.hud.rika"), x + slot / 2, y - 12, 0xFFB9E6FF);
        for (int i = 0; i < rika.length; i++) {
            renderSlot(ctx, tr, form, rika[i], false, x, y + i * (slot + gap), slot, slot, charging);
        }
    }

    private static boolean isActive(Ability a, Ability charging) {
        if (a == Ability.INFINITY_TOGGLE) {
            return !ClientState.has(StatePayload.INFINITY_OFF);
        }
        if (ClientState.form.isChoso()) {
            return switch (a) {
                case SPECIAL_ATTACK -> ClientState.has(StatePayload.RED_SCALE);
                case DOMAIN_EXPANSION -> ClientState.has(StatePayload.HARDEN);
                default -> false;
            };
        }
        return switch (a) {
            case WHEEL_ADAPTATION -> ClientState.has(StatePayload.WHEEL);
            case SUMMON_MAHORAGA -> ClientState.has(StatePayload.MAHORAGA);
            case DOMAIN_EXPANSION -> ClientState.has(StatePayload.DOMAIN_CASTER);
            case SPECIAL_ATTACK, ULTIMATE_SLASH -> a == charging;
            default -> false;
        };
    }

    private static Ability chargingAbility() {
        if (!ClientState.has(StatePayload.CHARGING)) return null;
        Ability best = null;
        int bestElapsed = Integer.MAX_VALUE;
        for (Ability a : new Ability[] {Ability.SPECIAL_ATTACK, Ability.ULTIMATE_SLASH}) {
            int left = ClientState.cooldownLeft(a);
            if (left <= 0) continue;
            int elapsed = a.cooldownFor(ClientState.form) - left;
            if (elapsed < bestElapsed) {
                bestElapsed = elapsed;
                best = a;
            }
        }
        return best;
    }
}
