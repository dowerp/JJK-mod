package com.dowerp.sukuna.client;

import com.dowerp.sukuna.SukunaMod;
import com.dowerp.sukuna.entity.TechniqueOrbEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class OrbRenderer extends GeoEntityRenderer<TechniqueOrbEntity, EntityRenderState> {
    public OrbRenderer(EntityRendererFactory.Context context, String name) {
        super(context, new DefaultedEntityGeoModel<>(SukunaMod.id(name)));
    }

    @Override
    protected int getBlockLight(TechniqueOrbEntity entity, BlockPos pos) {
        return 15;
    }

    @Override
    protected int getSkyLight(TechniqueOrbEntity entity, BlockPos pos) {
        return 15;
    }
}
