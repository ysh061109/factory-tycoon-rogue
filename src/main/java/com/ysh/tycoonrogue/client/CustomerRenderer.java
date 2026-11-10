package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.CrossedArmsItemLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class CustomerRenderer extends MobRenderer<CustomerEntity, CustomerModel> {
    private static final ResourceLocation BASE_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/entity/villager/villager.png");

    public CustomerRenderer(EntityRendererProvider.Context context) {
        super(context, new CustomerModel(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
        this.addLayer(new CustomerOutfitLayer(this));
        this.addLayer(new CrossedArmsItemLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomerEntity entity) {
        return BASE_TEXTURE;
    }

    @Override
    protected void setupRotations(CustomerEntity entity, PoseStack poseStack, float bob, float yBodyRot,
                                  float partialTick, float scale) {
        super.setupRotations(entity, poseStack, bob, yBodyRot, partialTick, scale);
        float time = entity.tickCount + partialTick;
        switch (entity.getState()) {
            // Leans forward like a sprinter when charging.
            case CustomerEntity.STATE_CHARGE -> poseStack.mulPose(Axis.XP.rotationDegrees(18.0F));
            // Hunched over and bobbing while ransacking.
            case CustomerEntity.STATE_RANSACK -> poseStack.mulPose(
                    Axis.XP.rotationDegrees(12.0F + Mth.sin(time * 1.1F) * 6.0F));
            // Sways side to side while running away.
            case CustomerEntity.STATE_FLEE -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(10.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 1.5F) * 7.0F));
            }
            default -> {
            }
        }
    }
}
