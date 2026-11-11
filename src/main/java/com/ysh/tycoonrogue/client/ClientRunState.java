package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.net.RunStatePayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/** The latest run state the server sent, shown on the HUD. */
public final class ClientRunState {
    public static boolean active;
    public static int round = 1;
    public static int ticksLeft;
    public static int revenue;
    public static int quota;
    public static int reputation;
    public static int rumor;
    public static List<ResourceLocation> cards = List.of();

    private ClientRunState() {
    }

    public static void update(RunStatePayload payload) {
        active = true;
        round = payload.round();
        ticksLeft = payload.ticksLeft();
        revenue = payload.revenue();
        quota = payload.quota();
        reputation = payload.reputation();
        rumor = payload.rumor();
        cards = payload.cards();
    }
}
