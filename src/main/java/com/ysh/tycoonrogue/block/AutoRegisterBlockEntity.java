package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The register's cash box. Customers' payments go in here as coin or emerald stacks.
 * Other mods can empty it from any side (hoppers, Create funnels and belts, pipes...) through the
 * normal item-handler capability; nothing can be inserted from outside.
 */
public class AutoRegisterBlockEntity extends BlockEntity {
    public static final int SLOTS = 27;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null) {
                level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
            }
        }
    };
    private final IItemHandler output = new IItemHandler() {
        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    };

    public AutoRegisterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_REGISTER.get(), pos, state);
    }

    /** What automation sees: take-only. */
    public IItemHandler output() {
        return output;
    }

    /** Puts a payment in the cash box; whatever does not fit is returned. */
    public ItemStack deposit(ItemStack stack) {
        ItemStack left = stack;
        for (int i = 0; i < SLOTS && !left.isEmpty(); i++) {
            left = inventory.insertItem(i, left, false);
        }
        return left;
    }

    /** Hands out everything inside (for the player and for breaking the block). */
    public java.util.List<ItemStack> takeAll() {
        java.util.List<ItemStack> out = new java.util.ArrayList<>();
        for (int i = 0; i < SLOTS; i++) {
            ItemStack s = inventory.extractItem(i, inventory.getSlotLimit(i), false);
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }

    /** 0..15 for comparators: how full the cash box is. */
    public int signal() {
        int used = 0;
        for (int i = 0; i < SLOTS; i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                used++;
            }
        }
        return used == 0 ? 0 : 1 + (used - 1) * 14 / (SLOTS - 1);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Cash", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Cash")) {
            inventory.deserializeNBT(registries, tag.getCompound("Cash"));
        }
    }
}
