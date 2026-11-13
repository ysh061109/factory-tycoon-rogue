package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.registry.ModEntities;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = TycoonRogue.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.CUSTOMER.get(), CustomerRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(CustomerRenderer.LAYER, CustomerModelDef::create);
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "run_hud"), TycoonHud::render);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "hologram_legend"), ClientHologram::renderLegend);
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "phone_toasts"), ToastOverlay::render);
    }

    public static final net.minecraft.client.KeyMapping DECK_KEY = new net.minecraft.client.KeyMapping(
            "key.tycoonrogue.deck", com.mojang.blaze3d.platform.InputConstants.KEY_K, "key.categories.tycoonrogue");

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(DECK_KEY);
    }
}
