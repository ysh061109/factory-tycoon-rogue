package com.ysh.tycoonrogue.item;

import com.ysh.tycoonrogue.data.Feed;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.net.FeedPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;

/** The phone: right-click to read SNS posts and news about your factory. */
public class PhoneItem extends Item {
    public PhoneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, net.minecraft.world.InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && level.getServer() != null) {
            TycoonData data = TycoonData.get(level.getServer());
            PacketDistributor.sendToPlayer(serverPlayer, new FeedPayload(new ArrayList<>(data.getFeed()),
                    data.getReputation(), data.getRumor(), data.getTrendJob(), Feed.averageRating(data)));
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
