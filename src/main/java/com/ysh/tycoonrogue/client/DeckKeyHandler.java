package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = TycoonRogue.MODID, value = Dist.CLIENT)
public final class DeckKeyHandler {
    private DeckKeyHandler() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (ClientSetup.DECK_KEY.consumeClick()) {
            if (mc.player != null && mc.screen == null) {
                mc.setScreen(new DeckScreen());
            }
        }
    }
}
