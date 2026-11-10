package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;

/** Draws the biome skin and the profession outfit on top of the base villager texture. */
public class CustomerOutfitLayer extends RenderLayer<CustomerEntity, VillagerModel<CustomerEntity>> {
    private static final ResourceLocation[] BIOME_TEXTURES = new ResourceLocation[CustomerEntity.BIOMES.length];
    private static final ResourceLocation[] OUTFIT_TEXTURES = new ResourceLocation[CustomerEntity.OUTFITS.length];

    static {
        for (int i = 0; i < BIOME_TEXTURES.length; i++) {
            BIOME_TEXTURES[i] = ResourceLocation.withDefaultNamespace(
                    "textures/entity/villager/type/" + CustomerEntity.BIOMES[i] + ".png");
        }
        for (int i = 0; i < OUTFIT_TEXTURES.length; i++) {
            OUTFIT_TEXTURES[i] = ResourceLocation.withDefaultNamespace(
                    "textures/entity/villager/profession/" + CustomerEntity.OUTFITS[i] + ".png");
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

        int outfit = entity.getOutfit();
        if (outfit >= 0 && outfit < OUTFIT_TEXTURES.length) {
            renderColoredCutoutModel(this.getParentModel(), OUTFIT_TEXTURES[outfit], poseStack, buffer, packedLight, entity, -1);
        }
    }
}
