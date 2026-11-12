package com.ysh.tycoonrogue.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

/**
 * Lets the factory evaluation see vehicles that were assembled with Create Simulated (physics
 * assembler, built on the Sable physics engine). Those blocks are moved into a separate "plot" far
 * outside the world and drawn at the vehicle's position, so a normal block scan cannot see them.
 *
 * Everything goes through reflection (class and method names from the Sable source), so nothing
 * here is needed to compile or run without Sable. If a name does not match the installed version,
 * the vehicle scan quietly switches itself off and the rest of the evaluation keeps working.
 */
public final class SableCompat {
    /** Receives each block of a vehicle: its state, the position where it appears in the world, and its plot position. */
    public interface Visitor {
        void block(BlockState state, BlockPos world, BlockPos plot);
    }

    private static final int MAX_BLOCKS = 120_000;
    private static boolean failed;
    private static Method getContainer;
    private static Method getAllSubLevels;
    private static Method logicalPose;
    private static Method getPlot;
    private static Method isRemoved;
    private static Method getCenterBlock;
    private static Method getLoadedChunks;
    private static Method holderChunk;
    private static Method containerChunk;
    private static Method transform;

    private SableCompat() {
    }

    private static boolean init() {
        if (failed) {
            return false;
        }
        if (getContainer != null) {
            return true;
        }
        try {
            Class<?> container = Class.forName("dev.ryanhcode.sable.api.sublevel.SubLevelContainer");
            Class<?> sub = Class.forName("dev.ryanhcode.sable.sublevel.SubLevel");
            Class<?> plot = Class.forName("dev.ryanhcode.sable.sublevel.plot.LevelPlot");
            Class<?> holder = Class.forName("dev.ryanhcode.sable.sublevel.plot.PlotChunkHolder");
            getAllSubLevels = container.getMethod("getAllSubLevels");
            containerChunk = container.getMethod("getChunk", ChunkPos.class);
            logicalPose = sub.getMethod("logicalPose");
            getPlot = sub.getMethod("getPlot");
            isRemoved = sub.getMethod("isRemoved");
            getCenterBlock = plot.getMethod("getCenterBlock");
            getLoadedChunks = plot.getMethod("getLoadedChunks");
            holderChunk = holder.getMethod("getChunk");
            getContainer = container.getMethod("getContainer", Level.class);
            return true;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = !(e instanceof ClassNotFoundException) || true;
            getContainer = null;
            return false;
        }
    }

    private static Vector3dc toWorld(Object pose, double x, double y, double z) throws ReflectiveOperationException {
        if (transform == null) {
            for (Method m : pose.getClass().getMethods()) {
                if (m.getName().equals("transformPosition") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].isAssignableFrom(Vector3d.class)
                        && Vector3dc.class.isAssignableFrom(m.getReturnType())) {
                    transform = m;
                    break;
                }
            }
            if (transform == null) {
                throw new NoSuchMethodException("transformPosition");
            }
        }
        return (Vector3dc) transform.invoke(pose, new Vector3d(x, y, z));
    }

    /**
     * Shows every block of nearby assembled vehicles that appears inside the factory box to the visitor.
     *
     * @return how many spinning machines (kinetic block entities) the vehicles contain
     */
    public static int scan(ServerLevel level, BlockPos center, int radius, int down, int up, Visitor visitor) {
        if (!init()) {
            return 0;
        }
        int spinning = 0;
        try {
            Object container = getContainer.invoke(null, level);
            if (container == null) {
                return 0;
            }
            int budget = MAX_BLOCKS;
            for (Object sub : (List<?>) getAllSubLevels.invoke(container)) {
                if ((boolean) isRemoved.invoke(sub)) {
                    continue;
                }
                Object pose = logicalPose.invoke(sub);
                Object plot = getPlot.invoke(sub);
                BlockPos mid = (BlockPos) getCenterBlock.invoke(plot);
                Vector3dc where = toWorld(pose, mid.getX() + 0.5D, mid.getY() + 0.5D, mid.getZ() + 0.5D);
                // Cheap cull: the centre of the plot is not exactly the centre of the vehicle, so be generous.
                if (Math.abs(where.x() - center.getX()) > radius + 40 || Math.abs(where.z() - center.getZ()) > radius + 40
                        || Math.abs(where.y() - center.getY()) > up + 60) {
                    continue;
                }
                for (Object holder : (Collection<?>) getLoadedChunks.invoke(plot)) {
                    LevelChunk chunk = (LevelChunk) holderChunk.invoke(holder);
                    if (chunk == null) {
                        continue;
                    }
                    ChunkPos pos = chunk.getPos();
                    LevelChunkSection[] sections = chunk.getSections();
                    for (int i = 0; i < sections.length && budget > 0; i++) {
                        LevelChunkSection section = sections[i];
                        if (section == null || section.hasOnlyAir()) {
                            continue;
                        }
                        int baseY = (chunk.getMinSection() + i) << 4;
                        for (int y = 0; y < 16; y++) {
                            for (int z = 0; z < 16; z++) {
                                for (int x = 0; x < 16; x++) {
                                    BlockState state = section.getBlockState(x, y, z);
                                    if (state.isAir()) {
                                        continue;
                                    }
                                    budget--;
                                    BlockPos plotPos = new BlockPos(pos.getMinBlockX() + x, baseY + y, pos.getMinBlockZ() + z);
                                    Vector3dc w = toWorld(pose, plotPos.getX() + 0.5D, plotPos.getY() + 0.5D,
                                            plotPos.getZ() + 0.5D);
                                    BlockPos worldPos = BlockPos.containing(w.x(), w.y(), w.z());
                                    if (Math.abs(worldPos.getX() - center.getX()) <= radius
                                            && Math.abs(worldPos.getZ() - center.getZ()) <= radius
                                            && worldPos.getY() >= center.getY() - down
                                            && worldPos.getY() <= center.getY() + up) {
                                        visitor.block(state, worldPos, plotPos);
                                    }
                                }
                            }
                        }
                    }
                    for (BlockEntity entity : chunk.getBlockEntities().values()) {
                        if (FactoryEvaluator.isSpinning(entity)) {
                            BlockPos p = entity.getBlockPos();
                            Vector3dc w = toWorld(pose, p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D);
                            if (Math.abs(w.x() - center.getX()) <= radius && Math.abs(w.z() - center.getZ()) <= radius) {
                                spinning++;
                            }
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            return spinning;
        }
        return spinning;
    }

    /** The chunk of a plot position, or null. */
    private static LevelChunk chunkOf(Level level, BlockPos plotPos) {
        if (!init()) {
            return null;
        }
        try {
            Object container = getContainer.invoke(null, level);
            return container == null ? null : (LevelChunk) containerChunk.invoke(container, new ChunkPos(plotPos));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** Current block state at a position inside a vehicle's plot (air when unknown). */
    public static BlockState stateAt(Level level, BlockPos plotPos) {
        LevelChunk chunk = chunkOf(level, plotPos);
        return chunk == null ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : chunk.getBlockState(plotPos);
    }

    public static BlockEntity entityAt(Level level, BlockPos plotPos) {
        LevelChunk chunk = chunkOf(level, plotPos);
        return chunk == null ? null : chunk.getBlockEntity(plotPos);
    }
}
