package com.ysh.tycoonrogue.event;

import com.ysh.tycoonrogue.TycoonRogue;
import com.ysh.tycoonrogue.data.TycoonData;
import com.ysh.tycoonrogue.entity.CustomerEntity;
import com.ysh.tycoonrogue.entity.CustomerRole;
import com.ysh.tycoonrogue.entity.Dialogue;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;

/**
 * Customers panic when a player points a gun at them. The owner loses reputation, and customers
 * who saw it spread a rumor that keeps visitors away for a while.
 *
 * "Gun" means: a TACZ gun, any Superb Warfare gun, a loaded crossbow, or any item in the
 * tycoonrogue:guns item tag (datapacks can add more).
 */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class ThreatHandler {
    public static final TagKey<Item> GUNS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(TycoonRogue.MODID, "guns"));

    private static final double RANGE = 24.0D;
    /** cos(13 degrees): how closely the player must look at a customer for it to count as aiming. */
    private static final double AIM_COS = 0.974D;
    private static final double WITNESS_RANGE = 20.0D;

    private ThreatHandler() {
    }

    public static boolean isGun(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.is(GUNS)) {
            return true;
        }
        if (stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack)) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        String namespace = key.getNamespace();
        if (namespace.equals("tacz")) {
            return key.getPath().equals("modern_kinetic_gun");
        }
        if (namespace.equals("superbwarfare")) {
            // Superb Warfare keeps its guns in an ".item.gun" package.
            return stack.getItem().getClass().getName().contains(".gun.");
        }
        return false;
    }

    private static boolean isArmed(ServerPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            if (isGun(player.getItemInHand(hand))) {
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 5 != 0
                || player.isSpectator() || !isArmed(player)) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        List<CustomerEntity> customers = level.getEntitiesOfClass(
                CustomerEntity.class, player.getBoundingBox().inflate(RANGE));
        for (CustomerEntity customer : customers) {
            if (customer.getRole() == CustomerRole.RAIDER || customer.isScared()) {
                continue;
            }
            Vec3 toCustomer = customer.getEyePosition().subtract(eye);
            double distance = toCustomer.length();
            if (distance > RANGE || distance < 0.5D || toCustomer.normalize().dot(look) < AIM_COS
                    || !player.hasLineOfSight(customer)) {
                continue;
            }
            if (customer.scare()) {
                onCustomerScared(level, player, customer);
            }
        }
    }

    private static void onCustomerScared(ServerLevel level, ServerPlayer player, CustomerEntity victim) {
        TycoonData data = TycoonData.get(level.getServer());
        // Timid customers are hurt more by this, and they tell more people.
        int penalty = 1 + victim.getTimidity() / 50;
        data.changeReputation(-penalty, player, "reason.tycoonrogue.gun_threat");
        data.addRumor(4 + victim.getGossip() / 20);
        com.ysh.tycoonrogue.data.Feed.post(level.getServer(), victim, "gun", 1);
        com.ysh.tycoonrogue.data.Feed.news(level.getServer(), "gun");

        // Everyone nearby who saw it gossips a moment later.
        List<CustomerEntity> witnesses = level.getEntitiesOfClass(
                CustomerEntity.class, victim.getBoundingBox().inflate(WITNESS_RANGE),
                other -> other != victim && other.getRole() != CustomerRole.RAIDER && !other.isScared());
        for (CustomerEntity witness : witnesses) {
            if (witness.getRandom().nextInt(100) < 30 + witness.getGossip() / 2) {
                witness.sayLater(Dialogue.Situation.GOSSIP, 40 + witness.getRandom().nextInt(80));
                data.addRumor(2 + witness.getGossip() / 25);
            }
        }
    }

    /** Bad word of mouth fades a little every 30 seconds. */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 600 == 0) {
            TycoonData.get(event.getServer()).decayRumor();
            TycoonData.get(event.getServer()).decayBuzz();
        }
    }
}
