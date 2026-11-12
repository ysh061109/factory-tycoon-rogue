package com.ysh.tycoonrogue.item;

import com.ysh.tycoonrogue.net.HologramPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Projects a hologram of a hanok pavilion. Right-click a block to place it (it faces the way you
 * look). Boxes show where blocks are still missing; red boxes mark the wrong block. Sneak and
 * right-click to switch it off.
 */
public class HologramGuideItem extends Item {
    public HologramGuideItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer) {
            if (player.isShiftKeyDown()) {
                PacketDistributor.sendToPlayer(serverPlayer, new HologramPayload(context.getClickedPos(), 0, false));
                player.displayClientMessage(Component.translatable("message.tycoonrogue.hologram.off"), true);
            } else {
                int rotation = context.getHorizontalDirection().get2DDataValue();
                PacketDistributor.sendToPlayer(serverPlayer, new HologramPayload(
                        context.getClickedPos().relative(context.getClickedFace()), rotation, true));
                player.displayClientMessage(Component.translatable("message.tycoonrogue.hologram.on"), true);
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() && player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new HologramPayload(player.blockPosition(), 0, false));
            player.displayClientMessage(Component.translatable("message.tycoonrogue.hologram.off"), true);
        }
        return InteractionResultHolder.pass(player.getItemInHand(hand));
    }
}
