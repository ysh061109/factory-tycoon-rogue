package com.ysh.tycoonrogue.event;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.block.FactoryEvaluator;
import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Watches each factory over time. Every 5 seconds it checks whether goods are really being made
 * and moved: stock in chests, vaults, belts and depots changing while machines run, new items
 * appearing, or money coming in. Customers pay for a factory that keeps producing, not for one
 * that ran once. Only the last two minutes count. Not saved: after a restart it builds up again.
 */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class ProductionTracker {
    private static final int SAMPLE_TICKS = 100;
    private static final int WINDOWS = 24;
    /** 16 of the last 24 windows producing is a full score. */
    private static final int FULL_AT = 16;
    private static final int RADIUS = 14;

    private static final class State {
        final Deque<Boolean> history = new ArrayDeque<>();
        final Set<UUID> seenItems = new HashSet<>();
        long signature;
        boolean first = true;
        int revenue;
    }

    private static final Map<String, State> STATES = new HashMap<>();

    private ProductionTracker() {
    }

    private static String key(GlobalPos pos) {
        return pos.dimension().location() + "@" + pos.pos().asLong();
    }

    /** 0..10: how steadily this factory has been producing lately. */
    public static int flowScore(ServerLevel level, BlockPos counter) {
        State state = STATES.get(level.dimension().location() + "@" + counter.asLong());
        if (state == null) {
            return 0;
        }
        int producing = 0;
        for (boolean window : state.history) {
            if (window) {
                producing++;
            }
        }
        return Math.min(10, producing * 10 / FULL_AT);
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % SAMPLE_TICKS != 0) {
            return;
        }
        TycoonData data = TycoonData.get(server);
        Set<String> alive = new HashSet<>();
        for (GlobalPos counter : data.allCounters()) {
            ServerLevel level = server.getLevel(counter.dimension());
            if (level == null || !level.isLoaded(counter.pos())) {
                continue;
            }
            String key = key(counter);
            alive.add(key);
            sample(level, counter.pos(), STATES.computeIfAbsent(key, k -> new State()), data);
        }
        STATES.keySet().retainAll(alive);
    }

    private static void sample(ServerLevel level, BlockPos center, State state, TycoonData data) {
        long signature = 17L;
        for (int cx = (center.getX() - RADIUS) >> 4; cx <= (center.getX() + RADIUS) >> 4; cx++) {
            for (int cz = (center.getZ() - RADIUS) >> 4; cz <= (center.getZ() + RADIUS) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (Math.abs(pos.getX() - center.getX()) > RADIUS || Math.abs(pos.getZ() - center.getZ()) > RADIUS
                            || Math.abs(pos.getY() - center.getY()) > RADIUS) {
                        continue;
                    }
                    IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pos,
                            entity.getBlockState(), entity, null);
                    if (handler == null) {
                        continue;
                    }
                    int total = 0;
                    for (int slot = 0; slot < handler.getSlots(); slot++) {
                        total += handler.getStackInSlot(slot).getCount();
                    }
                    signature = signature * 31L + pos.asLong() * 7L + total;
                }
            }
        }
        boolean newItems = false;
        AABB box = new AABB(center).inflate(RADIUS);
        Set<UUID> now = new HashSet<>();
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box)) {
            now.add(item.getUUID());
            if (item.getAge() < SAMPLE_TICKS && !state.seenItems.contains(item.getUUID())) {
                newItems = true;
            }
        }
        state.seenItems.clear();
        state.seenItems.addAll(now);
        boolean moneyIn = data.getRevenue() > state.revenue;
        boolean changed = !state.first && (signature != state.signature || newItems || moneyIn);
        // Something must also be running, otherwise a player moving chest items would count.
        boolean running = FactoryEvaluator.evaluate(level, center).active() > 0;
        state.history.addLast(changed && running);
        while (state.history.size() > WINDOWS) {
            state.history.removeFirst();
        }
        state.signature = signature;
        state.revenue = data.getRevenue();
        state.first = false;
    }
}
