package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: show (or hide) the hanok hologram at this spot, turned by quarter turns. */
public record HologramPayload(BlockPos origin, int rotation, boolean active) implements CustomPacketPayload {
    public static final Type<HologramPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "hologram"));
    public static final StreamCodec<RegistryFriendlyByteBuf, HologramPayload> CODEC =
            StreamCodec.ofMember((payload, buf) -> {
                buf.writeBlockPos(payload.origin);
                buf.writeVarInt(payload.rotation);
                buf.writeBoolean(payload.active);
            }, buf -> new HologramPayload(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
