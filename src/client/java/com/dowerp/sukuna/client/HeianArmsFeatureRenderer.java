package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.client.model.ArmsRenderState;
import com.dowerp.sukuna.client.model.ExtraArmsModel;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class HeianArmsFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private final PlayerEntityRenderer renderer;
    private final ExtraArmsModel model;

    public HeianArmsFeatureRenderer(PlayerEntityRenderer renderer, ExtraArmsModel model) {
        super(renderer);
        this.renderer = renderer;
        this.model = model;
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        RemoteFx.Entry fx = RemoteFx.get(state.id);
        if (fx == null || fx.form != SukunaForm.HEIAN || state.invisible) return;

        ArmsRenderState as = new ArmsRenderState();
        as.age = state.age;
        as.limbSwing = state.limbSwingAnimationProgress;
        as.limbAmp = state.limbSwingAmplitude;
        as.cast = RemoteFx.castProgress(fx);

        matrices.push();
        queue.submitModel(model, as, matrices, model.getLayer(com.dowerp.sukuna.client.model.GearModels.SKIN_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
        matrices.pop();
    }
}
