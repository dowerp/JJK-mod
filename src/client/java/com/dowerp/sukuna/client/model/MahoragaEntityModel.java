package com.dowerp.sukuna.client.model;

import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.util.math.MathHelper;

public class MahoragaEntityModel extends BipedEntityModel<MahoragaRenderState> {
    private final ModelPart wheel;

    public MahoragaEntityModel(ModelPart root) {
        super(root);
        this.wheel = root.getChild("body").getChild("wheel");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = BipedEntityModel.getModelData(Dilation.NONE, 0.0F);
        ModelPartData root = data.getRoot();

        root.addChild("hat", ModelPartBuilder.create(), ModelTransform.NONE);

        ModelPartData head = root.addChild("head", ModelPartBuilder.create()
            .uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F)
            .uv(44, 0).cuboid(-4.0F, -3.0F, -6.0F, 8.0F, 3.0F, 2.0F), ModelTransform.NONE);
        head.addChild("horn_right", ModelPartBuilder.create().uv(32, 0).cuboid(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.of(-3.5F, -7.0F, 0.0F, 0.0F, 0.0F, 0.5F));
        head.addChild("horn_left", ModelPartBuilder.create().uv(32, 0).cuboid(-1.0F, -5.0F, -1.0F, 2.0F, 5.0F, 2.0F),
            ModelTransform.of(3.5F, -7.0F, 0.0F, 0.0F, 0.0F, -0.5F));

        ModelPartData body = root.addChild("body", ModelPartBuilder.create()
            .uv(0, 16).cuboid(-5.0F, 0.0F, -3.0F, 10.0F, 14.0F, 6.0F)
            .uv(0, 64).cuboid(-6.0F, 1.0F, -5.0F, 12.0F, 8.0F, 2.0F)
            .uv(40, 64).cuboid(-6.0F, 12.0F, -3.5F, 12.0F, 6.0F, 7.0F), ModelTransform.NONE);

        DharmaWheelModel.addWheel(body, "wheel", 0.75F, ModelTransform.origin(0.0F, -17.0F, 4.0F),
            new int[] {96, 0}, new int[] {96, 16}, new int[] {96, 16}, new int[] {96, 16});

        root.addChild("right_arm", ModelPartBuilder.create()
            .uv(32, 16).cuboid(-4.0F, -2.0F, -2.5F, 5.0F, 14.0F, 5.0F)
            .uv(52, 16).cuboid(-4.5F, -3.5F, -3.5F, 7.0F, 3.0F, 7.0F)
            .uv(44, 40).cuboid(-4.5F, 8.0F, -3.0F, 6.0F, 3.0F, 6.0F), ModelTransform.origin(-6.0F, 2.0F, 0.0F));
        root.addChild("left_arm", ModelPartBuilder.create().mirrored()
            .uv(32, 16).cuboid(-1.0F, -2.0F, -2.5F, 5.0F, 14.0F, 5.0F)
            .uv(52, 16).cuboid(-2.5F, -3.5F, -3.5F, 7.0F, 3.0F, 7.0F)
            .uv(44, 40).cuboid(-1.5F, 8.0F, -3.0F, 6.0F, 3.0F, 6.0F), ModelTransform.origin(6.0F, 2.0F, 0.0F));

        root.addChild("right_leg", ModelPartBuilder.create()
            .uv(0, 40).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F)
            .uv(20, 40).cuboid(-3.0F, 3.0F, -3.0F, 6.0F, 4.0F, 6.0F), ModelTransform.origin(-3.0F, 12.0F, 0.0F));
        root.addChild("left_leg", ModelPartBuilder.create().mirrored()
            .uv(0, 40).cuboid(-2.5F, 0.0F, -2.5F, 5.0F, 12.0F, 5.0F)
            .uv(20, 40).cuboid(-3.0F, 3.0F, -3.0F, 6.0F, 4.0F, 6.0F), ModelTransform.origin(3.0F, 12.0F, 0.0F));

        return TexturedModelData.of(data, 128, 128);
    }

    @Override
    public void setAngles(MahoragaRenderState state) {
        super.setAngles(state);
        float t = state.age;

        this.body.pitch += MathHelper.sin(t * 0.08F) * 0.02F;
        float sway = 0.06F + MathHelper.cos(t * 0.09F) * 0.03F;
        this.rightArm.roll += sway;
        this.leftArm.roll -= sway;

        this.body.pitch += state.handSwingProgress * 0.45F;
        this.head.pitch += state.handSwingProgress * 0.2F;

        this.wheel.roll = (float) Math.toRadians(state.adaptSteps * 45.0F) + t * 0.035F;
        float pulse = 1.0F + 0.04F * MathHelper.sin(t * 0.2F);
        this.wheel.xScale = pulse;
        this.wheel.yScale = pulse;
    }
}
