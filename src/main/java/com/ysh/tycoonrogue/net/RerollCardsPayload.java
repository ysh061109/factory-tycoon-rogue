package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: pay to draw new cards for the current choice. */
public record RerollCardsPayload() implements CustomPacketPayload {
    public static final Type<RerollCardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "reroll_cards"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RerollCardsPayload> CODEC =
            StreamCodec.ofMember((payload, buf) -> { }, buf -> new RerollCardsPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
