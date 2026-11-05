package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.entity.SpriteFxEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

/** Рисует 2D-эффекты техник: плоские нарисованные текстуры, повёрнутые по направлению удара. */
public class SpriteFxRenderer extends EntityRenderer<SpriteFxEntity, SpriteFxRenderer.SpriteState> {
    private static final int FULL_LIGHT = 15728880;
    private static final String[] KINDS = {"streak", "crescent", "bolt", "ring", "burst", "distort", "blob", "splash", "torrent", "arrow"};
    private static final Identifier[][] TEXTURES = new Identifier[KINDS.length][5];

    static {
        for (int k = 0; k < KINDS.length; k++) {
            for (int p = 0; p < 5; p++) {
                TEXTURES[k][p] = SukunaMod.id("textures/entity/fx/" + KINDS[k] + "_" + p + ".png");
            }
        }
    }

    private final QuadModel model;

    public SpriteFxRenderer(EntityRendererFactory.Context context) {
        super(context);
        ModelData data = new ModelData();
        data.getRoot().addChild("quad", ModelPartBuilder.create().uv(0, 0).cuboid(-8.0F, -8.0F, 0.0F, 16.0F, 16.0F, 0.01F), ModelTransform.NONE);
        this.model = new QuadModel(TexturedModelData.of(data, 32, 16).createModel());
    }

    public static class SpriteState extends EntityRenderState {
        public int kind;
        public int palette;
        public float length;
        public float width;
        public float roll;
        public float yaw;
        public float pitch;
        public float lifeFrac;
    }

    private static final class QuadModel extends Model<SpriteState> {
        QuadModel(ModelPart root) {
            super(root, RenderLayers::entityCutoutNoCull);
        }
    }

    @Override
    public SpriteState createRenderState() {
        return new SpriteState();
    }

    @Override
    public void updateRenderState(SpriteFxEntity e, SpriteState s, float tickDelta) {
        super.updateRenderState(e, s, tickDelta);
        s.kind = MathHelper.clamp(e.kind(), 0, KINDS.length - 1);
        s.palette = s.kind == 5 ? 0 : s.kind == 9 ? 4 : s.kind >= 6 ? 1 : MathHelper.clamp(e.palette(), 0, 4);
        s.length = e.length();
        s.width = e.width();
        s.roll = e.roll();
        s.yaw = e.getYaw();
        s.pitch = e.getPitch();
        s.lifeFrac = Math.min(1.0F, (e.age + tickDelta) / e.life());
    }

    @Override
    public boolean shouldRender(SpriteFxEntity entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public void render(SpriteState s, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        float fade = Math.min(1.0F, (1.0F - s.lifeFrac) / 0.35F);
        float pop = 1.0F + 0.12F * s.lifeFrac;
        float len = s.length * pop * Math.max(0.05F, fade);
        float wid = s.width * pop * Math.max(0.05F, fade);
        Identifier tex = TEXTURES[s.kind][s.palette];

        matrices.push();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-s.yaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(s.pitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(s.roll));

        boolean along = s.kind == 0 || s.kind == 2 || s.kind == 8 || s.kind == 9;
        int planes = (s.kind == 3 || s.kind == 7) ? 1 : 2;
        for (int i = 0; i < planes; i++) {
            matrices.push();
            if (along) {
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i * 90.0F));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            } else if (i == 1) {
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90.0F));
            }
            matrices.scale(len, wid, 1.0F);
            queue.submitModel(model, s, matrices, RenderLayers.entityTranslucentEmissive(tex), FULL_LIGHT, OverlayTexture.DEFAULT_UV, 0, null);
            matrices.pop();
        }
        matrices.pop();
    }
}
