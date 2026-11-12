package com.ysh.tycoonrogue.shop;

import com.ysh.tycoonrogue.entity.ModCompat;
import com.ysh.tycoonrogue.registry.ModItems;
import net.minecraft.core.component.DataComponents;
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
            new ShopOffer("flight_guide", 10, () -> new ItemStack(Items.WRITTEN_BOOK),
                    () -> List.of(flightGuide()), true));

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
        return !offer.needsAeronautics() || ModCompat.isAeronauticsLoaded();
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

    /** A written book that explains how to build a flying machine. Text follows the reader's language. */
    private static ItemStack flightGuide() {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        List<Filterable<Component>> pages = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            pages.add(Filterable.passThrough(Component.translatable("book.tycoonrogue.flight." + i)));
        }
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough("Flight Guide / 비행 가이드"), "Tycoon Rogue", 0, pages, true));
        return book;
    }
}
