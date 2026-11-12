package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;

/** Draws the hair on top of the body texture, tinted with this customer's hair colour. */
public class CustomerHairLayer extends RenderLayer<CustomerEntity, CustomerModel> {
    private static final int[] HAIR_COLORS = {
            0x1C1612, 0x3A2A1E, 0x5A3A22, 0x7A3E22, 0xC9A45A, 0x9A9A9A, 0xE6E0D2, 0xB5482A, 0x2B2F4A, 0x6A3B6B};

    public CustomerHairLayer(RenderLayerParent<CustomerEntity, CustomerModel> parent) {
        super(parent);
    }

    public static int hairColor(CustomerEntity entity) {
        return HAIR_COLORS[(int) Math.floorMod(entity.getUUID().getLeastSignificantBits() >>> 5, (long) HAIR_COLORS.length)];
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, CustomerEntity entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        if (entity.isInvisible() || CustomerModelDef.HAIR_STYLES[CustomerModel.hairStyleFor(entity)].length == 0) {
            return;
        }
        CustomerModel model = this.getParentModel();
        model.beginHairPass();
        renderColoredCutoutModel(model, this.getTextureLocation(entity), poseStack, buffer, packedLight, entity,
                0xFF000000 | hairColor(entity));
        model.endHairPass();
    }
}
