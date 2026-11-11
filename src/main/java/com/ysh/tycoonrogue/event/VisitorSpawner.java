package com.ysh.tycoonrogue.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import com.ysh.tycoonrogue.entity.CustomerJob;
import com.ysh.tycoonrogue.entity.CustomerRole;
import com.ysh.tycoonrogue.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Sends customers, thieves and armed raiders to players, and provides /tycoon test commands. */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class VisitorSpawner {
    /** Turn natural visitors on or off with /tycoon visitors true|false. */
    public static boolean enabled = true;

    private static final int CHECK_INTERVAL_TICKS = 400;
    private static final int MAX_NEARBY = 4;
    private static final double NEARBY_RADIUS = 48.0D;
    /** Minimum time between two raids on the same player: 15 minutes. */
    private static final long RAID_COOLDOWN_TICKS = 18000L;

    private static final Map<UUID, Long> LAST_RAID = new HashMap<>();

    private VisitorSpawner() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!enabled || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (player.tickCount % CHECK_INTERVAL_TICKS != 0 || player.isSpectator()) {
            return;
        }
        ServerLevel level = player.serverLevel();
        TycoonData data = TycoonData.get(level.getServer());
        // Cards like "Word of Mouth" raise the visitor limit and the visit chance.
        double visitors = RunStats.visitorMultiplier(data);
        int maxNearby = Math.max(2, Math.min(10, (int) Math.round(MAX_NEARBY * visitors)));
        List<CustomerEntity> nearby = level.getEntitiesOfClass(
                CustomerEntity.class, player.getBoundingBox().inflate(NEARBY_RADIUS));
        if (nearby.size() >= maxNearby) {
            return;
        }
        // A good reputation brings more visitors; bad word of mouth (rumor) keeps them away.
        double chance = Math.max(0.15D, Math.min(1.0D, (0.55D + (data.getReputation() - 50) / 100.0D) * visitors))
                * (1.0D - data.getRumor() / 130.0D);
        if (level.random.nextDouble() > chance) {
            return;
        }
        // Weights: 62 customer, 14 scammer, 14 robber, 10 armed raid (if off cooldown).
        // Cards like "Security Guard" shrink the three threat weights.
        double threat = RunStats.threatMultiplier(data);
        double scammer = 14.0D * threat;
        double robber = 14.0D * threat;
        double raid = raidReady(level, player) ? 10.0D * threat : 0.0D;
        double roll = level.random.nextDouble() * (62.0D + scammer + robber + raid);
        if (roll < 62.0D) {
            // Famous factories attract streamers: 3% base chance, more with buzz.
            boolean streamer = level.random.nextDouble() < 0.03D + data.getBuzz() * 0.0012D;
            spawnNear(level, player, CustomerRole.NORMAL, 14.0D, 22.0D, streamer);
        } else if (roll < 62.0D + scammer) {
            spawnNear(level, player, CustomerRole.SCAMMER, 14.0D, 22.0D);
        } else if (roll < 62.0D + scammer + robber) {
            spawnNear(level, player, CustomerRole.ROBBER, 14.0D, 22.0D);
        } else {
            spawnRaid(level, player, 1 + level.random.nextInt(3));
        }
    }

    private static boolean raidReady(ServerLevel level, ServerPlayer player) {
        Long last = LAST_RAID.get(player.getUUID());
        return last == null || level.getGameTime() - last >= RAID_COOLDOWN_TICKS;
    }

    /** Warns the player, then sends {@code count} armed raiders from far away. */
    public static void spawnRaid(ServerLevel level, ServerPlayer player, int count) {
        LAST_RAID.put(player.getUUID(), level.getGameTime());
        player.displayClientMessage(Component.translatable("message.tycoonrogue.raid.alert"), false);
        player.playNotifySound(SoundEvents.PILLAGER_CELEBRATE, SoundSource.HOSTILE, 2.0F, 0.8F);
        for (int i = 0; i < count; i++) {
            spawnNear(level, player, CustomerRole.RAIDER, 26.0D, 34.0D);
        }
    }

    /**
     * Spawns one customer {@code minDist} to {@code maxDist} blocks from the player.
     * Robbers and scammers wear a random ordinary job outfit as their disguise.
     */
    public static boolean spawnNear(ServerLevel level, ServerPlayer player, CustomerRole role,
                                    double minDist, double maxDist) {
        return spawnNear(level, player, role, minDist, maxDist, false);
    }

    public static boolean spawnNear(ServerLevel level, ServerPlayer player, CustomerRole role,
                                    double minDist, double maxDist, boolean streamer) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = minDist + level.random.nextDouble() * (maxDist - minDist);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos pos = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
            if (!level.getFluidState(pos.below()).isEmpty() || !level.getBlockState(pos.below()).isSolid()) {
                continue;
            }
            CustomerEntity customer = ModEntities.CUSTOMER.get().create(level);
            if (customer == null) {
                return false;
            }
            customer.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
            CustomerJob job = role == CustomerRole.RAIDER ? CustomerJob.RAIDER : CustomerJob.randomCivilian(level.random);
            customer.initialize(job, role, true);
            if (streamer) {
                customer.makeStreamer();
            }
            level.addFreshEntity(customer);
            return true;
        }
        return false;
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("tycoon")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("visitors")
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(context -> {
                                    enabled = BoolArgumentType.getBool(context, "enabled");
                                    context.getSource().sendSuccess(
                                            () -> Component.literal("Natural visitors: " + enabled), true);
                                    return 1;
                                })))
                .then(Commands.literal("spawn")
                        .then(Commands.literal("customer")
                                .executes(context -> spawnCommand(context, CustomerRole.NORMAL)))
                        .then(Commands.literal("streamer").executes(context -> {
                            ServerPlayer p = context.getSource().getPlayer();
                            return p != null && spawnNear(p.serverLevel(), p, CustomerRole.NORMAL, 8.0D, 14.0D, true) ? 1 : 0;
                        }))
                        .then(Commands.literal("robber")
                                .executes(context -> spawnCommand(context, CustomerRole.ROBBER)))
                        .then(Commands.literal("scammer")
                                .executes(context -> spawnCommand(context, CustomerRole.SCAMMER)))
                        .then(Commands.literal("raid")
                                .executes(context -> spawnCommand(context, CustomerRole.RAIDER)))));
    }

    private static int spawnCommand(CommandContext<CommandSourceStack> context, CustomerRole role) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        boolean ok;
        if (role == CustomerRole.RAIDER) {
            spawnRaid(level, player, 2);
            ok = true;
        } else {
            ok = spawnNear(level, player, role, 8.0D, 14.0D);
        }
        context.getSource().sendSuccess(
                () -> Component.literal(ok ? "Spawned " + role.name().toLowerCase() : "No valid spot found, try again"),
                false);
        return ok ? 1 : 0;
    }
}
