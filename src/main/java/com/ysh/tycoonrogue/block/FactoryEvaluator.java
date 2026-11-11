package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.entity.Taste;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Looks at the factory around a Tycoon Counter and turns it into scores.
 *
 * - Scale: how many machine blocks there are (Create blocks and vanilla machines), 0..40
 * - Variety: how many different kinds of machine, 0..20
 * - Output: this round's revenue against the quota, 0..15
 * - Showcase: items in chests nearby that match a customer's taste, 0..25 per taste
 */
public final class FactoryEvaluator {
    private static final int RADIUS = 14;
    private static final int DOWN = 4;
    private static final int UP = 10;
    private static final long CACHE_TICKS = 400L;

    /** Vanilla blocks that count as machines (every block of the "create" namespace counts too). */
    private static final Set<String> VANILLA_MACHINES = Set.of(
            "furnace", "blast_furnace", "smoker", "hopper", "dropper", "dispenser", "piston", "sticky_piston",
            "observer", "crafter", "smithing_table", "anvil", "brewing_stand", "cauldron", "composter",
            "grindstone", "stonecutter", "loom", "fletching_table", "cartography_table");

    public record Report(int machineBlocks, int machineTypes, int scale, int variety, int output,
                         Map<Taste, Integer> showcase, List<BlockPos> spots, long time) {
    }

    private static final Map<String, Report> CACHE = new HashMap<>();

    private FactoryEvaluator() {
    }

    public static Report evaluate(ServerLevel level, BlockPos center) {
        String key = level.dimension().location() + "@" + center.asLong();
        Report cached = CACHE.get(key);
        if (cached != null && level.getGameTime() - cached.time() < CACHE_TICKS) {
            return cached;
        }

        // Machines.
        Map<Block, Boolean> isMachine = new HashMap<>();
        Set<Block> kinds = new HashSet<>();
        int machines = 0;
        List<BlockPos> spots = new ArrayList<>();
        java.util.Random sampler = new java.util.Random(center.asLong() ^ level.getGameTime());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -DOWN; dy <= UP; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    Block block = state.getBlock();
                    if (isMachine.computeIfAbsent(block, FactoryEvaluator::machine)) {
                        machines++;
                        // Reservoir sample of up to 12 machine spots for customers to walk to.
                        if (spots.size() < 12) {
                            spots.add(cursor.immutable());
                        } else if (sampler.nextInt(machines) < 12) {
                            spots.set(sampler.nextInt(12), cursor.immutable());
                        }
                        kinds.add(block);
                    }
                }
            }
        }

        // Showcase chests.
        Map<Taste, Integer> showcase = new EnumMap<>(Taste.class);
        for (Taste taste : Taste.values()) {
            showcase.put(taste, 0);
        }
        int minChunkX = (center.getX() - RADIUS) >> 4;
        int maxChunkX = (center.getX() + RADIUS) >> 4;
        int minChunkZ = (center.getZ() - RADIUS) >> 4;
        int maxChunkZ = (center.getZ() + RADIUS) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity entity : level.getChunk(cx, cz).getBlockEntities().values()) {
                    BlockPos pos = entity.getBlockPos();
                    if (Math.abs(pos.getX() - center.getX()) > RADIUS || Math.abs(pos.getZ() - center.getZ()) > RADIUS
                            || pos.getY() < center.getY() - DOWN || pos.getY() > center.getY() + UP) {
                        continue;
                    }
                    if (entity instanceof BaseContainerBlockEntity container) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (stack.isEmpty()) {
                                continue;
                            }
                            for (Taste taste : Taste.values()) {
                                if (taste.items().contains(stack.getItem())) {
                                    showcase.merge(taste, stack.getCount(), Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }

        TycoonData data = TycoonData.get(level.getServer());
        int scale = Math.min(40, machines / 5);
        int variety = Math.min(20, kinds.size());
        int output = Math.min(15, data.getRevenue() * 15 / Math.max(1, data.getQuota()));
        Report report = new Report(machines, kinds.size(), scale, variety, output, showcase, spots, level.getGameTime());
        CACHE.put(key, report);
        return report;
    }

    private static boolean machine(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return key.getNamespace().equals("create")
                || (key.getNamespace().equals("minecraft") && VANILLA_MACHINES.contains(key.getPath()));
    }

    /** Showcase score for one taste, 0..25 before card bonuses. 100 matching items is the maximum. */
    public static int showcaseScore(Report report, Taste taste) {
        return Math.min(25, report.showcase().getOrDefault(taste, 0) / 4);
    }

    /**
     * The overall factory score, 0..100, as seen by a customer with the given taste.
     * Picky customers score lower, generous ones higher, and cards can boost their favourite taste.
     */
    public static int score(Report report, Taste taste, int pickiness, int generosity, TycoonData data) {
        double taste25 = showcaseScore(report, taste) * (1.0D + RunStats.tasteBonus(data, taste));
        double bonus = RunStats.ratingBonus(data);
        double total = bonus + report.scale() + report.variety() + report.output() + Math.min(37.5D, taste25);
        total -= pickiness / 100.0D * 15.0D;
        total += generosity / 100.0D * 10.0D;
        return (int) Math.max(0, Math.min(100, Math.round(total)));
    }

    public static int stars(int score) {
        return Math.max(1, Math.min(5, (int) Math.round(score / 20.0D)));
    }
}
