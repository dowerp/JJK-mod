package com.dowerp.sukuna.client.model;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.RenderLayers;

public class DharmaWheelModel extends Model<WheelRenderState> {
    private final ModelPart wheel;

    public DharmaWheelModel(ModelPart root) {
        super(root, RenderLayers::entityCutout);
        this.wheel = root.getChild("wheel");
    }

    public static ModelPartData addWheel(ModelPartData parent, String name, float k, ModelTransform place,
                                         int[] ring, int[] handle, int[] spoke, int[] hub) {
        ModelPartData wheel = parent.addChild(name, ModelPartBuilder.create()
            .uv(hub[0], hub[1]).cuboid(-2.5F * k, -2.5F * k, -1.5F * k, 5.0F * k, 5.0F * k, 3.0F * k), place);
        for (int i = 0; i < 8; i++) {
            float rot = (float) (i * Math.PI / 4.0);
            ModelTransform r = ModelTransform.rotation(0.0F, 0.0F, rot);
            wheel.addChild("ring_" + i, ModelPartBuilder.create().uv(ring[0], ring[1])
                .cuboid(-4.1F * k, -10.4F * k, -0.9F * k, 8.2F * k, 1.6F * k, 1.8F * k), r);
            wheel.addChild("inner_" + i, ModelPartBuilder.create().uv(ring[0], ring[1])
                .cuboid(-2.5F * k, -6.2F * k, -0.6F * k, 5.0F * k, 1.0F * k, 1.2F * k), r);
            wheel.addChild("spoke_" + i, ModelPartBuilder.create().uv(spoke[0], spoke[1])
                .cuboid(-0.6F * k, -8.8F * k, -0.5F * k, 1.2F * k, 6.4F * k, 1.0F * k), r);
            wheel.addChild("handle_" + i, ModelPartBuilder.create().uv(handle[0], handle[1])
                .cuboid(-0.8F * k, -13.2F * k, -0.8F * k, 1.6F * k, 3.0F * k, 1.6F * k), r);
            wheel.addChild("knob_" + i, ModelPartBuilder.create().uv(hub[0], hub[1])
                .cuboid(-1.3F * k, -14.6F * k, -1.3F * k, 2.6F * k, 2.0F * k, 2.6F * k), r);
        }
        return wheel;
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData data = new ModelData();
        addWheel(data.getRoot(), "wheel", 1.0F, ModelTransform.NONE,
            new int[] {0, 0}, new int[] {0, 16}, new int[] {32, 0}, new int[] {32, 16});
        return TexturedModelData.of(data, 64, 32);
    }

    @Override
    public void setAngles(WheelRenderState state) {
        super.setAngles(state);
        wheel.roll = (float) Math.toRadians(state.angleDeg);
        float scale = 1.0F + state.pulse * 0.12F;
        wheel.xScale = scale;
        wheel.yScale = scale;
    }
}
