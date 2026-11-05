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

        renderInfo(ctx, tr, form);

        if (ClientState.has(StatePayload.IN_BATTLE)) {
            ctx.drawCenteredTextWithShadow(tr, Text.translatable("sukuna.battle"), w / 2, 6, 0xFFFF5050);
        }
        List<Ability> abilities = new ArrayList<>(SukunaClient.slots(form));
        if (Ability.DOMAIN_EXPANSION.availableIn(form)) abilities.add(Ability.DOMAIN_EXPANSION);
        if (Ability.ULTIMATE_SLASH.availableIn(form)) abilities.add(Ability.ULTIMATE_SLASH);
        int n = abilities.size() + 1;
        int gap = 2;
        int slotW = Math.max(18, Math.min(30, (w - 24 - (n - 1) * gap) / n));
        int slotH = 26;
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
            default -> 0xFFC4182A;
        };
        bar(ctx, bx, ceY, barW, 9, ceFrac, 0xFF0A0A1A, ceColor, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(tr, Text.literal("CE " + ClientState.ce + " / " + ClientState.maxCe), w / 2, ceY + 1, 0xFFFFFFFF);

        if (ClientState.meterTenths >= 0) {
            float mf = Math.max(0.0F, Math.min(1.0F, ClientState.meterTenths / 1000.0F));
            int meterY = ceY - 9;
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
        int left = isTransform ? 0 : ClientState.cooldownLeft(a);
        boolean open = ClientState.has(StatePayload.OPEN_READY);
        int cd = isTransform ? 0 : a.cooldownFor(form, open);
        boolean active = !isTransform && isActive(a, charging);
        boolean noEnergy = !isTransform && ClientState.ce < a.costFor(form, open);
        boolean locked = false;

        int border = active ? 0xFFF2C14E : locked ? 0xFF554422 : noEnergy ? 0xFFAA2222 : isTransform ? 0xFF7A5CC8 : 0xFF55556A;
        ctx.fill(x, y, x + w, y + h, border);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xD0101018);

        if (left > 0 && cd > 0) {
            int fh = (int) ((h - 2) * Math.min(1.0F, left / (float) cd));
            ctx.fill(x + 1, y + 1, x + w - 1, y + 1 + fh, 0xA0802030);
        }

        String key = "[" + SukunaClient.keyLabel(a) + "]";
        String keyShort = SukunaClient.keyLabel(a);
        if (keyShort.length() > 3) keyShort = keyShort.substring(0, 3);
        int keyColor = (left > 0 || locked) ? 0xFF888888 : 0xFFF2C14E;
        ctx.drawTextWithShadow(tr, keyShort, x + (w - tr.getWidth(keyShort)) / 2, y + 3, keyColor);

        String line2;
        int line2Color;
        if (isTransform) {
            line2 = "";
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
        ctx.drawTextWithShadow(tr, line2, x + Math.max(1, (w - tr.getWidth(line2)) / 2), y + 14, line2Color);
    }

    private static String abbreviation(String name, int slotW) {
        int len = slotW >= 24 ? 3 : 2;
        return name.length() <= len ? name : name.substring(0, len);
    }

    private static void renderInfo(DrawContext ctx, TextRenderer tr, SukunaForm form) {
        int x = 10;
        int line = 10;
        ctx.drawTextWithShadow(tr, form.displayName(), x, line, 0xFFFFFFFF);
        line += 12;

        if (form == SukunaForm.SUKUNA && ClientState.kills < SukunaState.HEIAN_KILLS) {
            ctx.drawTextWithShadow(tr, Text.translatable("sukuna.hud.kills", ClientState.kills, SukunaState.HEIAN_KILLS), x, line, 0xFFFFAA00);
            line += 10;
        }
        int timer = ClientState.formSecondsLeft();
        if (timer > 0) {
            ctx.drawTextWithShadow(tr, Text.translatable("sukuna.hud.timer", timer), x, line, 0xFFFFD27A);
            line += 10;
        }
        if (ClientState.has(StatePayload.WHEEL)) {
            ctx.drawTextWithShadow(tr, Text.translatable("sukuna.hud.wheel", ClientState.adaptation), x, line, 0xFFF2C14E);
            line += 10;
        }
        if (ClientState.has(StatePayload.DOMAIN_CASTER)) {
            ctx.drawTextWithShadow(tr, Text.translatable("sukuna.hud.domain", ClientState.domainLeft / 20), x, line, 0xFFE0303C);
        }
    }

    private static boolean isActive(Ability a, Ability charging) {
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
