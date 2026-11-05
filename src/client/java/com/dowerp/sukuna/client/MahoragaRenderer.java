package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.entity.MahoragaEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class MahoragaRenderer extends GeoEntityRenderer<MahoragaEntity, EntityRenderState> {
    public static final EntityModelLayer WHEEL_LAYER = new EntityModelLayer(SukunaMod.id("wheel"), "main");
    public static final EntityModelLayer ARMS_LAYER = new EntityModelLayer(SukunaMod.id("heian_arms"), "main");

    public MahoragaRenderer(EntityRendererFactory.Context context) {
        super(context, new DefaultedEntityGeoModel<>(SukunaMod.id("mahoraga")));
        this.scaleWidth = 1.35F;
        this.scaleHeight = 1.35F;
    }
}
