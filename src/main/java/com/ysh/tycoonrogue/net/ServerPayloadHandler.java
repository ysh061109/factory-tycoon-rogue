package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.data.CardDefinition;
import com.ysh.tycoonrogue.data.CardManager;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.event.RoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Handles messages that clients send to the server. */
public final class ServerPayloadHandler {
    private ServerPayloadHandler() {
    }

    public static void handlePickCard(PickCardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.getServer() == null) {
                return;
            }
            MinecraftServer server = player.getServer();
            TycoonData data = TycoonData.get(server);
            // The server checks the card was really on offer, so a modified client cannot pick any card.
            if (!data.pick(payload.card())) {
                return;
            }
            CardDefinition card = CardManager.INSTANCE.get(payload.card());
            String nameKey = card == null ? payload.card().toString() : card.nameKey();
            server.getPlayerList().broadcastSystemMessage(Component.translatable(
                    "message.tycoonrogue.card.picked", player.getDisplayName(), Component.translatable(nameKey)), false);
            RoundManager.broadcastState(server);
            com.ysh.tycoonrogue.event.VisitorSpawner.ensureGuards(player);
        });
    }
}
