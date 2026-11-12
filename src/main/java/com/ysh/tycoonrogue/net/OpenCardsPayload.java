package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Server to client: opens the card selection screen. The rarity is sent along because card
 * definitions only exist on the server.
 */
public record OpenCardsPayload(int round, boolean success, List<ResourceLocation> cards, List<Integer> rarities,
                               int rerollsLeft)
        implements CustomPacketPayload {
    public static final Type<OpenCardsPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "open_cards"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenCardsPayload> CODEC =
            StreamCodec.ofMember(OpenCardsPayload::write, OpenCardsPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(round);
        buf.writeBoolean(success);
        buf.writeVarInt(cards.size());
        for (int i = 0; i < cards.size(); i++) {
            buf.writeResourceLocation(cards.get(i));
            buf.writeVarInt(rarities.get(i));
        }
        buf.writeVarInt(rerollsLeft);
    }

    private static OpenCardsPayload read(RegistryFriendlyByteBuf buf) {
        int round = buf.readVarInt();
        boolean success = buf.readBoolean();
        int count = buf.readVarInt();
        List<ResourceLocation> cards = new ArrayList<>();
        List<Integer> rarities = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(buf.readResourceLocation());
            rarities.add(buf.readVarInt());
        }
        return new OpenCardsPayload(round, success, cards, rarities, buf.readVarInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
