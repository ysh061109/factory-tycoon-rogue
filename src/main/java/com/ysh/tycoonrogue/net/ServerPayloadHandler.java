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

    /** Buys one offer from the Supply Desk. Everything is checked here, never trusted from the client. */
    public static void handleBuy(BuyOfferPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)
                    || !com.ysh.tycoonrogue.block.SupplyDeskBlock.isAtDesk(player)) {
                return;
            }
            var offer = com.ysh.tycoonrogue.shop.ShopOffers.byId(payload.offer());
            if (offer == null || !com.ysh.tycoonrogue.shop.ShopOffers.available(offer)) {
                return;
            }
            if (com.ysh.tycoonrogue.entity.Currency.balance(player) < offer.price()) {
                player.displayClientMessage(Component.translatable("message.tycoonrogue.shop.poor"), true);
                return;
            }
            var taken = com.ysh.tycoonrogue.entity.Currency.take(player, offer.price());
            int value = com.ysh.tycoonrogue.entity.Currency.valueOf(taken);
            if (value < offer.price()) {
                // Could not collect the full price: give back what was taken.
                com.ysh.tycoonrogue.entity.Currency.pay(player, value);
                player.displayClientMessage(Component.translatable("message.tycoonrogue.shop.poor"), true);
                return;
            }
            if (value > offer.price()) {
                com.ysh.tycoonrogue.entity.Currency.pay(player, value - offer.price()); // change
            }
            for (var stack : offer.goods().get()) {
                player.getInventory().placeItemBackInInventory(stack);
            }
            player.displayClientMessage(Component.translatable("message.tycoonrogue.shop.bought",
                    Component.translatable(offer.nameKey())), true);
            com.ysh.tycoonrogue.block.SupplyDeskBlock.sendShop(player);
        });
    }

    public static final int REROLL_COST = 6;
    public static final int MAX_REROLLS = 3;

    /** Draws new cards for the waiting choice, for a price. Checked here on the server. */
    public static void handleReroll(RerollCardsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || player.getServer() == null) {
                return;
            }
            MinecraftServer server = player.getServer();
            TycoonData data = TycoonData.get(server);
            if (data.getPending().isEmpty() || data.getRerolls() >= MAX_REROLLS) {
                return;
            }
            if (com.ysh.tycoonrogue.entity.Currency.balance(player) < REROLL_COST) {
                player.displayClientMessage(Component.translatable("message.tycoonrogue.reroll.poor", REROLL_COST), true);
                return;
            }
            var taken = com.ysh.tycoonrogue.entity.Currency.take(player, REROLL_COST);
            int value = com.ysh.tycoonrogue.entity.Currency.valueOf(taken);
            if (value < REROLL_COST) {
                com.ysh.tycoonrogue.entity.Currency.pay(player, value);
                player.displayClientMessage(Component.translatable("message.tycoonrogue.reroll.poor", REROLL_COST), true);
                return;
            }
            if (value > REROLL_COST) {
                com.ysh.tycoonrogue.entity.Currency.pay(player, value - REROLL_COST);
            }
            int rerolls = data.getRerolls() + 1;
            data.setPending(CardManager.INSTANCE.draw(Math.max(1, data.getRound() - 1), 3, server.overworld().getRandom()));
            for (int i = 0; i < rerolls; i++) {
                data.addReroll();
            }
            RoundManager.sendPendingCards(player, data);
        });
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
