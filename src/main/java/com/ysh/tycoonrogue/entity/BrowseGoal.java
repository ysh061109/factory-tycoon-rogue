package com.ysh.tycoonrogue.entity;

import com.ysh.tycoonrogue.block.DisplayStandBlockEntity;
import com.ysh.tycoonrogue.data.Feed;
import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * Customers look at the items on display stands. A customer who likes one takes it and either
 * pays at an auto register or asks the player to take the money. A disguised robber takes it and runs.
 * Phases: 0 walk to the stand, 1 look at it, 2 walk to checkout, 3 wait for payment.
 */
public class BrowseGoal extends Goal {
    private static final double SHOP_RANGE = 64.0D;
    private static final double STAND_RANGE = 30.0D;

    private final CustomerEntity customer;
    private BlockPos stand;
    private BlockPos register;
    private int phase;
    private int timer;
    private int total;

    public BrowseGoal(CustomerEntity customer) {
        this.customer = customer;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    /** Stands that currently show an item, nearest to the shop first. */
    private static List<BlockPos> stocked(CustomerEntity customer) {
        List<BlockPos> out = new ArrayList<>();
        if (!(customer.level() instanceof ServerLevel level) || level.getServer() == null) {
            return out;
        }
        TycoonData data = TycoonData.get(level.getServer());
        BlockPos shop = data.nearestShop(level.dimension().location(), customer.blockPosition(), SHOP_RANGE);
        if (shop == null) {
            return out;
        }
        for (BlockPos pos : data.listKind("stand", level.dimension().location(), shop, STAND_RANGE)) {
            if (level.getBlockEntity(pos) instanceof DisplayStandBlockEntity be && !be.getShown().isEmpty()) {
                out.add(pos);
            }
        }
        return out;
    }

    /** True when there is something on display this customer could still look at. */
    public static boolean hasStandsFor(CustomerEntity customer) {
        return customer.tickCount < 1200 && !stocked(customer).isEmpty();
    }

    @Override
    public boolean canUse() {
        if (customer.hasBrowsed() || customer.isQueued() || customer.isFleeing() || customer.isScared()
                || !customer.getLoot().isEmpty() || customer.getPurchasePrice() > 0) {
            return false;
        }
        CustomerRole role = customer.getRole();
        if (role != CustomerRole.NORMAL && role != CustomerRole.ROBBER) {
            return false;
        }
        CustomerOrder order = customer.getOrder();
        if (role == CustomerRole.NORMAL && order != null && !order.isComplete()) {
            return false;
        }
        if (customer.tickCount % 20 != 0) {
            return false;
        }
        List<BlockPos> stands = stocked(customer);
        if (stands.isEmpty()) {
            return false;
        }
        // Not everybody wants to buy something: normal customers do 75% of the time, thieves always.
        if (role == CustomerRole.NORMAL && customer.getRandom().nextInt(100) >= 75) {
            customer.setBrowsed();
            return false;
        }
        stand = stands.get(customer.getRandom().nextInt(stands.size()));
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        return !customer.hasBrowsed() && !customer.isFleeing() && !customer.isScared() && total < 2400;
    }

    @Override
    public void start() {
        phase = 0;
        timer = 0;
        total = 0;
        register = null;
        customer.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 0.9D);
    }

    @Override
    public void stop() {
        customer.getNavigation().stop();
        if (phase >= 2 && customer.getPurchasePrice() > 0) {
            giveUp();
        }
    }

    @Override
    public void tick() {
        total++;
        if (!(customer.level() instanceof ServerLevel level) || level.getServer() == null) {
            return;
        }
        switch (phase) {
            case 0 -> {
                if (!standHasItem(level)) {
                    customer.setBrowsed();
                    return;
                }
                if (customer.distanceToSqr(stand.getX() + 0.5D, stand.getY() + 0.5D, stand.getZ() + 0.5D) > 5.0D) {
                    if (customer.getNavigation().isDone()) {
                        customer.getNavigation().moveTo(stand.getX() + 0.5D, stand.getY(), stand.getZ() + 0.5D, 0.9D);
                    }
                    return;
                }
                customer.getNavigation().stop();
                phase = 1;
                timer = 0;
                customer.say(Dialogue.Situation.BROWSE);
            }
            case 1 -> {
                customer.getLookControl().setLookAt(stand.getX() + 0.5D, stand.getY() + 0.8D, stand.getZ() + 0.5D);
                if (++timer < 50) {
                    return;
                }
                if (!standHasItem(level)) {
                    customer.setBrowsed();
                    return;
                }
                takeItem(level);
            }
            case 2 -> walkToCheckout(level);
            case 3 -> waitForPayment(level);
            default -> {
            }
        }
    }

    private boolean standHasItem(ServerLevel level) {
        return level.getBlockEntity(stand) instanceof DisplayStandBlockEntity be && !be.getShown().isEmpty();
    }

    private void takeItem(ServerLevel level) {
        DisplayStandBlockEntity be = (DisplayStandBlockEntity) level.getBlockEntity(stand);
        ItemStack item = be.getShown();
        be.setShown(ItemStack.EMPTY);
        level.playSound(null, stand, SoundEvents.ITEM_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.0F);
        if (customer.getRole() == CustomerRole.ROBBER) {
            // Shoplifting: grab it and run.
            customer.getLoot().add(item);
            customer.reveal();
            Player near = level.getNearestPlayer(customer, 48.0D);
            if (near instanceof ServerPlayer player) {
                player.displayClientMessage(Component.translatable("message.tycoonrogue.shoplift",
                        item.getHoverName()), false);
                TycoonData.get(level.getServer()).changeReputation(-2, player, "reason.tycoonrogue.shoplifted");
            }
            Feed.news(level.getServer(), "shoplift");
            customer.setBrowsed();
            customer.startFleeing();
            return;
        }
        customer.setPurchase(item, priceOf(level, item));
        customer.say(Dialogue.Situation.BROWSE_BUY);
        // An auto register, if there is one, takes the payment.
        TycoonData data = TycoonData.get(level.getServer());
        List<BlockPos> registers = data.listKind("register", level.dimension().location(), stand, STAND_RANGE + 10);
        register = registers.isEmpty() ? null : registers.get(0);
        phase = 2;
        timer = 0;
        if (register != null) {
            customer.getNavigation().moveTo(register.getX() + 0.5D, register.getY(), register.getZ() + 0.5D, 0.95D);
        }
    }

    private int priceOf(ServerLevel level, ItemStack stack) {
        Rarity rarity = stack.getRarity();
        int base = switch (rarity) {
            case COMMON -> 3;
            case UNCOMMON -> 6;
            case RARE -> 12;
            case EPIC -> 24;
        };
        if (stack.isEnchanted()) {
            base += 4;
        }
        TycoonData data = TycoonData.get(level.getServer());
        double reputationFactor = 1.0D + (data.getReputation() - 50) / 250.0D;
        double multiplier = (1.0D + customer.getGenerosity() / 100.0D * 0.3D)
                * RunStats.rewardMultiplier(data, customer.getJob()) * reputationFactor;
        return Math.max(1, (int) Math.ceil(base * stack.getCount() * multiplier));
    }

    private void walkToCheckout(ServerLevel level) {
        if (register != null) {
            if (customer.distanceToSqr(register.getX() + 0.5D, register.getY() + 0.5D, register.getZ() + 0.5D) > 6.0D) {
                if (customer.getNavigation().isDone() || ++timer % 40 == 0) {
                    customer.getNavigation().moveTo(register.getX() + 0.5D, register.getY(), register.getZ() + 0.5D, 0.95D);
                }
                if (timer > 600) {
                    register = null;
                }
                return;
            }
            // The auto register pays out right away.
            customer.getNavigation().stop();
            Player owner = level.getNearestPlayer(customer, 80.0D);
            int price = customer.getPurchasePrice();
            TycoonData data = TycoonData.get(level.getServer());
            data.addRevenue(price);
            if (owner != null) {
                com.ysh.tycoonrogue.entity.Currency.pay(owner, price);
                owner.displayClientMessage(Component.translatable("message.tycoonrogue.purchase.auto",
                        customer.getPurchase().getHoverName(), price), true);
                if (owner instanceof ServerPlayer sp) {
                    data.changeReputation(1, sp, "reason.tycoonrogue.sold");
                }
            } else {
                level.addFreshEntity(new ItemEntity(level, register.getX() + 0.5D, register.getY() + 1.0D,
                        register.getZ() + 0.5D, new ItemStack(net.minecraft.world.item.Items.EMERALD, price)));
            }
            level.playSound(null, register, SoundEvents.AMETHYST_BLOCK_CHIME, net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.6F);
            level.playSound(null, register, SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.BLOCKS, 0.8F, 1.1F);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, register.getX() + 0.5D, register.getY() + 1.2D,
                    register.getZ() + 0.5D, 6, 0.3D, 0.2D, 0.3D, 0.0D);
            customer.say(Dialogue.Situation.THANKS);
            customer.setPurchase(customer.getPurchase(), 0);
            customer.setBrowsed();
            return;
        }
        // No register: go to the player and ask them to take the payment.
        Player owner = level.getNearestPlayer(customer, 48.0D);
        if (owner == null) {
            giveUp();
            return;
        }
        phase = 3;
        timer = 0;
        customer.getNavigation().moveTo(owner, 0.95D);
    }

    private void waitForPayment(ServerLevel level) {
        Player owner = level.getNearestPlayer(customer, 48.0D);
        if (owner == null || customer.getPurchasePrice() <= 0) {
            if (customer.getPurchasePrice() <= 0) {
                customer.setBrowsed();
            } else {
                giveUp();
            }
            return;
        }
        double dist = customer.distanceToSqr(owner);
        if (dist > 6.0D) {
            if (customer.getNavigation().isDone()) {
                customer.getNavigation().moveTo(owner, 0.95D);
            }
            return;
        }
        customer.getNavigation().stop();
        customer.getLookControl().setLookAt(owner, 30.0F, 30.0F);
        if (timer % 100 == 0) {
            customer.say(Dialogue.Situation.CHECKOUT);
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, customer.getX(), customer.getY() + 2.2D, customer.getZ(),
                    3, 0.2D, 0.1D, 0.2D, 0.0D);
            owner.displayClientMessage(Component.translatable("message.tycoonrogue.checkout.ask",
                    customer.getPurchase().getHoverName(), customer.getPurchasePrice()), true);
        }
        if (++timer > 900) {
            giveUp();
        }
    }

    /** The customer loses patience: puts the item back and walks on. */
    private void giveUp() {
        if (customer.level() instanceof ServerLevel level && !customer.getPurchase().isEmpty()) {
            ItemStack item = customer.getPurchase().copy();
            if (stand != null && level.getBlockEntity(stand) instanceof DisplayStandBlockEntity be && be.getShown().isEmpty()) {
                be.setShown(item);
            } else {
                customer.spawnAtLocation(item);
            }
            customer.setPurchase(ItemStack.EMPTY, 0);
            customer.say(Dialogue.Situation.LEAVING);
        }
        customer.setBrowsed();
    }
}
