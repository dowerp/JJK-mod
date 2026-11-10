package com.dowerp.sukuna.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.math.MathHelper;

public class ExtraArmsModel extends Model<ArmsRenderState> {
    private final ModelPart rightUpper;
    private final ModelPart rightFore;
    private final ModelPart leftUpper;
    private final ModelPart leftFore;

    public ExtraArmsModel(ModelPart root) {
        super(root, RenderLayers::entityCutoutNoCull);
        this.rightUpper = root.getChild("extra_right");
        this.rightFore = this.rightUpper.getChild("fore");
        this.leftUpper = root.getChild("extra_left");
        this.leftFore = this.leftUpper.getChild("fore");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        ModelPartData root = data.getRoot();
        ModelPartData r = root.addChild("extra_right", ModelPartBuilder.create().uv(40, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
            ModelTransform.origin(-5.0F, 5.0F, 0.0F));
        r.addChild("fore", ModelPartBuilder.create().uv(40, 22).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
            ModelTransform.origin(0.0F, 6.0F, 0.0F));
        ModelPartData l = root.addChild("extra_left", ModelPartBuilder.create().uv(32, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
            ModelTransform.origin(5.0F, 5.0F, 0.0F));
        l.addChild("fore", ModelPartBuilder.create().uv(32, 54).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F),
            ModelTransform.origin(0.0F, 6.0F, 0.0F));
        return TexturedModelData.of(data, 64, 64);
    }

    @Override
    public void setAngles(ArmsRenderState state) {
        super.setAngles(state);
        float idle = MathHelper.sin(state.age * 0.09F) * 0.04F;
        float swing = MathHelper.cos(state.limbSwing * 0.6662F) * 0.35F * state.limbAmp;
        float thrust = state.cast >= 0.0F ? MathHelper.sin(state.cast * (float) Math.PI) : 0.0F;
        float thrustLate = state.cast >= 0.0F ? MathHelper.sin(MathHelper.clamp(state.cast * 1.3F - 0.3F, 0.0F, 1.0F) * (float) Math.PI) : 0.0F;

        rightUpper.pitch = -0.25F + idle + swing - thrust * 1.2F;
        leftUpper.pitch = -0.25F - idle - swing - thrustLate * 1.2F;
        rightUpper.roll = 0.28F - thrust * 0.3F;
        leftUpper.roll = -0.28F + thrustLate * 0.3F;
        rightUpper.yaw = 0.0F;
        leftUpper.yaw = 0.0F;
        rightFore.pitch = -1.05F - thrust * 0.5F;
        leftFore.pitch = -1.05F - thrustLate * 0.5F;
        rightFore.yaw = -0.25F;
        leftFore.yaw = 0.25F;
    }
}
