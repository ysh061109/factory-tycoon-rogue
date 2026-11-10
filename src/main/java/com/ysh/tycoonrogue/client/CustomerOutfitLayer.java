package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws the biome skin, then this mod's own job outfit on top of the base villager texture. */
public class CustomerOutfitLayer extends RenderLayer<CustomerEntity, VillagerModel<CustomerEntity>> {
    private static final ResourceLocation[] BIOME_TEXTURES = new ResourceLocation[CustomerEntity.BIOMES.length];

    static {
        for (int i = 0; i < BIOME_TEXTURES.length; i++) {
            BIOME_TEXTURES[i] = ResourceLocation.withDefaultNamespace(
                    "textures/entity/villager/type/" + CustomerEntity.BIOMES[i] + ".png");
        }
    }

    public CustomerOutfitLayer(RenderLayerParent<CustomerEntity, VillagerModel<CustomerEntity>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CustomerEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) {
            return;
        }
        int biome = Math.floorMod(entity.getBiome(), BIOME_TEXTURES.length);
        renderColoredCutoutModel(this.getParentModel(), BIOME_TEXTURES[biome], poseStack, buffer, packedLight, entity, -1);
        renderColoredCutoutModel(this.getParentModel(), entity.getJob().textureLocation(), poseStack, buffer, packedLight, entity, -1);
    }
}
