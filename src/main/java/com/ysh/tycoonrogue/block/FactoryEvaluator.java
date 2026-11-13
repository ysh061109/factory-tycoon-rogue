package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.entity.ModCompat;
import com.ysh.tycoonrogue.entity.Taste;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Looks at the factory around a Tycoon Counter and turns it into scores. A customer's rating
 * (0..100) is the sum of:
 *
 * - Scale (25): how many machine blocks there are (Create and its add-ons, plus vanilla machines)
 * - Variety (10): how many different kinds of machine
 * - Output (15): this round's revenue against the quota
 * - Operation (15): machines that are really running (spinning Create parts, lit furnaces)
 * - Taste (15): what the customer loves: items in chests, or real buildings (see below)
 * - Art (10): decoration variety, lighting, greenery and heritage architecture
 * - Colour (10): how much of the customer's favourite colour is built into the factory
 *
 * History lovers look at architecture (tiled roofs, painted eaves, libraries). Airplane and car
 * lovers also count real vehicles built with Create Aeronautics and Offroad, and the parts for them.
 */
public final class FactoryEvaluator {
    private static final int RADIUS = 14;
    private static final int DOWN = 4;
    private static final int UP = 10;
    private static final long CACHE_TICKS = 400L;
    private static final DyeColor[] COLORS_BY_LENGTH = sortedColors();

    /** Vanilla blocks that count as machines (every block of Create and its add-ons counts too). */
    private static final Set<String> VANILLA_MACHINES = Set.of(
            "furnace", "blast_furnace", "smoker", "hopper", "dropper", "dispenser", "piston", "sticky_piston",
            "observer", "crafter", "smithing_table", "anvil", "brewing_stand", "cauldron", "composter",
            "grindstone", "stonecutter", "loom", "fletching_table", "cartography_table");
    private static final Set<String> LIT_MACHINES = Set.of("furnace", "blast_furnace", "smoker");

    /** Blocks that are scenery, not decoration someone built. */
    private static final Set<String> NATURAL = Set.of(
            "stone", "cobblestone", "dirt", "grass_block", "coarse_dirt", "podzol", "mycelium", "sand", "red_sand",
            "gravel", "deepslate", "netherrack", "bedrock", "water", "lava", "andesite", "diorite", "granite",
            "tuff", "clay", "snow", "snow_block", "ice", "packed_ice", "dripstone_block", "dirt_path", "farmland",
            "short_grass", "tall_grass", "moss_block", "calcite", "basalt", "mud", "rooted_dirt");

    public record Report(int machineBlocks, int machineTypes, int scale, int variety, int output, int active,
                         int operation, int art, int[] colors, Map<Taste, Integer> showcase, List<BlockPos> spots,
                         int roofs, int dancheong, int libraries, int historyScore, int aircraft, int cars,
                         int beltCount, int wheelCount, int thrusterCount, List<BlockPos> belts,
                         List<BlockPos> wheelSpots, List<BlockPos> thrusterSpots, int propellerCount, int sailCount,
                         int engineCount, int balloonCount, List<BlockPos> propellerSpots,
                         List<BlockPos> sailSpots, List<BlockPos> engineSpots, List<BlockPos> balloonSpots,
                         long time) {
    }

    /** All parts of one customer's rating, for the explanation shown to the player. */
    public record Breakdown(int scale, int variety, int output, int operation, int taste, int art, int colour,
                            int flow, int inspect, int total) {
    }

    private static final Map<String, Report> CACHE = new HashMap<>();
    private static final Map<Class<?>, Method> SPEED = new HashMap<>();
    private static final Method NONE;

    static {
        Method none;
        try {
            none = Object.class.getMethod("hashCode");
        } catch (NoSuchMethodException e) {
            none = null;
        }
        NONE = none;
    }

    private FactoryEvaluator() {
    }

    private static DyeColor[] sortedColors() {
        DyeColor[] all = DyeColor.values();
        List<DyeColor> list = new ArrayList<>(List.of(all));
        list.sort((a, b) -> b.getName().length() - a.getName().length());
        return list.toArray(new DyeColor[0]);
    }

    /** Which dye colour a block belongs to by name ("light_blue_concrete" is light blue), or -1. */
    private static int colourOf(String path) {
        for (DyeColor color : COLORS_BY_LENGTH) {
            if (path.startsWith(color.getName() + "_")) {
                return color.getId();
            }
        }
        return -1;
    }

    static boolean isSpinning(BlockEntity entity) {
        Method method = SPEED.computeIfAbsent(entity.getClass(), c -> {
            try {
                return c.getMethod("getSpeed");
            } catch (ReflectiveOperationException e) {
                return NONE;
            }
        });
        if (method == NONE || method.getReturnType() != float.class) {
            return false;
        }
        try {
            return Math.abs((float) method.invoke(entity)) > 0.0F;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** Everything counted while looking at the blocks of a factory (the world and any assembled vehicles). */
    private static final class Acc {
        Map<Block, Boolean> isMachine = new HashMap<>();
        Map<Block, Integer> heritage = new HashMap<>();
        Map<Block, Integer> flags = new HashMap<>();
        Map<Block, Integer> colourCache = new HashMap<>();
        Set<Block> kinds = new HashSet<>();
        Set<Block> decorations = new HashSet<>();
        int[] colors = new int[16];
        int machines = 0;
        int active = 0;
        int roofs = 0;
        int dancheong = 0;
        int accents = 0;
        int libraries = 0;
        int lights = 0;
        int plants = 0;
        int propellers = 0;
        int levitite = 0;
        int sails = 0;
        int engines = 0;
        int assemblers = 0;
        int wheels = 0;
        int beltCount = 0;
        int thrusters = 0;
        int windmills = 0;
        int plainPropellers = 0;
        int balloons = 0;
        int suspension = 0;
        List<BlockPos> propellerSpots = new ArrayList<>();
        List<BlockPos> sailSpots = new ArrayList<>();
        List<BlockPos> engineSpots = new ArrayList<>();
        List<BlockPos> balloonSpots = new ArrayList<>();
        List<BlockPos> belts = new ArrayList<>();
        List<BlockPos> wheelSpots = new ArrayList<>();
        List<BlockPos> thrusterSpots = new ArrayList<>();
        List<BlockPos> spots = new ArrayList<>();
        final java.util.Random sampler;

        Acc(BlockPos center, long time) {
            sampler = new java.util.Random(center.asLong() ^ time);
        }

        void visit(BlockState state, BlockPos cursor) {
            Block block = state.getBlock();
            switch (heritage.computeIfAbsent(block, FactoryEvaluator::heritageKind)) {
                case 1 -> roofs++;
                case 2 -> dancheong++;
                case 3 -> accents++;
                case 4 -> libraries++;
                default -> {
                }
            }
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            String ns = id.getNamespace();
            String path = id.getPath();
            // Vehicle parts (Create Aeronautics, Simulated, Offroad).
            if ((ModCompat.AERONAUTICS.contains(ns) || ns.equals("create")) && path.contains("propeller")) {
                if (path.contains("bearing")) {
                    propellers++;
                } else {
                    plainPropellers++;
                }
                addSpot(propellerSpots, cursor, propellers + plainPropellers, sampler);
            } else if (ns.equals("create") && path.equals("windmill_bearing")) {
                windmills++;
                addSpot(propellerSpots, cursor, propellers + plainPropellers + windmills, sampler);
            } else if (path.contains("levitite")) {
                levitite++;
            } else if ((ns.equals("create") || ModCompat.AERONAUTICS.contains(ns))
                    && (path.contains("sail") || path.contains("wing"))) {
                sails++;
                addSpot(sailSpots, cursor, sails, sampler);
            } else if (path.contains("portable_engine")) {
                engines++;
                addSpot(engineSpots, cursor, engines, sampler);
            } else if (path.equals("physics_assembler")) {
                assemblers++;
            } else if (ModCompat.AERONAUTICS.contains(ns)
                    && (path.contains("envelope") || path.contains("burner") || path.contains("balloon"))) {
                balloons++;
                addSpot(balloonSpots, cursor, balloons, sampler);
            } else if (ModCompat.AERONAUTICS.contains(ns) && path.contains("suspension")) {
                suspension++;
            } else if (ModCompat.OFFROAD.contains(ns) && (path.contains("wheel") || path.contains("tire"))) {
                wheels++;
                addSpot(wheelSpots, cursor, wheels, sampler);
            } else if (isThruster(ns, path)) {
                thrusters++;
                addSpot(thrusterSpots, cursor, thrusters, sampler);
            } else if (ns.equals("create") && path.equals("belt")) {
                beltCount++;
                addSpot(belts, cursor, beltCount, sampler);
            }
            // Colour.
            int colour = colourCache.computeIfAbsent(block, b -> colourOf(path));
            if (colour >= 0) {
                colors[colour]++;
            }
            boolean machine = isMachine.computeIfAbsent(block, FactoryEvaluator::machine);
            int f = flags.computeIfAbsent(block, b -> artFlags(b, id));
            if ((f & 2) != 0) {
                lights++;
            }
            if ((f & 4) != 0) {
                plants++;
            }
            if (machine) {
                machines++;
                if (spots.size() < 12) {
                    spots.add(cursor.immutable());
                } else if (sampler.nextInt(machines) < 12) {
                    spots.set(sampler.nextInt(12), cursor.immutable());
                }
                kinds.add(block);
                if (LIT_MACHINES.contains(path) && state.hasProperty(BlockStateProperties.LIT)
                        && state.getValue(BlockStateProperties.LIT)) {
                    active++;
                }
            } else if ((f & 1) != 0) {
                decorations.add(block);
            }
        }
    }

    /** Plot position of blocks that belong to assembled vehicles, by world position (kept for live checks). */
    private static final Map<String, BlockPos> PLOT_LOOKUP = new java.util.concurrent.ConcurrentHashMap<>();

    /** The spot inside a vehicle's plot for a world position, or null for ordinary blocks. */
    public static BlockPos plotOf(ServerLevel level, BlockPos world) {
        return PLOT_LOOKUP.get(level.dimension().location() + "@" + world.asLong());
    }

    /** Where a customer should stand to look at a spot: on the ground below, for flying vehicles. */
    public static BlockPos walkTarget(ServerLevel level, BlockPos pos) {
        if (plotOf(level, pos) == null) {
            return pos;
        }
        return level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos);
    }

    public static Report evaluate(ServerLevel level, BlockPos center) {
        String key = level.dimension().location() + "@" + center.asLong();
        Report cached = CACHE.get(key);
        if (cached != null && level.getGameTime() - cached.time() < CACHE_TICKS) {
            return cached;
        }

        Acc acc = new Acc(center, level.getGameTime());
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                for (int dy = -DOWN; dy <= UP; dy++) {
                    cursor.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!state.isAir()) {
                        acc.visit(state, cursor);
                    }
                }
            }
        }
        // Vehicles assembled by Simulated / Sable live in their own plots; look at them too.
        PLOT_LOOKUP.keySet().removeIf(k -> k.startsWith(level.dimension().location() + "@"));
        int vehicleActive = SableCompat.scan(level, center, RADIUS, DOWN, UP, (state, world, plot) -> {
            PLOT_LOOKUP.put(level.dimension().location() + "@" + world.asLong(), plot);
            acc.visit(state, world);
        });
        Map<Block, Boolean> isMachine = acc.isMachine;
        Map<Block, Integer> heritage = acc.heritage;
        Map<Block, Integer> flags = acc.flags;
        Map<Block, Integer> colourCache = acc.colourCache;
        Set<Block> kinds = acc.kinds;
        Set<Block> decorations = acc.decorations;
        int[] colors = acc.colors;
        int machines = acc.machines;
        int active = acc.active;
        int roofs = acc.roofs;
        int dancheong = acc.dancheong;
        int accents = acc.accents;
        int libraries = acc.libraries;
        int lights = acc.lights;
        int plants = acc.plants;
        int propellers = acc.propellers;
        int levitite = acc.levitite;
        int sails = acc.sails;
        int engines = acc.engines;
        int assemblers = acc.assemblers;
        int wheels = acc.wheels;
        int beltCount = acc.beltCount;
        int thrusters = acc.thrusters;
        int windmills = acc.windmills;
        int plainPropellers = acc.plainPropellers;
        int balloons = acc.balloons;
        int suspension = acc.suspension;
        List<BlockPos> propellerSpots = acc.propellerSpots;
        List<BlockPos> sailSpots = acc.sailSpots;
        List<BlockPos> engineSpots = acc.engineSpots;
        List<BlockPos> balloonSpots = acc.balloonSpots;
        List<BlockPos> belts = acc.belts;
        List<BlockPos> wheelSpots = acc.wheelSpots;
        List<BlockPos> thrusterSpots = acc.thrusterSpots;
        List<BlockPos> spots = acc.spots;
        java.util.Random sampler = acc.sampler;
        active += vehicleActive;

        // Chests (showcase) and running machines (block entities).
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
                    if (isSpinning(entity)) {
                        active++;
                    }
                    if (entity instanceof BaseContainerBlockEntity container) {
                        for (int slot = 0; slot < container.getContainerSize(); slot++) {
                            ItemStack stack = container.getItem(slot);
                            if (stack.isEmpty()) {
                                continue;
                            }
                            for (Taste taste : Taste.values()) {
                                if (taste != Taste.HISTORY && taste.items().contains(stack.getItem())) {
                                    showcase.merge(taste, stack.getCount(), Integer::sum);
                                }
                            }
                        }
                    }
                }
            }
        }

        // History is about architecture, not chest items.
        int historyScore = Math.min(25, Math.min(12, roofs / 6) + Math.min(10, dancheong / 5)
                + Math.min(3, accents / 2) + Math.min(12, libraries / 4));
        showcase.put(Taste.HISTORY, historyScore * 4);

        // Vehicles: real builds count for airplane and car lovers, on top of chest items.
        int aircraft = Math.min(25, propellers * 9 + Math.min(6, (plainPropellers + windmills) * 2)
                + Math.min(8, levitite / 2) + Math.min(10, sails / 6) + Math.min(4, engines * 2)
                + Math.min(6, balloons / 3) + Math.min(6, thrusters * 2));
        int cars = Math.min(25, wheels * 5 + Math.min(6, engines * 3) + Math.min(4, suspension * 2)
                + (assemblers > 0 ? 3 : 0));
        showcase.merge(Taste.AIRPLANES, aircraft * 4, Integer::sum);
        showcase.merge(Taste.CARS, cars * 4, Integer::sum);

        // Art: variety of decorative blocks, light, greenery and heritage architecture.
        int art = Math.min(4, decorations.size() / 5) + Math.min(2, lights / 6) + Math.min(2, plants / 8)
                + Math.min(2, historyScore / 12);

        TycoonData data = TycoonData.get(level.getServer());
        int scale = Math.min(25, machines / 8);
        int variety = Math.min(10, kinds.size());
        int output = Math.min(15, data.getRevenue() * 15 / Math.max(1, data.getQuota()));
        int operation = Math.min(15, active / 3);
        Report report = new Report(machines, kinds.size(), scale, variety, output, active, operation, art, colors,
                showcase, spots, roofs, dancheong, libraries, historyScore, aircraft, cars, beltCount, wheels, thrusters, belts, wheelSpots,
                thrusterSpots, propellers + plainPropellers + windmills, sails, engines, balloons, propellerSpots,
                sailSpots, engineSpots, balloonSpots, level.getGameTime());
        CACHE.put(key, report);
        return report;
    }

    private static void addSpot(List<BlockPos> list, BlockPos pos, int seen, java.util.Random sampler) {
        if (list.size() < 8) {
            list.add(pos.immutable());
        } else if (sampler.nextInt(seen) < 8) {
            list.set(sampler.nextInt(8), pos.immutable());
        }
    }

    /** Rocket engines and thrusters (Create Propulsion and look-alikes). */
    private static boolean isThruster(String ns, String path) {
        return path.contains("thruster") || path.contains("rocket_engine") || path.contains("rocket_nozzle")
                || (ModCompat.AERONAUTICS.contains(ns) && !ns.equals("aeronautics") && !ns.equals("simulated")
                && (path.contains("engine") || path.contains("nozzle")) && !path.contains("portable"));
    }

    public static final int STOP_MACHINE = 0;
    public static final int STOP_BELT = 1;
    public static final int STOP_WHEEL = 2;
    public static final int STOP_ROCKET = 3;
    /** Propeller bearings (Create Aeronautics) and windmill bearings (Create). */
    public static final int STOP_PROPELLER = 4;
    /** Windmill sails / wings. */
    public static final int STOP_SAIL = 5;
    /** Portable engines. */
    public static final int STOP_ENGINE = 6;
    /** Hot air envelopes and burners. */
    public static final int STOP_BALLOON = 7;

    /**
     * Is the thing at this spot really working right now? Belts and wheels must be turning,
     * thrusters must be firing. Add-on blocks are asked in a few common ways because they differ.
     */
    public static boolean isLiveAt(ServerLevel level, BlockPos pos, int kind) {
        BlockPos plot = plotOf(level, pos);
        if (plot == null && !level.isLoaded(pos)) {
            return false;
        }
        // Parts of assembled vehicles are looked up inside the vehicle's own plot.
        BlockState state = plot != null ? SableCompat.stateAt(level, plot) : level.getBlockState(pos);
        BlockEntity entity = plot != null ? SableCompat.entityAt(level, plot) : level.getBlockEntity(pos);
        if (kind == STOP_ROCKET || kind == STOP_ENGINE || kind == STOP_BALLOON) {
            for (var property : state.getProperties()) {
                String name = property.getName();
                if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty
                        && (name.equals("active") || name.equals("lit") || name.equals("powered")
                        || name.equals("firing") || name.equals("running") || name.equals("enabled"))
                        && state.getValue((net.minecraft.world.level.block.state.properties.BooleanProperty) property)) {
                    return true;
                }
            }
            return entity != null && (booleanCall(entity, "isActive") || booleanCall(entity, "isFiring")
                    || booleanCall(entity, "isRunning") || booleanCall(entity, "isLit") || isSpinning(entity));
        }
        if (entity != null && isSpinning(entity)) {
            return true;
        }
        for (var property : state.getProperties()) {
            if (property instanceof net.minecraft.world.level.block.state.properties.BooleanProperty
                    && (property.getName().equals("moving") || property.getName().equals("running"))
                    && state.getValue((net.minecraft.world.level.block.state.properties.BooleanProperty) property)) {
                return true;
            }
        }
        return false;
    }

    private static boolean booleanCall(BlockEntity entity, String name) {
        try {
            Method method = entity.getClass().getMethod(name);
            return method.getReturnType() == boolean.class && (boolean) method.invoke(entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return false;
        }
    }

    /** bit 0 = decoration someone built, bit 1 = gives light, bit 2 = plant. */
    private static int artFlags(Block block, ResourceLocation id) {
        String path = id.getPath();
        int f = 0;
        boolean plant = block instanceof BushBlock || block instanceof LeavesBlock || block instanceof FlowerPotBlock
                || path.contains("vine") || path.contains("azalea") || path.contains("moss_carpet");
        if (plant) {
            f |= 4;
        }
        if (block.defaultBlockState().getLightEmission() > 0) {
            f |= 2;
        }
        boolean natural = NATURAL.contains(path) || path.endsWith("_ore") || path.contains("grass") || plant
                || path.equals("lava") || path.equals("water");
        if (!natural && !machine(block)) {
            f |= 1;
        }
        return f;
    }

    private static final Set<String> ROOF_BASES = Set.of(
            "deepslate_tile", "stone_brick", "mud_brick", "nether_brick", "red_nether_brick",
            "polished_blackstone_brick", "deepslate_brick", "brick", "cobbled_deepslate", "blackstone");
    private static final Set<String> DANCHEONG_BLOCKS = Set.of(
            "red_terracotta", "green_terracotta", "blue_terracotta", "light_blue_terracotta",
            "yellow_terracotta", "cyan_terracotta", "white_terracotta", "black_terracotta", "crimson_planks",
            "stripped_crimson_stem", "stripped_crimson_hyphae", "crimson_stairs", "crimson_slab");
    private static final Set<String> LIBRARY_BLOCKS = Set.of(
            "bookshelf", "chiseled_bookshelf", "lectern", "cartography_table", "enchanting_table");

    /** 1 = tiled roof (giwa), 2 = painted eaves (dancheong), 3 = lantern or bell, 4 = library, 0 = none. */
    private static int heritageKind(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        String path = key.getPath();
        if (path.contains("dancheong")) {
            return 2;
        }
        if (path.contains("giwa") || path.contains("hanok") || path.contains("roof_tile")) {
            return 1;
        }
        if (!key.getNamespace().equals("minecraft")) {
            return 0;
        }
        if (path.endsWith("_stairs") || path.endsWith("_slab")) {
            String base = path.substring(0, path.lastIndexOf('_'));
            if (ROOF_BASES.contains(base)) {
                return 1;
            }
        }
        if (DANCHEONG_BLOCKS.contains(path)) {
            return 2;
        }
        if (path.equals("lantern") || path.equals("soul_lantern") || path.equals("bell")) {
            return 3;
        }
        return LIBRARY_BLOCKS.contains(path) ? 4 : 0;
    }

    private static boolean machine(Block block) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(block);
        return ModCompat.isMachineNamespace(key.getNamespace())
                || ModCompat.isKitchenMachine(block, key.getNamespace(), key.getPath())
                || (key.getNamespace().equals("minecraft") && VANILLA_MACHINES.contains(key.getPath()));
    }

    /** Taste score for one taste, 0..25 before card bonuses. 100 points of showcase is the maximum. */
    public static int showcaseScore(Report report, Taste taste) {
        return Math.min(25, report.showcase().getOrDefault(taste, 0) / 4);
    }

    /** Colour score 0..10: 40 blocks of the customer's favourite colour is the maximum. */
    public static int colourScore(Report report, int colourId) {
        return Math.min(10, report.colors()[Math.floorMod(colourId, 16)] * 10 / 40);
    }

    /**
     * One customer's rating of the factory. Picky customers score lower, generous ones higher,
     * and cards can boost their favourite taste or add flat points.
     */
    public static Breakdown breakdown(Report report, Taste taste, int colourId, int pickiness, int generosity,
                                      TycoonData data, int flow, int inspect) {
        int tasteScore = (int) Math.min(22, Math.round(showcaseScore(report, taste) * 0.6D
                * (1.0D + RunStats.tasteBonus(data, taste))));
        int colour = colourScore(report, colourId);
        // Two extra parts (steady production 10, inspection 8) bring the raw sum to 118; scaled back to 100.
        double total = RunStats.ratingBonus(data) + 0.85D * (report.scale() + report.variety() + report.output()
                + report.operation() + tasteScore + report.art() + colour + flow + inspect);
        total -= pickiness / 100.0D * 15.0D;
        total += generosity / 100.0D * 10.0D;
        int clamped = (int) Math.max(0, Math.min(100, Math.round(total)));
        return new Breakdown(report.scale(), report.variety(), report.output(), report.operation(), tasteScore,
                report.art(), colour, flow, inspect, clamped);
    }

    public static int score(Report report, Taste taste, int colourId, int pickiness, int generosity, TycoonData data,
                            int flow, int inspect) {
        return breakdown(report, taste, colourId, pickiness, generosity, data, flow, inspect).total();
    }

    public static int stars(int score) {
        return Math.max(1, Math.min(5, (int) Math.round(score / 20.0D)));
    }
}
