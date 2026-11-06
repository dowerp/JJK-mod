package com.dowerp.sukuna.client;

import com.dowerp.sukuna.item.ModItems;
import com.dowerp.sukuna.net.WeaponPayload;
import com.dowerp.sukuna.tasks.MakiTasks;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Колесо оружия Маки: открывается при зажатии U, выбор применяется при отпускании клавиши (или кликом). */
public class WeaponWheelScreen extends Screen {
    private static final int RADIUS = 62;
    private static final int BOX = 34;
    private static final int[] WEAPONS = {MakiTasks.WEAPON_KATANA, MakiTasks.WEAPON_CLOUD, MakiTasks.WEAPON_FISTS};
    private static final double[] ANGLES = {-90.0, 30.0, 150.0};
    private static final String[] NAMES = {"sukuna.weapon.katana", "sukuna.weapon.cloud", "sukuna.weapon.fists"};

    private final KeyBinding key;
    private int hovered = -1;
    private int age = 0;

    public WeaponWheelScreen(KeyBinding key) {
        super(Text.translatable("sukuna.weapon.title"));
        this.key = key;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private boolean keyHeld() {
        if (this.client == null) return false;
        InputUtil.Key bound = InputUtil.fromTranslationKey(key.getBoundKeyTranslationKey());
        if (bound.getCategory() != InputUtil.Type.KEYSYM) return key.isPressed();
        return GLFW.glfwGetKey(this.client.getWindow().getHandle(), bound.getCode()) == GLFW.GLFW_PRESS;
    }

    private int pick(double mx, double my) {
        double dx = mx - this.width / 2.0;
        double dy = my - this.height / 2.0;
        if (Math.sqrt(dx * dx + dy * dy) < 14.0) return -1;
        double ang = Math.toDegrees(Math.atan2(dy, dx));
        int best = -1;
        double bestDiff = 1000.0;
        for (int i = 0; i < ANGLES.length; i++) {
            double diff = Math.abs(((ang - ANGLES[i]) % 360.0 + 540.0) % 360.0 - 180.0);
            if (diff < bestDiff) {
                bestDiff = diff;
                best = i;
            }
        }
        return best;
    }

    private void choose(int index) {
        if (index >= 0) {
            ClientPlayNetworking.send(new WeaponPayload(WEAPONS[index]));
        }
        this.close();
    }

    @Override
    public void tick() {
        age++;
        if (age > 2 && !keyHeld()) {
            choose(hovered);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
        choose(pick(click.x(), click.y()));
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        hovered = pick(mouseX, mouseY);
        int cx = this.width / 2;
        int cy = this.height / 2;
        ctx.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFFAAAAAA);
        ItemStack[] icons = {new ItemStack(ModItems.SOUL_SPLIT_KATANA), new ItemStack(ModItems.PLAYFUL_CLOUD), ItemStack.EMPTY};
        for (int i = 0; i < WEAPONS.length; i++) {
            double a = Math.toRadians(ANGLES[i]);
            int bx = cx + (int) Math.round(Math.cos(a) * RADIUS);
            int by = cy + (int) Math.round(Math.sin(a) * RADIUS);
            boolean on = i == hovered;
            ctx.fill(bx - BOX / 2 - 2, by - BOX / 2 - 2, bx + BOX / 2 + 2, by + BOX / 2 + 2, on ? 0xFFFFD54F : 0xFF303030);
            ctx.fill(bx - BOX / 2, by - BOX / 2, bx + BOX / 2, by + BOX / 2, on ? 0xCC4A4A20 : 0xCC101010);
            if (!icons[i].isEmpty()) {
                ctx.drawItem(icons[i], bx - 8, by - 8);
            } else {
                ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("\u270A"), bx, by - 4, 0xFFFFFFFF);
            }
            ctx.drawCenteredTextWithShadow(this.textRenderer, Text.translatable(NAMES[i]), bx, by + BOX / 2 + 5, on ? 0xFFFFD54F : 0xFFCCCCCC);
        }
        ctx.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, 20, 0xFFFFFFFF);
    }
}
