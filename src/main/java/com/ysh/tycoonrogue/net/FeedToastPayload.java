package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.FeedEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Server to client: a new post or headline arrived. Shown as a phone notification banner. */
public record FeedToastPayload(FeedEntry entry) implements CustomPacketPayload {
    public static final Type<FeedToastPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "feed_toast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FeedToastPayload> CODEC =
            StreamCodec.ofMember((payload, buf) -> payload.entry.write(buf),
                    buf -> new FeedToastPayload(FeedEntry.read(buf)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
