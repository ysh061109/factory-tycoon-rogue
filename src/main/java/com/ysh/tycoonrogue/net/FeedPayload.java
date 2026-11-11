package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.FeedEntry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the SNS and news feed, sent when a player opens the phone. */
public record FeedPayload(List<FeedEntry> entries, int reputation, int rumor, String trendJob,
                          double averageRating) implements CustomPacketPayload {
    public static final Type<FeedPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "feed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FeedPayload> CODEC =
            StreamCodec.ofMember(FeedPayload::write, FeedPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(entries.size());
        for (FeedEntry entry : entries) {
            entry.write(buf);
        }
        buf.writeVarInt(reputation);
        buf.writeVarInt(rumor);
        buf.writeUtf(trendJob);
        buf.writeDouble(averageRating);
    }

    private static FeedPayload read(RegistryFriendlyByteBuf buf) {
        int count = buf.readVarInt();
        List<FeedEntry> entries = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            entries.add(FeedEntry.read(buf));
        }
        return new FeedPayload(entries, buf.readVarInt(), buf.readVarInt(), buf.readUtf(), buf.readDouble());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
