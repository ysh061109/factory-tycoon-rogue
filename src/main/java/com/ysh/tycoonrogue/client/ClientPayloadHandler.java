package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.net.FeedPayload;
import com.ysh.tycoonrogue.net.OpenCardsPayload;
import com.ysh.tycoonrogue.net.RunStatePayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Handles messages the server sends to the client. Only ever loaded on the client. */
public final class ClientPayloadHandler {
    private ClientPayloadHandler() {
    }

    public static void handleRunState(RunStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRunState.update(payload));
    }

    public static void handleOpenCards(OpenCardsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(
                new CardScreen(payload.round(), payload.cards(), payload.rarities())));
    }

    public static void handleFeed(FeedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new PhoneScreen(payload)));
    }
}
