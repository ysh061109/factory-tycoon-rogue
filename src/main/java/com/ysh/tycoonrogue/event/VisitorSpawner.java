package com.ysh.tycoonrogue.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ysh.tycoonrogue.TycoonRogue;
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
        List<CustomerEntity> nearby = level.getEntitiesOfClass(
                CustomerEntity.class, player.getBoundingBox().inflate(NEARBY_RADIUS));
        if (nearby.size() >= MAX_NEARBY) {
            return;
        }
        // 62% customer, 14% disguised scammer, 14% disguised robber, 10% armed raid (if off cooldown).
        int roll = level.random.nextInt(100);
        if (roll >= 90 && raidReady(level, player)) {
            spawnRaid(level, player, 1 + level.random.nextInt(3));
        } else if (roll >= 76) {
            spawnNear(level, player, CustomerRole.ROBBER, 14.0D, 22.0D);
        } else if (roll >= 62) {
            spawnNear(level, player, CustomerRole.SCAMMER, 14.0D, 22.0D);
        } else {
            spawnNear(level, player, CustomerRole.NORMAL, 14.0D, 22.0D);
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
