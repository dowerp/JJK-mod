package com.dowerp.sukuna;

import net.minecraft.text.Text;

public enum SukunaCharacter {
    ITADORI("itadori", true),
    GOJO("gojo", true),
    MEGUMI("megumi", true),
    CHOSO("choso", true),
    MAKI("maki", true),
    TODO("todo", true),
    YUTA("yuta", true);

    public final String id;
    public final boolean playable;

    SukunaCharacter(String id, boolean playable) {
        this.id = id;
        this.playable = playable;
    }

    public SukunaForm defaultForm() {
        return switch (this) {
            case GOJO -> SukunaForm.GOJO;
            case CHOSO -> SukunaForm.CHOSO;
            case TODO -> SukunaForm.TODO;
            case MAKI -> SukunaForm.MAKI;
            case MEGUMI -> SukunaForm.MEGUMI;
            case YUTA -> SukunaForm.YUTA;
            default -> SukunaForm.YUJI;
        };
    }

    public Text displayName() {
        return Text.translatable("sukuna.char." + id);
    }

    public static SukunaCharacter byId(String id) {
        for (SukunaCharacter c : values()) {
            if (c.id.equals(id)) return c;
        }
        return ITADORI;
    }
}
