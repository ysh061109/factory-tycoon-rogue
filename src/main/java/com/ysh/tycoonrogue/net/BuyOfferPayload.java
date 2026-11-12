package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player wants to buy this offer. */
public record BuyOfferPayload(String offer) implements CustomPacketPayload {
    public static final Type<BuyOfferPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "buy_offer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BuyOfferPayload> CODEC =
            StreamCodec.ofMember((payload, buf) -> buf.writeUtf(payload.offer),
                    buf -> new BuyOfferPayload(buf.readUtf()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
