package com.ysh.tycoonrogue.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;

/**
 * A fully custom humanoid model (see tools/gen_customer_model.py): separate head, body, arms and legs,
 * ears, nose, eyelids, five hair styles (tinted in a second pass) and job gear such as hats, goggles,
 * aprons, vests, backpacks and armour. Arms and legs swing, customers breathe, gesture while talking
 * and blink.
 */
public class CustomerModel extends EntityModel<CustomerEntity> implements ArmedModel {
    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final Map<String, ModelPart> parts = new HashMap<>();
    private final ModelPart[] hair = new ModelPart[CustomerModelDef.HAIR_PARTS.length];
    private final List<ModelPart> nonHair = new ArrayList<>();
    private int hairStyle;

    public CustomerModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
        this.head = root.getChild("head");
        this.body = root.getChild("body");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
        this.rightLeg = root.getChild("right_leg");
        this.leftLeg = root.getChild("left_leg");
        for (String path : CustomerModelDef.ALL_PARTS) {
            ModelPart part = root;
            for (String name : path.split("/")) {
                part = part.getChild(name);
            }
            parts.put(path, part);
        }
        for (int i = 0; i < hair.length; i++) {
            hair[i] = parts.get(CustomerModelDef.HAIR_PARTS[i]);
        }
        for (ModelPart part : parts.values()) {
            boolean isHair = false;
            for (ModelPart h : hair) {
                isHair |= h == part;
            }
            if (!isHair) {
                nonHair.add(part);
            }
        }
    }

    public ModelPart getRightArm() {
        return rightArm;
    }

    private void show(String[] paths, boolean visible) {
        for (String path : paths) {
            ModelPart part = parts.get(path);
            if (part != null) {
                part.visible = visible;
            }
        }
    }

    /** Hair style 0..5 for this customer. Hats rule out styles that would poke through them. */
    public static int hairStyleFor(CustomerEntity entity) {
        int style = (int) Math.floorMod(entity.getUUID().getMostSignificantBits() >>> 7, 6L);
        if (CustomerModelDef.HAT_JOBS.contains(entity.getJob().id()) && (style == 3 || style == 4)) {
            style = style == 3 ? 2 : 1;
        }
        return style;
    }

    /** Only the hair is drawn (tinted) in the second pass. */
    public void beginHairPass() {
        for (ModelPart part : nonHair) {
            part.skipDraw = true;
        }
        for (int i = 0; i < hair.length; i++) {
            hair[i].visible = false;
        }
        for (int index : CustomerModelDef.HAIR_STYLES[hairStyle]) {
            hair[index].visible = true;
        }
    }

    public void endHairPass() {
        for (ModelPart part : nonHair) {
            part.skipDraw = false;
        }
        for (ModelPart h : hair) {
            h.visible = false;
        }
    }

    @Override
    public void setupAnim(CustomerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        // ---- which gear is shown ----
        show(CustomerModelDef.ACCESSORY_PARTS, false);
        String[] gear = CustomerModelDef.JOB_PARTS.get(entity.getJob().id());
        if (gear != null) {
            show(gear, true);
        }
        int reveal = entity.getReveal();
        if (reveal == 1) {
            show(CustomerModelDef.REVEAL_1, true);
            // The disguise hat gives way to the beanie.
            show(new String[]{"head/cap_dome", "head/cap_visor", "head/hh_dome", "head/hh_brim", "head/hh_ridge",
                    "head/lamp", "head/chef_band", "head/chef_puff", "head/straw_brim", "head/straw_crown"}, false);
        } else if (reveal == 2) {
            show(CustomerModelDef.REVEAL_2, true);
            show(new String[]{"head/glasses"}, false);
        } else if (reveal == 3) {
            show(CustomerModelDef.REVEAL_3, true);
        }
        hairStyle = hairStyleFor(entity);
        for (ModelPart h : hair) {
            h.visible = false;
        }
        boolean blink = (entity.tickCount + entity.getId() * 37) % 110 < 4;
        parts.get(CustomerModelDef.LIDS).visible = blink && entity.getState() != CustomerEntity.STATE_SCARED;

        // ---- head ----
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        head.xRot = headPitch * Mth.DEG_TO_RAD;
        boolean talking = !entity.getSpeech().getString().isEmpty();
        if (talking) {
            head.xRot += Mth.sin(ageInTicks * 0.5F) * 0.05F;
        }
        float breath = Mth.sin(ageInTicks * 0.09F + entity.getId());

        // ---- walking ----
        float swing = limbSwingAmount;
        rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * swing;
        leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * swing;
        rightLeg.yRot = 0.0F;
        leftLeg.yRot = 0.0F;
        rightArm.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.2F * swing;
        leftArm.xRot = Mth.cos(limbSwing * 0.6662F) * 1.2F * swing;
        rightArm.yRot = 0.0F;
        leftArm.yRot = 0.0F;
        rightArm.zRot = 0.04F + breath * 0.03F;
        leftArm.zRot = -0.04F - breath * 0.03F;
        body.xRot = 0.0F;
        body.y = breath * 0.1F;

        // Holding something: the right arm comes forward a little.
        if (!entity.getMainHandItem().isEmpty()) {
            rightArm.xRot = rightArm.xRot * 0.5F - 0.7F;
            rightArm.yRot = -0.15F;
        }
        // Talking: the free hand gestures.
        if (talking) {
            leftArm.xRot = -0.9F + Mth.sin(ageInTicks * 0.22F) * 0.3F;
            leftArm.zRot = -0.25F + Mth.sin(ageInTicks * 0.33F) * 0.15F;
            leftArm.yRot = 0.3F;
        }

        // ---- states ----
        switch (entity.getState()) {
            case CustomerEntity.STATE_CHARGE -> {
                rightArm.xRot = -2.4F + Mth.sin(ageInTicks * 0.9F) * 0.35F;
                leftArm.xRot = rightArm.xRot;
                rightArm.zRot = Mth.sin(ageInTicks * 0.45F) * 0.15F;
                leftArm.zRot = -rightArm.zRot;
            }
            case CustomerEntity.STATE_RANSACK -> {
                rightArm.xRot = -1.0F + Mth.sin(ageInTicks * 1.1F) * 0.7F;
                leftArm.xRot = -1.0F - Mth.sin(ageInTicks * 1.1F) * 0.7F;
            }
            case CustomerEntity.STATE_SCARED -> {
                rightArm.xRot = -2.9F + Mth.sin(ageInTicks * 2.5F) * 0.12F;
                leftArm.xRot = rightArm.xRot;
                rightArm.zRot = 0.25F + Mth.sin(ageInTicks * 2.1F) * 0.2F;
                leftArm.zRot = -0.25F - Mth.sin(ageInTicks * 2.1F) * 0.2F;
                rightArm.yRot = 0.0F;
                leftArm.yRot = 0.0F;
            }
            case CustomerEntity.STATE_FLEE -> {
                rightArm.xRot = -1.6F + Mth.sin(ageInTicks * 1.5F) * 0.9F;
                leftArm.xRot = -1.6F - Mth.sin(ageInTicks * 1.5F) * 0.9F;
                rightArm.zRot = 0.2F;
                leftArm.zRot = -0.2F;
            }
            default -> {
            }
        }

        // Ponytail swings with the walk.
        ModelPart tail = hair[CustomerModelDef.TAIL_INDEX];
        tail.xRot = 0.15F + Math.min(1.0F, swing) * (0.35F + 0.25F * Mth.sin(limbSwing * 0.6662F))
                + Mth.sin(ageInTicks * 0.1F) * 0.04F;
        tail.zRot = Mth.sin(limbSwing * 0.3331F) * 0.15F * swing;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    @Override
    public void translateToHand(HumanoidArm arm, PoseStack poseStack) {
        (arm == HumanoidArm.RIGHT ? rightArm : leftArm).translateAndRotate(poseStack);
    }
}
