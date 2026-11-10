package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.client.model.DharmaWheelModel;
import com.dowerp.sukuna.client.model.WheelRenderState;
import com.dowerp.sukuna.net.StatePayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class WheelFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final Identifier WHEEL_TEXTURE = SukunaMod.id("textures/entity/wheel.png");
    private final DharmaWheelModel wheelModel;

    public WheelFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context, DharmaWheelModel wheelModel) {
        super(context);
        this.wheelModel = wheelModel;
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        RemoteFx.Entry fx = RemoteFx.get(state.id);
        if (!RemoteFx.has(fx, StatePayload.WHEEL)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        boolean self = client.player != null && state.id == client.player.getId();

        WheelRenderState ws = new WheelRenderState();
        ws.age = state.age;
        ws.angleDeg = (self ? ClientState.wheelAngle : state.age * 1.5F) + (float) Math.sin(state.age * 0.05F) * 4.0F;
        ws.pulse = self ? (float) Math.max(0.0, 1.0 - (ClientState.clientTick - ClientState.wheelClickTick) / 8.0) : 0.0F;

        matrices.push();
        matrices.translate(0.0, -1.15, 0.3);
        matrices.scale(0.8F, 0.8F, 0.8F);
        queue.submitModel(wheelModel, ws, matrices, wheelModel.getLayer(WHEEL_TEXTURE),
            light, OverlayTexture.DEFAULT_UV, 0, null);
        matrices.pop();
    }
}
