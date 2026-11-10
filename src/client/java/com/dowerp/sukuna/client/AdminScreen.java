package com.dowerp.sukuna.client;

import com.dowerp.sukuna.net.AdminActionPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Окно админ-меню, открывается на ~. Все действия выполняет сервер после проверки прав оператора. */
public class AdminScreen extends Screen {
    private static final int BUTTON_W = 220;
    private static final int BUTTON_H = 20;
    private static final int GAP = 6;

    public AdminScreen() {
        super(Text.translatable("sukuna.admin.title"));
    }

    @Override
    protected void init() {
        int[] actions = {AdminActionPayload.HEAL_ALL, AdminActionPayload.CLEAR_COMBAT, AdminActionPayload.CD_SELF,
            AdminActionPayload.CD_ALL, AdminActionPayload.CE_SELF, AdminActionPayload.NOCD_TOGGLE};
        String[] keys = {"sukuna.admin.heal_all", "sukuna.admin.clear_combat", "sukuna.admin.cd_self",
            "sukuna.admin.cd_all", "sukuna.admin.ce", "sukuna.admin.nocd"};
        int total = actions.length * (BUTTON_H + GAP) - GAP;
        int top = this.height / 2 - total / 2;
        int x = this.width / 2 - BUTTON_W / 2;
        for (int i = 0; i < actions.length; i++) {
            final int action = actions[i];
            this.addDrawableChild(ButtonWidget.builder(Text.translatable(keys[i]), b -> ClientPlayNetworking.send(new AdminActionPayload(action)))
                .dimensions(x, top + i * (BUTTON_H + GAP), BUTTON_W, BUTTON_H).build());
        }
        this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
            .dimensions(x, top + total + 14, BUTTON_W, BUTTON_H).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 20, 0xFFFFD54F);
    }
}
