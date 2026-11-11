package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.client.ClientPayloadHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = TycoonRogue.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";

    private ModNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);
        registrar.playToClient(RunStatePayload.TYPE, RunStatePayload.CODEC, ClientPayloadHandler::handleRunState);
        registrar.playToClient(OpenCardsPayload.TYPE, OpenCardsPayload.CODEC, ClientPayloadHandler::handleOpenCards);
        registrar.playToServer(PickCardPayload.TYPE, PickCardPayload.CODEC, ServerPayloadHandler::handlePickCard);
    }
}
