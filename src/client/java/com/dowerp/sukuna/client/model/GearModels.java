package com.dowerp.sukuna.client.model;

import com.dowerp.sukuna.SukunaMod;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public final class GearModels {
    public static final EntityModelLayer HOOD_LAYER = new EntityModelLayer(SukunaMod.id("hood"), "main");
    public static final EntityModelLayer BLINDFOLD_LAYER = new EntityModelLayer(SukunaMod.id("blindfold"), "main");
    public static final EntityModelLayer CLOAK_LAYER = new EntityModelLayer(SukunaMod.id("choso_cloak"), "main");
    public static final EntityModelLayer KAMUTOKE_LAYER = new EntityModelLayer(SukunaMod.id("kamutoke"), "main");

    public static final EntityModelLayer WINGS_LAYER = new EntityModelLayer(SukunaMod.id("blood_wings"), "main");
    public static final Identifier WINGS_TEXTURE = SukunaMod.id("textures/entity/gear/blood_wings.png");
    public static final EntityModelLayer SKULL_LAYER = new EntityModelLayer(SukunaMod.id("skull"), "main");
    public static final EntityModelLayer HAIR_LAYER = new EntityModelLayer(SukunaMod.id("sukuna_hair"), "main");
    public static final Identifier HAIR_TEXTURE = SukunaMod.id("textures/entity/gear/sukuna_hair.png");
    public static final Identifier SKIN_TEXTURE = SukunaMod.id("textures/entity/sukuna_skin.png");
    public static final Identifier HOOD_TEXTURE = SukunaMod.id("textures/entity/gear/hood.png");
    public static final Identifier BLINDFOLD_TEXTURE = SukunaMod.id("textures/entity/gear/blindfold.png");
    public static final Identifier CLOAK_TEXTURE = SukunaMod.id("textures/entity/gear/choso_cloak.png");
    public static final Identifier KAMUTOKE_TEXTURE = SukunaMod.id("textures/entity/gear/kamutoke.png");

    private GearModels() {
    }

    public static class Static extends Model<ArmsRenderState> {
        public Static(ModelPart root) {
            super(root, RenderLayers::entityCutoutNoCull);
        }
    }

    public static TexturedModelData hood() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartBuilder b = ModelPartBuilder.create()
            .uv(0, 0).cuboid(-5.0F, -2.2F, -3.0F, 10.0F, 2.6F, 6.0F)
            .uv(0, 12).cuboid(-4.2F, -0.6F, 1.7F, 8.4F, 6.5F, 2.6F)
            .uv(0, 24).cuboid(-2.6F, 5.9F, 2.2F, 5.2F, 2.4F, 1.8F)
            .uv(0, 32).cuboid(-2.4F, 0.4F, -3.4F, 0.8F, 4.0F, 0.8F)
            .uv(8, 32).cuboid(1.6F, 0.4F, -3.4F, 0.8F, 4.0F, 0.8F);
        root.addChild("hood", b, ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    public static TexturedModelData wings() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        float[] lens = {13.0F, 16.0F, 19.0F, 16.0F, 12.0F};
        float[] rolls = {0.15F, 0.45F, 0.75F, 1.05F, 1.35F};
        for (int side = 0; side < 2; side++) {
            float sg = side == 0 ? -1.0F : 1.0F;
            String n = side == 0 ? "r" : "l";
            ModelPartData wing = root.addChild("wing_" + n, ModelPartBuilder.create(), ModelTransform.of(sg * 2.2F, 2.0F, 2.6F, 0.0F, -sg * 0.45F, 0.0F));
            ModelPartData arm = wing.addChild("arm_" + n, ModelPartBuilder.create().uv(0, 0).cuboid(-0.8F, -14.0F, -0.8F, 1.6F, 14.0F, 1.6F), ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, sg * 0.75F));
            ModelPartData fore = arm.addChild("fore_" + n, ModelPartBuilder.create().uv(16, 0).cuboid(-0.7F, -12.0F, -0.7F, 1.4F, 12.0F, 1.4F), ModelTransform.of(0.0F, -13.0F, 0.0F, 0.0F, 0.0F, sg * 0.6F));
            for (int i = 0; i < 5; i++) {
                float oy = -2.0F - i * 2.2F;
                fore.addChild("feather_" + n + i, ModelPartBuilder.create().uv(i * 8, 20).cuboid(-0.5F, 0.0F, -0.3F, 1.0F, lens[i], 0.6F), ModelTransform.of(0.0F, oy, 0.0F, 0.0F, 0.0F, sg * (float) (Math.PI - rolls[i] * 0.9F)));
            }
        }
        return TexturedModelData.of(data, 64, 64);
    }

    public static class Wings extends Model<ArmsRenderState> {
        private final ModelPart wingR;
        private final ModelPart wingL;

        public Wings(ModelPart root) {
            super(root, RenderLayers::entityCutoutNoCull);
            this.wingR = root.getChild("wing_r");
            this.wingL = root.getChild("wing_l");
        }

        @Override
        public void setAngles(ArmsRenderState state) {
            super.setAngles(state);
            float flap = MathHelper.sin(state.age * 0.28F) * 0.22F + state.limbAmp * 0.25F;
            wingR.yaw = 0.45F + flap;
            wingL.yaw = -0.45F - flap;
            wingR.roll = flap * 0.4F;
            wingL.roll = -flap * 0.4F;
        }
    }

    public static TexturedModelData skull() {
        ModelData data = new ModelData();
        data.getRoot().addChild("head", ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }

    public static TexturedModelData hair() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("mass", ModelPartBuilder.create().uv(0, 0).cuboid(-4.4F, -9.5F, -4.4F, 8.8F, 2.0F, 8.8F), ModelTransform.NONE);
        root.addChild("spike_a", ModelPartBuilder.create().uv(0, 12).cuboid(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F), ModelTransform.of(-2.2F, -9.2F, -1.6F, -0.2F, 0.0F, -0.45F));
        root.addChild("spike_b", ModelPartBuilder.create().uv(10, 12).cuboid(-1.0F, -3.6F, -1.0F, 2.0F, 3.6F, 2.0F), ModelTransform.of(1.4F, -9.2F, -0.8F, 0.1F, 0.0F, 0.35F));
        root.addChild("spike_c", ModelPartBuilder.create().uv(20, 12).cuboid(-1.0F, -3.4F, -1.0F, 2.0F, 3.4F, 2.0F), ModelTransform.of(-0.2F, -9.2F, 1.8F, 0.45F, 0.0F, -0.1F));
        root.addChild("side_l", ModelPartBuilder.create().uv(0, 20).cuboid(-0.8F, -1.2F, -1.3F, 1.6F, 2.4F, 2.6F), ModelTransform.of(4.9F, -6.6F, 0.4F, 0.0F, 0.0F, 0.35F));
        root.addChild("side_r", ModelPartBuilder.create().uv(12, 20).cuboid(-0.8F, -1.2F, -1.3F, 1.6F, 2.4F, 2.6F), ModelTransform.of(-4.9F, -6.6F, 0.4F, 0.0F, 0.0F, -0.35F));
        root.addChild("back", ModelPartBuilder.create().uv(0, 30).cuboid(-3.4F, 0.0F, 0.0F, 6.8F, 6.0F, 1.6F), ModelTransform.of(0.0F, -8.0F, 4.0F, 0.18F, 0.0F, 0.0F));
        root.addChild("fringe_a", ModelPartBuilder.create().uv(24, 30).cuboid(-0.7F, 0.0F, -0.4F, 1.4F, 2.6F, 0.8F), ModelTransform.of(-2.6F, -8.6F, -4.5F, 0.0F, 0.0F, 0.2F));
        root.addChild("fringe_b", ModelPartBuilder.create().uv(30, 30).cuboid(-0.7F, 0.0F, -0.4F, 1.4F, 2.6F, 0.8F), ModelTransform.of(0.2F, -8.6F, -4.5F, 0.0F, 0.0F, -0.1F));
        root.addChild("fringe_c", ModelPartBuilder.create().uv(36, 30).cuboid(-0.7F, 0.0F, -0.4F, 1.4F, 2.2F, 0.8F), ModelTransform.of(2.6F, -8.6F, -4.5F, 0.0F, 0.0F, -0.25F));
        return TexturedModelData.of(data, 64, 64);
    }

    public static TexturedModelData blindfold() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartBuilder b = ModelPartBuilder.create()
            .uv(0, 0).cuboid(-4.6F, -5.6F, -4.6F, 9.2F, 2.2F, 9.2F);
        root.addChild("band", b, ModelTransform.NONE);
        ModelPartBuilder knot = ModelPartBuilder.create()
            .uv(0, 22).cuboid(-1.5F, -5.2F, 4.6F, 3.0F, 1.6F, 0.8F)
            .uv(10, 22).cuboid(-3.4F, -5.0F, 5.0F, 1.2F, 3.6F, 0.6F)
            .uv(16, 22).cuboid(2.2F, -5.0F, 5.0F, 1.2F, 3.6F, 0.6F);
        root.addChild("knot", knot, ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 32);
    }

    public static TexturedModelData cloak() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("mantle", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-6.6F, -0.8F, -3.4F, 13.2F, 3.8F, 6.8F)
            .uv(0, 12).cuboid(-5.2F, -2.8F, -3.2F, 10.4F, 2.4F, 6.4F)
            .uv(0, 22).cuboid(-6.8F, 3.0F, -3.5F, 2.0F, 6.0F, 7.0F)
            .uv(18, 22).cuboid(4.8F, 3.0F, -3.5F, 2.0F, 6.0F, 7.0F), ModelTransform.NONE);
        root.addChild("back", ModelPartBuilder.create()
            .uv(0, 36).cuboid(-5.8F, 0.0F, 0.0F, 11.6F, 21.0F, 1.3F), ModelTransform.origin(0.0F, 0.8F, 2.4F));
        root.addChild("front_left", ModelPartBuilder.create()
            .uv(30, 36).cuboid(0.0F, 0.0F, -1.3F, 4.6F, 17.0F, 1.2F), ModelTransform.origin(0.4F, 3.0F, -2.4F));
        root.addChild("front_right", ModelPartBuilder.create()
            .uv(44, 36).cuboid(-4.6F, 0.0F, -1.3F, 4.6F, 17.0F, 1.2F), ModelTransform.origin(-0.4F, 3.0F, -2.4F));
        return TexturedModelData.of(data, 64, 64);
    }

    public static class Cloak extends Model<ArmsRenderState> {
        private final ModelPart back;
        private final ModelPart frontLeft;
        private final ModelPart frontRight;

        public Cloak(ModelPart root) {
            super(root, RenderLayers::entityCutoutNoCull);
            this.back = root.getChild("back");
            this.frontLeft = root.getChild("front_left");
            this.frontRight = root.getChild("front_right");
        }

        @Override
        public void setAngles(ArmsRenderState state) {
            super.setAngles(state);
            float sway = MathHelper.sin(state.age * 0.07F) * 0.03F;
            float run = Math.min(0.9F, state.limbAmp * 0.9F);
            back.pitch = 0.08F + sway + run * 0.55F + (state.sneaking ? 0.25F : 0.0F);
            frontLeft.pitch = -0.02F - run * 0.3F;
            frontRight.pitch = -0.02F - run * 0.3F;
            frontLeft.roll = -0.04F - run * 0.12F;
            frontRight.roll = 0.04F + run * 0.12F;
        }
    }

    public static TexturedModelData kamutoke() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        root.addChild("weapon", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-0.8F, -15.0F, -0.8F, 1.6F, 30.0F, 1.6F)
            .uv(8, 0).cuboid(-1.4F, -17.0F, -1.4F, 2.8F, 2.4F, 2.8F)
            .uv(0, 34).cuboid(-2.4F, 14.0F, -2.4F, 4.8F, 1.6F, 4.8F)
            .uv(20, 0).cuboid(-1.8F, 15.6F, -0.5F, 3.6F, 11.0F, 1.0F)
            .uv(20, 12).cuboid(-4.6F, 15.6F, -0.5F, 1.2F, 7.0F, 1.0F)
            .uv(20, 20).cuboid(3.4F, 15.6F, -0.5F, 1.2F, 7.0F, 1.0F)
            .uv(32, 0).cuboid(-3.6F, 13.0F, -0.4F, 1.0F, 2.6F, 0.8F)
            .uv(32, 6).cuboid(2.6F, 13.0F, -0.4F, 1.0F, 2.6F, 0.8F)
            .uv(0, 44).cuboid(-1.6F, 7.0F, -1.6F, 3.2F, 1.0F, 3.2F)
            .uv(0, 50).cuboid(-1.6F, 2.0F, -1.6F, 3.2F, 1.0F, 3.2F), ModelTransform.NONE);
        return TexturedModelData.of(data, 64, 64);
    }
}
