package com.ysh.tycoonrogue.entity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/** A bulk order: the customer wants {@code count} of one item and pays emeralds for it. */
public final class CustomerOrder {
    private final Item item;
    private final int count;
    private final double unitPrice;
    private int delivered;
    private int paid;

    public CustomerOrder(Item item, int count, double unitPrice) {
        this.item = item;
        this.count = count;
        this.unitPrice = unitPrice;
    }

    public Item item() {
        return item;
    }

    public int count() {
        return count;
    }

    public int delivered() {
        return delivered;
    }

    public int remaining() {
        return count - delivered;
    }

    public boolean isComplete() {
        return delivered >= count;
    }

    public double unitPrice() {
        return unitPrice;
    }

    public int totalPrice() {
        return (int) Math.ceil(count * unitPrice);
    }

    public void addDelivered(int amount) {
        delivered = Math.min(count, delivered + amount);
    }

    /**
     * Emeralds owed right now for everything delivered so far, minus what was already paid.
     * The generosity bonus is a multiplier (1.0 = no bonus).
     */
    public int takeOwedPayment(double multiplier) {
        int owedTotal = (int) Math.floor(delivered * unitPrice * multiplier);
        int owedNow = Math.max(0, owedTotal - paid);
        paid += owedNow;
        return owedNow;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Item", BuiltInRegistries.ITEM.getKey(item).toString());
        tag.putInt("Count", count);
        tag.putDouble("UnitPrice", unitPrice);
        tag.putInt("Delivered", delivered);
        tag.putInt("Paid", paid);
        return tag;
    }

    public static CustomerOrder load(CompoundTag tag) {
        ResourceLocation key = ResourceLocation.tryParse(tag.getString("Item"));
        Item item = key == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(key).orElse(Items.AIR);
        if (item == Items.AIR) {
            return null;
        }
        CustomerOrder order = new CustomerOrder(item, tag.getInt("Count"), tag.getDouble("UnitPrice"));
        order.delivered = tag.getInt("Delivered");
        order.paid = tag.getInt("Paid");
        return order;
    }
}
