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

    @Override
    public void render(CustomerEntity entity, float entityYaw, float partialTicks, PoseStack poseStack,
                       MultiBufferSource buffer, int packedLight) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
        renderSpeech(entity, poseStack, buffer);
    }

    /** Draws what the customer is saying as a bubble that always faces the camera. */
    private void renderSpeech(CustomerEntity entity, PoseStack poseStack, MultiBufferSource buffer) {
        Component speech = entity.getSpeech();
        if (speech.getString().isEmpty() || this.entityRenderDispatcher.distanceToSqr(entity) > SPEECH_RANGE_SQR) {
            return;
        }
        Font font = this.getFont();
        List<FormattedCharSequence> lines = font.split(speech, BUBBLE_WIDTH);
        if (lines.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0F, entity.getBbHeight() + 0.55F + lines.size() * 0.2F, 0.0F);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(-0.022F, -0.022F, 0.022F);
        Matrix4f matrix = poseStack.last().pose();
        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            float x = -font.width(line) / 2.0F;
            float y = (i - lines.size() + 1) * LINE_HEIGHT;
            font.drawInBatch(line, x, y, 0xFF1A1A1A, false, matrix, buffer, Font.DisplayMode.NORMAL,
                    0xEEFFFFFF, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
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
