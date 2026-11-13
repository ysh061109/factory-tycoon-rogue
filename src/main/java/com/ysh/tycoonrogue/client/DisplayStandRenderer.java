package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ysh.tycoonrogue.block.DisplayStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Draws the displayed item floating and slowly spinning under the glass dome. */
public class DisplayStandRenderer implements BlockEntityRenderer<DisplayStandBlockEntity> {
    public DisplayStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DisplayStandBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        ItemStack stack = be.getShown();
        if (stack.isEmpty() || be.getLevel() == null) {
            return;
        }
        float time = be.getLevel().getGameTime() + partialTick;
        pose.pushPose();
        pose.translate(0.5D, 0.62D + Math.sin(time / 12.0D) * 0.03D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(time * 2.0F));
        pose.scale(0.55F, 0.55F, 0.55F);
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, 0xF000F0, overlay,
                pose, buffer, be.getLevel(), 0);
        pose.popPose();
    }
}
