package com.ysh.tycoonrogue.event;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.CardDefinition;
import com.ysh.tycoonrogue.data.CardManager;
import com.ysh.tycoonrogue.data.Feed;
import com.ysh.tycoonrogue.entity.CustomerJob;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.net.OpenCardsPayload;
import com.ysh.tycoonrogue.net.RunStatePayload;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the round timer. Each round lasts one Minecraft day. At the end, if enough emeralds were
 * earned, the player picks one of three cards. Otherwise reputation drops, and after three
 * failed rounds in a row the run starts over.
 */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class RoundManager {
    private static final int CARDS_OFFERED = 3;
    private static final int SUCCESS_REPUTATION = 5;
    private static final int FAIL_REPUTATION = -10;

    private RoundManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getPlayerList().getPlayerCount() == 0) {
            return;
        }
        TycoonData data = TycoonData.get(server);
        // The clock waits while the player is choosing a card.
        if (data.getPending().isEmpty()) {
            data.tickRound(1);
            if (data.getTicksLeft() <= 0) {
                endRound(server, data);
            }
        }
        if (server.getTickCount() % 20 == 0) {
            broadcastState(server);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getServer() != null) {
            TycoonData data = TycoonData.get(player.getServer());
            if (data.getTrendJob().isEmpty()) {
                rollTrend(player.getServer(), data);
            }
            PacketDistributor.sendToPlayer(player, statePayload(data));
            sendPendingCards(player, data);
        }
    }

    public static RunStatePayload statePayload(TycoonData data) {
        return new RunStatePayload(data.getRound(), data.getTicksLeft(), data.getRevenue(), data.getQuota(),
                data.getReputation(), data.getRumor(), new ArrayList<>(data.getCards()));
    }

    public static void broadcastState(MinecraftServer server) {
        PacketDistributor.sendToAllPlayers(statePayload(TycoonData.get(server)));
    }

    /** Opens the card screen for this player if a choice is waiting. */
    public static void sendPendingCards(ServerPlayer player, TycoonData data) {
        if (data.getPending().isEmpty()) {
            return;
        }
        List<ResourceLocation> ids = new ArrayList<>(data.getPending());
        List<Integer> rarities = new ArrayList<>();
        for (ResourceLocation id : ids) {
            CardDefinition card = CardManager.INSTANCE.get(id);
            rarities.add(card == null ? 0 : card.rarity().ordinal());
        }
        PacketDistributor.sendToPlayer(player, new OpenCardsPayload(data.getRound() - 1, true, ids, rarities));
    }

    /** Ends the current round now (also used by /tycoon round end for testing). */
    public static void endRound(MinecraftServer server, TycoonData data) {
        int round = data.getRound();
        int revenue = data.getRevenue();
        int quota = data.getQuota();
        boolean success = revenue >= quota;

        if (success) {
            broadcast(server, Component.translatable("message.tycoonrogue.round.success", round, revenue, quota));
            data.setFailStreak(0);
            Feed.news(server, "round_success", String.valueOf(round));
            data.changeReputation(SUCCESS_REPUTATION, null, "");
            data.setPending(CardManager.INSTANCE.draw(round, CARDS_OFFERED, server.overworld().getRandom()));
            data.startNextRound();
        } else {
            broadcast(server, Component.translatable("message.tycoonrogue.round.fail", round, revenue, quota,
                    -FAIL_REPUTATION));
            data.changeReputation(FAIL_REPUTATION, null, "");
            data.setFailStreak(data.getFailStreak() + 1);
            Feed.news(server, "round_fail", String.valueOf(round));
            if (data.getFailStreak() >= TycoonData.MAX_FAIL_STREAK) {
                data.resetRun();
                broadcast(server, Component.translatable("message.tycoonrogue.run.over"));
            } else {
                data.startNextRound();
            }
        }
        rollTrend(server, data);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendPendingCards(player, data);
        }
        broadcastState(server);
    }

    /** Picks the trending job for the new round and announces it in the news. */
    public static void rollTrend(MinecraftServer server, TycoonData data) {
        CustomerJob job = CustomerJob.randomCivilian(server.overworld().getRandom());
        data.setTrendJob(job.id());
        Feed.news(server, "trend", "$job.tycoonrogue." + job.id());
    }

    private static void broadcast(MinecraftServer server, Component message) {
        server.getPlayerList().broadcastSystemMessage(message, false);
    }
}
