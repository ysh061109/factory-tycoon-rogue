package com.ysh.tycoonrogue.event;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import com.ysh.tycoonrogue.entity.CustomerJob;
import com.ysh.tycoonrogue.registry.ModEntities;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/** Sends customers, robbers and scammers to players, and provides /tycoon test commands. */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class VisitorSpawner {
    /** Turn natural visitors on or off with /tycoon visitors true|false. */
    public static boolean enabled = true;

    private static final int CHECK_INTERVAL_TICKS = 400;
    private static final int MAX_NEARBY = 4;
    private static final double NEARBY_RADIUS = 48.0D;

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
        // 70% customer, 15% scammer, 15% robber.
        int roll = level.random.nextInt(100);
        CustomerJob job = roll < 70 ? CustomerJob.randomNormal(level.random)
                : roll < 85 ? CustomerJob.SCAMMER : CustomerJob.ROBBER;
        spawnNear(level, player, job, true);
    }

    /** Spawns one customer 14 to 22 blocks away from the player. Returns false if no spot was found. */
    public static boolean spawnNear(ServerLevel level, ServerPlayer player, CustomerJob job, boolean visitor) {
        for (int attempt = 0; attempt < 10; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = 14.0D + level.random.nextDouble() * 8.0D;
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
            customer.initialize(job, visitor);
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
                        .then(Commands.literal("customer").executes(context -> spawnCommand(context, null)))
                        .then(Commands.literal("robber")
                                .executes(context -> spawnCommand(context, CustomerJob.ROBBER)))
                        .then(Commands.literal("scammer")
                                .executes(context -> spawnCommand(context, CustomerJob.SCAMMER)))));
    }

    private static int spawnCommand(CommandContext<CommandSourceStack> context, CustomerJob job) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return 0;
        }
        ServerLevel level = player.serverLevel();
        CustomerJob chosen = job != null ? job : CustomerJob.randomNormal(level.random);
        boolean ok = spawnNear(level, player, chosen, true);
        context.getSource().sendSuccess(
                () -> Component.literal(ok ? "Spawned " + chosen.id() : "No valid spot found, try again"), false);
        return ok ? 1 : 0;
    }
}
