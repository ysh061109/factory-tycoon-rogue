package com.ysh.tycoonrogue.shop;

import com.ysh.tycoonrogue.entity.ModCompat;
import com.ysh.tycoonrogue.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Everything the Supply Desk sells. Money earned from customers is spent here. */
public final class ShopOffers {
    public static final List<ShopOffer> ALL = List.of(
            new ShopOffer("hologram_guide", 12, () -> new ItemStack(ModItems.HOLOGRAM_GUIDE.get()),
                    () -> List.of(new ItemStack(ModItems.HOLOGRAM_GUIDE.get())), false),
            new ShopOffer("giwa_kit", 24, () -> new ItemStack(Blocks.DEEPSLATE_TILE_STAIRS),
                    () -> List.of(new ItemStack(Blocks.DEEPSLATE_TILE_STAIRS, 64),
                            new ItemStack(Blocks.DEEPSLATE_TILE_SLAB, 48)), false),
            new ShopOffer("dancheong_kit", 30, () -> new ItemStack(Blocks.RED_TERRACOTTA),
                    () -> List.of(new ItemStack(Blocks.RED_TERRACOTTA, 24), new ItemStack(Blocks.GREEN_TERRACOTTA, 24),
                            new ItemStack(Blocks.BLUE_TERRACOTTA, 24), new ItemStack(Blocks.YELLOW_TERRACOTTA, 24),
                            new ItemStack(Blocks.STRIPPED_CRIMSON_STEM, 32)), false),
            new ShopOffer("pavilion_kit", 70, () -> new ItemStack(Blocks.STONE_BRICKS),
                    ShopOffers::pavilionMaterials, false),
            new ShopOffer("library_kit", 28, () -> new ItemStack(Blocks.BOOKSHELF),
                    () -> List.of(new ItemStack(Blocks.BOOKSHELF, 16), new ItemStack(Blocks.LECTERN, 2),
                            new ItemStack(Blocks.CARTOGRAPHY_TABLE, 2)), false),
            new ShopOffer("lantern_set", 8, () -> new ItemStack(Blocks.LANTERN),
                    () -> List.of(new ItemStack(Blocks.LANTERN, 8), new ItemStack(Items.BELL, 1)), false),
            new ShopOffer("giwa_guide", 6, () -> new ItemStack(Items.WRITTEN_BOOK),
                    () -> List.of(guide("Giwa Guide / 기와 가이드", "giwa", 4)), false),
            new ShopOffer("dancheong_guide", 6, () -> new ItemStack(Items.WRITTEN_BOOK),
                    () -> List.of(guide("Dancheong Guide / 단청 가이드", "dancheong", 4)), false),
            new ShopOffer("flight_guide", 10, () -> new ItemStack(Items.WRITTEN_BOOK),
                    () -> List.of(guide("Flight Guide / 비행 가이드", "flight", 5)), true),
            new ShopOffer("aero_kit", 80, () -> new ItemStack(Items.ELYTRA),
                    () -> modStacks("aeronautics:propeller_bearing:1", "simulated:physics_assembler:1",
                            "simulated:linked_typewriter:1", "simulated:red_portable_engine:3",
                            "simulated:analog_transmission:1", "simulated:directional_gearshift:2",
                            "simulated:torsion_spring:2", "simulated:white_symmetric_sail:16",
                            "create:white_sail:32", "aeronautics:levitite:4", "create:mechanical_bearing:2",
                            "create:redstone_link:2", "create:shaft:8", "create:cogwheel:4",
                            "create:andesite_casing:8", "create:red_seat:1"), true),
            new ShopOffer("car_guide", 10, () -> new ItemStack(Items.WRITTEN_BOOK),
                    () -> List.of(guide("Car Guide / 자동차 가이드", "car", 5)), true),
            new ShopOffer("car_kit", 60, () -> new ItemStack(Items.MINECART),
                    () -> modStacks("offroad:wheel_mount:4", "simulated:physics_assembler:1",
                            "simulated:linked_typewriter:1", "simulated:red_portable_engine:2",
                            "simulated:analog_transmission:1", "create:linear_chassis:2",
                            "create:andesite_casing:8", "create:shaft:8", "create:cogwheel:4",
                            "create:red_seat:1", "create:redstone_link:2"), true));

    private ShopOffers() {
    }

    public static ShopOffer byId(String id) {
        for (ShopOffer offer : ALL) {
            if (offer.id().equals(id)) {
                return offer;
            }
        }
        return null;
    }

    public static boolean available(ShopOffer offer) {
        return !offer.needsAeronautics() || ModCompat.isAeronauticsLoaded() || !ModCompat.offroadItems().isEmpty();
    }

    private static List<ItemStack> pavilionMaterials() {
        List<ItemStack> stacks = new ArrayList<>();
        for (Map.Entry<Block, Integer> entry : Hanok.materials().entrySet()) {
            int left = entry.getValue();
            while (left > 0) {
                int n = Math.min(64, left);
                stacks.add(new ItemStack(entry.getKey(), n));
                left -= n;
            }
        }
        return stacks;
    }

    /** Parts from other mods, written as "namespace:item:count". Missing items are skipped. */
    private static List<ItemStack> modStacks(String... entries) {
        List<ItemStack> stacks = new ArrayList<>();
        for (String entry : entries) {
            int cut = entry.lastIndexOf(':');
            ResourceLocation id = ResourceLocation.tryParse(entry.substring(0, cut));
            int count = Integer.parseInt(entry.substring(cut + 1));
            if (id != null) {
                BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> {
                    if (item != Items.AIR) {
                        stacks.add(new ItemStack(item, count));
                    }
                });
            }
        }
        return stacks;
    }

    /** A written book. Pages are translation keys book.tycoonrogue.&lt;name&gt;.1..n, so text follows the reader's language. */
    private static ItemStack guide(String title, String name, int pageCount) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= pageCount; i++) {
            pages.add(Filterable.passThrough(Component.translatable("book.tycoonrogue." + name + "." + i)));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(title), "Tycoon Rogue", 0, pages, true));
        return book;
    }
}
