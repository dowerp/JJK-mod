package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaCharacter;
import com.dowerp.sukuna.net.CharacterPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CharacterSelectScreen extends Screen {
    private static final int BUTTON_W = 230;
    private static final int BUTTON_H = 20;
    private static final int GAP = 6;

    public CharacterSelectScreen() {
        super(Text.translatable("sukuna.char.title"));
    }

    @Override
    protected void init() {
        SukunaCharacter[] all = SukunaCharacter.values();
        int total = all.length * (BUTTON_H + GAP) - GAP;
        int top = this.height / 2 - total / 2;
        int x = this.width / 2 - BUTTON_W / 2;

        for (int i = 0; i < all.length; i++) {
            SukunaCharacter c = all[i];
            ButtonWidget button = ButtonWidget.builder(label(c, i), b -> {
                ClientPlayNetworking.send(new CharacterPayload(c.ordinal()));
                this.close();
            }).dimensions(x, top + i * (BUTTON_H + GAP), BUTTON_W, BUTTON_H).build();
            button.active = c.playable;
            this.addDrawableChild(button);
        }

        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
            .dimensions(x, top + total + 14, BUTTON_W, BUTTON_H).build());
    }

    private Text label(SukunaCharacter c, int index) {
        Text name = c.displayName();
        if (!c.playable) {
            return name.copy().append(Text.literal(" - ")).append(Text.translatable("sukuna.char.soon").formatted(Formatting.GRAY));
        }
        if (index == ClientState.character) {
            return Text.literal("> ").append(name).append(Text.literal(" <"));
        }
        return name;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable("sukuna.char.hint"), this.width / 2, 34, 0xFFAAAAAA);
    }
}
