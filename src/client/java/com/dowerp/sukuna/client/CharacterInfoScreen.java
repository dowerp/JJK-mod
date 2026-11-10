package com.dowerp.sukuna.client;

import com.dowerp.sukuna.Ability;
import com.dowerp.sukuna.SukunaCharacter;
import com.dowerp.sukuna.SukunaForm;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Окно сводки о персонаже (клавиша E): описание, формы, техники с ключами, ценой, КД и описанием; вкладка механик. */
public class CharacterInfoScreen extends Screen {
    private record Entry(Text title, int color, List<OrderedText> body) {
    }

    private final SukunaCharacter character;
    private final List<SukunaForm> forms;
    private int tab;
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    public CharacterInfoScreen() {
        super(Text.translatable("sukuna.info.title"));
        SukunaCharacter[] all = SukunaCharacter.values();
        this.character = ClientState.character >= 0 && ClientState.character < all.length ? all[ClientState.character] : SukunaCharacter.ITADORI;
        this.forms = formsOf(character);
        int idx = forms.indexOf(ClientState.form);
        this.tab = Math.max(0, idx);
    }

    private static List<SukunaForm> formsOf(SukunaCharacter c) {
        return switch (c) {
            case ITADORI -> List.of(SukunaForm.YUJI, SukunaForm.SUKUNA, SukunaForm.HEIAN);
            case GOJO -> List.of(SukunaForm.GOJO, SukunaForm.GOJO_AWAKENED);
            case CHOSO -> List.of(SukunaForm.CHOSO, SukunaForm.CHOSO_WING);
            case TODO -> List.of(SukunaForm.TODO, SukunaForm.TODO_AWAKENED);
            case MAKI -> List.of(SukunaForm.MAKI);
            case MEGUMI -> List.of(SukunaForm.MEGUMI, SukunaForm.MEGUMI_BEASTS);
            case YUTA -> List.of(SukunaForm.YUTA, SukunaForm.YUTA_RIKA);
            default -> List.of(ClientState.form);
        };
    }

    @Override
    protected void init() {
        panelW = Math.min(this.width - 24, 440);
        panelH = this.height - 40;
        panelX = (this.width - panelW) / 2;
        panelY = 20;
        int tabs = forms.size() + 1;
        int bw = Math.max(60, (panelW - 12 - (tabs - 1) * 4) / tabs);
        for (int i = 0; i < tabs; i++) {
            final int index = i;
            Text label = i < forms.size() ? Text.literal(forms.get(i).displayName().getString()) : Text.translatable("sukuna.info.tab_mechanics");
            this.addDrawableChild(ButtonWidget.builder(label, b -> {
                tab = index;
                scroll = 0;
            }).dimensions(panelX + 6 + i * (bw + 4), panelY + 24, bw, 18).build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
            .dimensions(panelX + panelW / 2 - 60, panelY + panelH - 22, 120, 18).build());
    }

    private List<OrderedText> wrap(Text text, int width) {
        return this.textRenderer.wrapLines(text, width);
    }

    private static String extraLabel(SukunaForm form, Ability a) {
        if (SukunaMod_legacy()) return null;
        for (Ability slot : new Ability[] {Ability.PRIMARY_ATTACK, Ability.SECONDARY_ATTACK, Ability.UTILITY_TECHNIQUE, Ability.SPECIAL_ATTACK}) {
            if (SukunaClient.doubleTapFor(form, slot) == a) {
                return SukunaClient.keyLabel(slot) + "x2";
            }
        }
        return null;
    }

    private static boolean SukunaMod_legacy() {
        return com.dowerp.sukuna.SukunaMod.config.legacyBindings;
    }

    private static String num(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.1f", v);
    }

    private List<Entry> build(int width) {
        List<Entry> out = new ArrayList<>();
        if (tab >= forms.size()) {
            for (String key : new String[] {"form_timer", "combo", "extras", "heat", "black_flash", "counters", "domain_meter", "m1", "convergence", "rika", "shikigami"}) {
                out.add(new Entry(Text.translatable("sukuna.mech." + key), 0xFFFFD54F, wrap(Text.translatable("sukuna.mech." + key + ".desc"), width)));
            }
            return out;
        }
        SukunaForm form = forms.get(tab);
        String descKey = "sukuna.info." + character.id;
        if (I18n.hasTranslation(descKey)) {
            out.add(new Entry(form.displayName(), 0xFFFFD54F, wrap(Text.translatable(descKey), width)));
        } else {
            out.add(new Entry(form.displayName(), 0xFFFFD54F, List.of()));
        }
        for (Ability a : Ability.values()) {
            if (a == Ability.TRANSFORM || !a.availableIn(form)) continue;
            String key = extraLabel(form, a);
            if (key == null) key = SukunaClient.keyLabel(a);
            StringBuilder head = new StringBuilder("[").append(key).append("] ");
            Text name = Text.translatable(a.nameKey(form));
            int cost = a.costFor(form, false);
            double cd = a.cooldownFor(form, false) / 20.0;
            StringBuilder stats = new StringBuilder("  ");
            if (cost > 0) stats.append("CE ").append(cost).append(" | ");
            stats.append(Text.translatable("sukuna.info.cd_short", num(cd)).getString());
            Text title = Text.literal(head.toString()).append(name).append(Text.literal(stats.toString()));
            String dk = a.nameKey(form) + ".desc";
            List<OrderedText> body = I18n.hasTranslation(dk) ? wrap(Text.translatable(dk), width - 8) : List.of();
            out.add(new Entry(title, 0xFFFFFFFF, body));
        }
        // комбо: слоты 1+3 и 2+4
        Ability[] slots = {Ability.PRIMARY_ATTACK, Ability.SECONDARY_ATTACK, Ability.UTILITY_TECHNIQUE, Ability.SPECIAL_ATTACK};
        int[][] pairs = {{0, 2}, {1, 3}};
        for (int[] pr : pairs) {
            Ability x = slots[pr[0]];
            Ability y = slots[pr[1]];
            if (!x.availableIn(form) || !y.availableIn(form)) continue;
            Text title = Text.translatable("sukuna.combo.title", SukunaClient.keyLabel(x), SukunaClient.keyLabel(y), Text.translatable(x.nameKey(form)), Text.translatable(y.nameKey(form)));
            out.add(new Entry(title, 0xFFFF9E40, wrap(Text.translatable("sukuna.combo.desc"), width - 8)));
        }
        return out;
    }

    private int contentHeight(List<Entry> entries) {
        int h = 0;
        for (Entry e : entries) {
            h += 12 + e.body.size() * 10 + 6;
        }
        return h;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll -= (int) Math.signum(verticalAmount) * 16;
        return true;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xEE0D0D14);
        ctx.fill(panelX, panelY, panelX + panelW, panelY + 1, 0xFFFFD54F);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, character.displayName(), this.width / 2, panelY + 8, 0xFFFFD54F);

        int viewTop = panelY + 48;
        int viewBottom = panelY + panelH - 28;
        int contentW = panelW - 24;
        List<Entry> entries = build(contentW);
        int maxScroll = Math.max(0, contentHeight(entries) - (viewBottom - viewTop));
        scroll = Math.max(0, Math.min(maxScroll, scroll));

        ctx.enableScissor(panelX, viewTop, panelX + panelW, viewBottom);
        int y = viewTop - scroll;
        for (Entry e : entries) {
            ctx.drawTextWithShadow(this.textRenderer, e.title, panelX + 10, y, e.color);
            y += 12;
            for (OrderedText line : e.body) {
                ctx.drawTextWithShadow(this.textRenderer, line, panelX + 18, y, 0xFFB0B0B8);
                y += 10;
            }
            y += 6;
        }
        ctx.disableScissor();
        if (maxScroll > 0) {
            int barH = Math.max(12, (int) ((double) (viewBottom - viewTop) * (viewBottom - viewTop) / (contentHeight(entries))));
            int barY = viewTop + (int) ((double) scroll / maxScroll * (viewBottom - viewTop - barH));
            ctx.fill(panelX + panelW - 5, barY, panelX + panelW - 2, barY + barH, 0xFF888888);
        }
    }
}
