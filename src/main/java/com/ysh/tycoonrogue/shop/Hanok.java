package com.ysh.tycoonrogue.shop;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The hanok pavilion that the hologram guide shows: a stone platform, red pillars, painted
 * (dancheong) beams and a tiled (giwa) roof. Coordinates are relative to the centre of the
 * platform's bottom layer. Building it earns heritage points from history-loving customers.
 */
public final class Hanok {
    public record Part(int dx, int dy, int dz, Block block) {
    }

    private static List<Part> parts;

    private Hanok() {
    }

    public static List<Part> parts() {
        if (parts == null) {
            List<Part> p = new ArrayList<>();
            // Platform.
            for (int x = -4; x <= 4; x++) {
                for (int z = -3; z <= 3; z++) {
                    p.add(new Part(x, 0, z, Blocks.STONE_BRICKS));
                }
            }
            // Pillars.
            for (int x : new int[]{-3, 0, 3}) {
                for (int z : new int[]{-2, 2}) {
                    for (int y = 1; y <= 3; y++) {
                        p.add(new Part(x, y, z, Blocks.STRIPPED_CRIMSON_STEM));
                    }
                }
            }
            // Dancheong beam ring.
            for (int x = -3; x <= 3; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) != 3 && Math.abs(z) != 2) {
                        continue;
                    }
                    boolean corner = Math.abs(x) == 3 && Math.abs(z) == 2;
                    Block block = corner ? Blocks.RED_TERRACOTTA
                            : (x == 0 && Math.abs(z) == 2) ? Blocks.YELLOW_TERRACOTTA
                            : Math.abs(z) == 2 ? Blocks.BLUE_TERRACOTTA : Blocks.GREEN_TERRACOTTA;
                    p.add(new Part(x, 4, z, block));
                }
            }
            // Roof: wide eave ring, two stair rings, a slab cap and a ridge.
            for (int x = -5; x <= 5; x++) {
                for (int z = -4; z <= 4; z++) {
                    if (Math.abs(x) == 5 || Math.abs(z) == 4) {
                        p.add(new Part(x, 5, z, Blocks.DEEPSLATE_TILE_SLAB));
                    }
                }
            }
            for (int x = -4; x <= 4; x++) {
                for (int z = -3; z <= 3; z++) {
                    if (Math.abs(x) == 4 || Math.abs(z) == 3) {
                        p.add(new Part(x, 6, z, Blocks.DEEPSLATE_TILE_STAIRS));
                    }
                }
            }
            for (int x = -3; x <= 3; x++) {
                for (int z = -2; z <= 2; z++) {
                    p.add(new Part(x, 7, z, Math.abs(x) == 3 || Math.abs(z) == 2
                            ? Blocks.DEEPSLATE_TILE_STAIRS : Blocks.DEEPSLATE_TILE_SLAB));
                }
            }
            for (int x = -2; x <= 2; x++) {
                p.add(new Part(x, 8, 0, Blocks.DEEPSLATE_TILE_SLAB));
            }
            parts = List.copyOf(p);
        }
        return parts;
    }

    /** How many of each block the pavilion needs. */
    public static Map<Block, Integer> materials() {
        Map<Block, Integer> map = new LinkedHashMap<>();
        for (Part part : parts()) {
            map.merge(part.block(), 1, Integer::sum);
        }
        return map;
    }

    /** Rotates a local offset by quarter turns. */
    public static int[] rotate(int dx, int dz, int quarterTurns) {
        int x = dx;
        int z = dz;
        for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
            int nx = -z;
            z = x;
            x = nx;
        }
        return new int[]{x, z};
    }
}
