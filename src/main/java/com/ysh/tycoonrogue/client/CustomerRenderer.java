package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

import java.util.List;

public class CustomerRenderer extends MobRenderer<CustomerEntity, CustomerModel> {
    public static final net.minecraft.client.model.geom.ModelLayerLocation LAYER =
            new net.minecraft.client.model.geom.ModelLayerLocation(
                    ResourceLocation.fromNamespaceAndPath("tycoonrogue", "customer"), "main");
    private static final java.util.Map<String, ResourceLocation> TEXTURES = new java.util.HashMap<>();

    /** Speech bubbles are only drawn within this distance (squared). */
    private static final double SPEECH_RANGE_SQR = 24.0D * 24.0D;
    private static final int BUBBLE_WIDTH = 120;
    private static final int LINE_HEIGHT = 10;

    public CustomerRenderer(EntityRendererProvider.Context context) {
        super(context, new CustomerModel(context.bakeLayer(LAYER)), 0.45F);
        this.addLayer(new CustomerHairLayer(this));
        this.addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this, context.getItemInHandRenderer()));
    }

    @Override
    public ResourceLocation getTextureLocation(CustomerEntity entity) {
        int skin = Math.floorMod(entity.getBiome(), CustomerModelDef.SKIN_COUNT);
        String key = "skin" + skin + "/" + entity.getJob().id();
        return TEXTURES.computeIfAbsent(key, k -> ResourceLocation.fromNamespaceAndPath(
                "tycoonrogue", "textures/entity/customer/" + k + ".png"));
    }

    /** Every customer has their own build: height and width vary a little. */
    @Override
    protected void scale(CustomerEntity entity, PoseStack poseStack, float partialTick) {
        long bits = entity.getUUID().getMostSignificantBits();
        float height = 0.93F + (Math.floorMod(bits >>> 20, 100L)) / 100.0F * 0.10F;
        float width = 0.92F + (Math.floorMod(bits >>> 33, 100L)) / 100.0F * 0.16F;
        poseStack.scale(width, height, width);
    }

    /**
     * Speech bubbles use the game's own name-tag drawing (the proven path for floating text), with the
     * customer's current line instead of a name. Long lines are wrapped and stacked upwards.
     */
    @Override
    protected boolean shouldShowName(CustomerEntity entity) {
        return !entity.getSpeech().getString().isEmpty()
                && this.entityRenderDispatcher.distanceToSqr(entity) <= SPEECH_RANGE_SQR;
    }

    @Override
    protected void renderNameTag(CustomerEntity entity, Component displayName, PoseStack poseStack,
                                 MultiBufferSource buffer, int packedLight, float partialTick) {
        Component speech = entity.getSpeech();
        if (speech.getString().isEmpty()) {
            return;
        }
        List<net.minecraft.network.chat.FormattedText> lines =
                this.getFont().getSplitter().splitLines(speech, BUBBLE_WIDTH, net.minecraft.network.chat.Style.EMPTY);
        int n = lines.size();
        for (int i = 0; i < n; i++) {
            poseStack.pushPose();
            poseStack.translate(0.0F, (n - 1 - i) * 0.26F + 0.15F, 0.0F);
            super.renderNameTag(entity, Component.literal(lines.get(i).getString()), poseStack, buffer,
                    packedLight, partialTick);
            poseStack.popPose();
        }
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
            // Trembling, leaning back, when a gun is pointed at them.
            case CustomerEntity.STATE_SCARED -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(-8.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(Mth.sin(time * 3.2F) * 4.0F));
            }
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
