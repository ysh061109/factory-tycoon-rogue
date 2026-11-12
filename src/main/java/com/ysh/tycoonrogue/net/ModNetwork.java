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
        registrar.playToClient(FeedPayload.TYPE, FeedPayload.CODEC, ClientPayloadHandler::handleFeed);
        registrar.playToClient(FeedToastPayload.TYPE, FeedToastPayload.CODEC, ClientPayloadHandler::handleToast);
        registrar.playToClient(OpenShopPayload.TYPE, OpenShopPayload.CODEC, ClientPayloadHandler::handleOpenShop);
        registrar.playToClient(HologramPayload.TYPE, HologramPayload.CODEC, ClientPayloadHandler::handleHologram);
        registrar.playToServer(BuyOfferPayload.TYPE, BuyOfferPayload.CODEC, ServerPayloadHandler::handleBuy);
        registrar.playToServer(PickCardPayload.TYPE, PickCardPayload.CODEC, ServerPayloadHandler::handlePickCard);
    }
}
