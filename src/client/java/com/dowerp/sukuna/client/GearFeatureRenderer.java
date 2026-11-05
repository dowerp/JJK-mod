package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaCharacter;
import com.dowerp.sukuna.SukunaForm;
import com.dowerp.sukuna.client.model.ArmsRenderState;
import com.dowerp.sukuna.client.model.GearModels;
import com.dowerp.sukuna.net.StatePayload;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;

public class GearFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
    private static final int FULL_LIGHT = 15728880;

    private final GearModels.Static hood;
    private final GearModels.Static blindfold;
    private final GearModels.Cloak cloak;
    private final GearModels.Static kamutoke;
    private final GearModels.Static hair;
    private final GearModels.Static skull;
    private final GearModels.Wings wings;

    public GearFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context,
                               ModelPart hoodPart, ModelPart blindfoldPart, ModelPart cloakPart, ModelPart kamutokePart, ModelPart hairPart, ModelPart skullPart, ModelPart wingsPart) {
        super(context);
        this.hood = new GearModels.Static(hoodPart);
        this.blindfold = new GearModels.Static(blindfoldPart);
        this.cloak = new GearModels.Cloak(cloakPart);
        this.kamutoke = new GearModels.Static(kamutokePart);
        this.hair = new GearModels.Static(hairPart);
        this.skull = new GearModels.Static(skullPart);
        this.wings = new GearModels.Wings(wingsPart);
    }

    @Override
    public void render(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        RemoteFx.Entry fx = RemoteFx.get(state.id);
        if (fx == null || state.invisible) return;

        ArmsRenderState as = new ArmsRenderState();
        as.age = state.age;
        as.limbSwing = state.limbSwingAnimationProgress;
        as.limbAmp = state.limbSwingAmplitude;
        as.sneaking = state.sneaking;

        PlayerEntityModel body = getContextModel();

        if (fx.character == SukunaCharacter.ITADORI && (fx.form == SukunaForm.SUKUNA || fx.form == SukunaForm.HEIAN)) {
            matrices.push();
            queue.submitModel(body, state, matrices, RenderLayers.entityDecal(GearModels.SKIN_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
            matrices.push();
            body.head.applyTransform(matrices);
            queue.submitModel(hair, as, matrices, hair.getLayer(GearModels.HAIR_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }

        if (fx.character == SukunaCharacter.ITADORI && fx.form == SukunaForm.YUJI) {
            matrices.push();
            body.body.applyTransform(matrices);
            queue.submitModel(hood, as, matrices, hood.getLayer(GearModels.HOOD_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }

        if (fx.character == SukunaCharacter.GOJO && fx.form == SukunaForm.GOJO) {
            matrices.push();
            body.head.applyTransform(matrices);
            queue.submitModel(blindfold, as, matrices, blindfold.getLayer(GearModels.BLINDFOLD_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }

        if (fx.character == SukunaCharacter.CHOSO && fx.form == SukunaForm.CHOSO_WING) {
            matrices.push();
            body.body.applyTransform(matrices);
            queue.submitModel(wings, as, matrices, wings.getLayer(GearModels.WINGS_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }

        if (fx.character == SukunaCharacter.CHOSO) {
            matrices.push();
            body.body.applyTransform(matrices);
            queue.submitModel(cloak, as, matrices, cloak.getLayer(GearModels.CLOAK_TEXTURE), light, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }

        if (fx.head != null && !fx.head.isEmpty()) {
            try {
                java.util.UUID victim = java.util.UUID.fromString(fx.head);
                net.minecraft.client.network.PlayerListEntry entry = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler() == null
                    ? null : net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler().getPlayerListEntry(victim);
                if (entry != null) {
                    matrices.push();
                    body.rightArm.applyTransform(matrices);
                    matrices.translate(-1.0F / 16.0F, 11.0F / 16.0F, 0.0F);
                    matrices.scale(0.85F, 0.85F, 0.85F);
                    queue.submitModel(skull, as, matrices, RenderLayers.entityCutoutNoCull(entry.getSkinTextures().body().texturePath()), light, OverlayTexture.DEFAULT_UV, 0, null);
                    matrices.pop();
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (RemoteFx.has(fx, StatePayload.KAMUTOKE)) {
            matrices.push();
            body.rightArm.applyTransform(matrices);
            matrices.translate(-1.0F / 16.0F, 10.5F / 16.0F, 0.0F);
            matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(-90.0F));
            queue.submitModel(kamutoke, as, matrices, kamutoke.getLayer(GearModels.KAMUTOKE_TEXTURE), FULL_LIGHT, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }
    }
}
