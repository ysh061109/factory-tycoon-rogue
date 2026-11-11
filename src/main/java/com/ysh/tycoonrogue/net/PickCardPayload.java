package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Client to server: the player chose this card. */
public record PickCardPayload(ResourceLocation card) implements CustomPacketPayload {
    public static final Type<PickCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "pick_card"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PickCardPayload> CODEC =
            StreamCodec.ofMember(PickCardPayload::write, PickCardPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeResourceLocation(card);
    }

    private static PickCardPayload read(RegistryFriendlyByteBuf buf) {
        return new PickCardPayload(buf.readResourceLocation());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
