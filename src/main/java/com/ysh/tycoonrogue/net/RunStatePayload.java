package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server to client: the current state of the run, for the HUD. Sent once a second. */
public record RunStatePayload(int round, int ticksLeft, int revenue, int quota, int reputation, int rumor,
                              List<ResourceLocation> cards) implements CustomPacketPayload {
    public static final Type<RunStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "run_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RunStatePayload> CODEC =
            StreamCodec.ofMember(RunStatePayload::write, RunStatePayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(round);
        buf.writeVarInt(ticksLeft);
        buf.writeVarInt(revenue);
        buf.writeVarInt(quota);
        buf.writeVarInt(reputation);
        buf.writeVarInt(rumor);
        buf.writeVarInt(cards.size());
        for (ResourceLocation card : cards) {
            buf.writeResourceLocation(card);
        }
    }

    private static RunStatePayload read(RegistryFriendlyByteBuf buf) {
        int round = buf.readVarInt();
        int ticksLeft = buf.readVarInt();
        int revenue = buf.readVarInt();
        int quota = buf.readVarInt();
        int reputation = buf.readVarInt();
        int rumor = buf.readVarInt();
        int count = buf.readVarInt();
        List<ResourceLocation> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            cards.add(buf.readResourceLocation());
        }
        return new RunStatePayload(round, ticksLeft, revenue, quota, reputation, rumor, cards);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
