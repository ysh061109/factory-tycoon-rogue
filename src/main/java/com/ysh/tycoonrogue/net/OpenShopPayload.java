package com.ysh.tycoonrogue.net;

import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/** Server to client: open (or refresh) the Supply Desk with the player's balance and what is on sale. */
public record OpenShopPayload(int balance, List<String> offers) implements CustomPacketPayload {
    public static final Type<OpenShopPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "open_shop"));
    public static final StreamCodec<RegistryFriendlyByteBuf, OpenShopPayload> CODEC =
            StreamCodec.ofMember(OpenShopPayload::write, OpenShopPayload::read);

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(balance);
        buf.writeVarInt(offers.size());
        for (String id : offers) {
            buf.writeUtf(id);
        }
    }

    private static OpenShopPayload read(RegistryFriendlyByteBuf buf) {
        int balance = buf.readVarInt();
        int n = buf.readVarInt();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(buf.readUtf());
        }
        return new OpenShopPayload(balance, ids);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
