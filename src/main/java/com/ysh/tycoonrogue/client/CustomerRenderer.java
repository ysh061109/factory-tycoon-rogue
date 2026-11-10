package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

public class CustomerRenderer extends MobRenderer<CustomerEntity, VillagerModel<CustomerEntity>> {
    private static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public CustomerRenderer(EntityRendererProvider.Context context) {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomerOutfitLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomerEntity entity) {
        return BASE_TEXTURE;
    }
}
