package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class DisplayStandBlockEntity extends BlockEntity {
    private ItemStack shown = ItemStack.EMPTY;

    public DisplayStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_STAND.get(), pos, state);
    }

    public ItemStack getShown() {
        return shown;
    }

    public void setShown(ItemStack stack) {
        this.shown = stack;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!shown.isEmpty()) {
            tag.put("Shown", shown.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        shown = tag.contains("Shown") ? ItemStack.parse(registries, tag.getCompound("Shown")).orElse(ItemStack.EMPTY)
                : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
