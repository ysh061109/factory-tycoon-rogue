package com.ysh.tycoonrogue.registry;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, TycoonRogue.MODID);

    public static final Supplier<EntityType<CustomerEntity>> CUSTOMER = ENTITY_TYPES.register(
            "customer",
            () -> EntityType.Builder.of(CustomerEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build("customer"));

    private ModEntities() {
    }
}
