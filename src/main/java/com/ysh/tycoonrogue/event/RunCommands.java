package com.ysh.tycoonrogue.event;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Admin / test commands for the run: /tycoon status, round end, cards, reset. */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class RunCommands {
    private RunCommands() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tycoon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> status(context.getSource())))
                .then(Commands.literal("round")
                        .then(Commands.literal("end").executes(context -> {
                            var server = context.getSource().getServer();
                            RoundManager.endRound(server, TycoonData.get(server));
                            return 1;
                        })))
                .then(Commands.literal("cards").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        return 0;
                    }
                    RoundManager.sendPendingCards(player, TycoonData.get(context.getSource().getServer()));
                    return 1;
                }))
                .then(Commands.literal("reset").executes(context -> {
                    var server = context.getSource().getServer();
                    TycoonData.get(server).resetRun();
                    RoundManager.broadcastState(server);
                    context.getSource().sendSuccess(() -> Component.literal("Run reset to round 1."), true);
                    return 1;
                })));
    }

    private static int status(CommandSourceStack source) {
        TycoonData data = TycoonData.get(source.getServer());
        String text = String.format(
                "Round %d | revenue %d / quota %d | %d s left | reputation %d | rumor %d | cards %d | failed in a row %d%s",
                data.getRound(), data.getRevenue(), data.getQuota(), data.getTicksLeft() / 20,
                data.getReputation(), data.getRumor(), data.getCards().size(), data.getFailStreak(),
                data.getPending().isEmpty() ? "" : " | card choice waiting (/tycoon cards)");
        source.sendSuccess(() -> Component.literal(text), false);
        return 1;
    }
}
