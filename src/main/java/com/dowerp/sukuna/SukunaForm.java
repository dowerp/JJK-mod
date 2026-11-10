package com.dowerp.sukuna;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public enum SukunaForm {
    YUJI("sukuna.form.yuji", Formatting.GREEN, 100.0),
    SUKUNA("sukuna.form.sukuna", Formatting.DARK_RED, 150.0),
    HEIAN("sukuna.form.heian", Formatting.GOLD, 200.0),
    GOJO("sukuna.form.gojo", Formatting.AQUA, 120.0),
    GOJO_AWAKENED("sukuna.form.gojo_awakened", Formatting.LIGHT_PURPLE, 160.0),
    CHOSO("sukuna.form.choso", Formatting.DARK_RED, 130.0),
    CHOSO_WING("sukuna.form.choso_wing", Formatting.RED, 170.0),
    TODO("sukuna.form.todo", Formatting.GOLD, 140.0),
    TODO_AWAKENED("sukuna.form.todo_awakened", Formatting.YELLOW, 180.0),
    MAKI("sukuna.form.maki", Formatting.DARK_GREEN, 170.0),
    MEGUMI("sukuna.form.megumi", Formatting.DARK_BLUE, 120.0),
    YUTA("sukuna.form.yuta", Formatting.WHITE, 130.0),
    YUTA_RIKA("sukuna.form.yuta_rika", Formatting.AQUA, 170.0),
    MEGUMI_BEASTS("sukuna.form.megumi_beasts", Formatting.BLUE, 130.0);

    public final String translationKey;
    public final Formatting color;
    public final double targetHp;

    SukunaForm(String translationKey, Formatting color, double targetHp) {
        this.translationKey = translationKey;
        this.color = color;
        this.targetHp = targetHp;
    }

    public boolean isGojo() {
        return this == GOJO || this == GOJO_AWAKENED;
    }

    public boolean isChoso() {
        return this == CHOSO || this == CHOSO_WING;
    }

    public boolean isTodo() {
        return this == TODO || this == TODO_AWAKENED;
    }

    public boolean isMegumi() {
        return this == MEGUMI || this == MEGUMI_BEASTS;
    }

    public boolean isYuta() {
        return this == YUTA || this == YUTA_RIKA;
    }

    public boolean isMaki() {
        return this == MAKI;
    }

    public boolean awakenable() {
        return this == GOJO || this == CHOSO || this == TODO || this == YUTA || this == MEGUMI;
    }

    public boolean awakened() {
        return this == GOJO_AWAKENED || this == CHOSO_WING || this == TODO_AWAKENED || this == YUTA_RIKA || this == MEGUMI_BEASTS;
    }

    public Text displayName() {
        return Text.translatable(translationKey).formatted(color, Formatting.BOLD);
    }
}
