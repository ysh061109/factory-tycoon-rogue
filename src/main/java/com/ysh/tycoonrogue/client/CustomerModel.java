package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The vanilla villager model with extra arm animations for charging, ransacking and fleeing. */
public class CustomerModel extends VillagerModel<CustomerEntity> {
    public CustomerModel(ModelPart root) {
        super(root);
    }

    @Override
    public void setupAnim(CustomerEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        ModelPart arms = this.root().getChild("arms");
        switch (entity.getState()) {
            // Arms thrown forward and shaking while charging.
            case CustomerEntity.STATE_CHARGE -> {
                arms.xRot = -2.4F + Mth.sin(ageInTicks * 0.9F) * 0.35F;
                arms.zRot = Mth.sin(ageInTicks * 0.45F) * 0.15F;
            }
            // Pumping arms while grabbing loot.
            case CustomerEntity.STATE_RANSACK -> {
                arms.xRot = -1.0F + Mth.sin(ageInTicks * 1.1F) * 0.7F;
                arms.zRot = 0.0F;
            }
            // Flailing arms while running away.
            case CustomerEntity.STATE_FLEE -> {
                arms.xRot = -1.6F + Mth.sin(ageInTicks * 1.5F) * 0.9F;
                arms.zRot = Mth.cos(ageInTicks * 1.5F) * 0.2F;
            }
            default -> {
            }
        }
    }
}
