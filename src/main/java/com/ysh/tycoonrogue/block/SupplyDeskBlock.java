package com.ysh.tycoonrogue.block;

import com.ysh.tycoonrogue.entity.Currency;
import com.ysh.tycoonrogue.net.OpenShopPayload;
import com.ysh.tycoonrogue.shop.ShopOffer;
import com.ysh.tycoonrogue.shop.ShopOffers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The Supply Desk: spend the money you earn on building materials, guides and holograms. */
public class SupplyDeskBlock extends Block {
    /** Which desk each player has open. The server only sells to players standing at one. */
    private static final Map<UUID, BlockPos> OPEN = new HashMap<>();

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING;

    @Override
    protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    public SupplyDeskBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING,
                net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !oldState.is(this)) {
            com.ysh.tycoonrogue.data.TycoonData.get(sl.getServer()).addDesk(level.dimension().location(), pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl && !newState.is(this)) {
            com.ysh.tycoonrogue.data.TycoonData.get(sl.getServer()).removeDesk(level.dimension().location(), pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    public static boolean isAtDesk(ServerPlayer player) {
        BlockPos pos = OPEN.get(player.getUUID());
        return pos != null && player.blockPosition().distSqr(pos) <= 14 * 14;
    }

    public static void sendShop(ServerPlayer player) {
        List<String> ids = new ArrayList<>();
        for (ShopOffer offer : ShopOffers.ALL) {
            if (ShopOffers.available(offer)) {
                ids.add(offer.id());
            }
        }
        PacketDistributor.sendToPlayer(player, new OpenShopPayload(Currency.balance(player), ids));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            OPEN.put(serverPlayer.getUUID(), pos.immutable());
            sendShop(serverPlayer);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
