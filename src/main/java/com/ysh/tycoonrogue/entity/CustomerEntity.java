package com.ysh.tycoonrogue.entity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer who visits the player's factory. Looks like a villager but is its own entity.
 * Ordinary customers order goods for their job. Robbers steal emeralds and run away.
 * Scammers take your goods and never pay.
 */
public class CustomerEntity extends PathfinderMob {
    /** Villager biome skins (vanilla textures). */
    public static final String[] BIOMES = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};

    /** How long a visitor stays before leaving on its own (ticks). */
    private static final int VISITOR_LIFETIME = 12000;
    private static final int FLEE_TICKS = 400;

    private static final EntityDataAccessor<Integer> DATA_BIOME =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_JOB =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASTE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);

    // Personality, each 0..100.
    private int pickiness;
    private int generosity;
    private int timidity;
    private int talkativeness;
    private int gossip;

    private CustomerOrder order;
    private final List<ItemStack> loot = new ArrayList<>();
    private int fleeTicks;
    /** True for customers created by the visitor spawner. They leave after a while. */
    private boolean visitor;
    private boolean initialized;

    public CustomerEntity(EntityType<? extends CustomerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 32.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CustomerFleeGoal(this));
        this.goalSelector.addGoal(2, new RobPlayerGoal(this));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BIOME, 0);
        builder.define(DATA_JOB, CustomerJob.FARMER.ordinal());
        builder.define(DATA_TASTE, 0);
    }

    /** Rolls looks, personality and (for ordinary customers and scammers) an order. */
    public void initialize(CustomerJob job, boolean asVisitor) {
        var rnd = this.getRandom();
        this.initialized = true;
        this.visitor = asVisitor;
        this.entityData.set(DATA_BIOME, rnd.nextInt(BIOMES.length));
        this.entityData.set(DATA_JOB, job.ordinal());
        Taste taste = job.fixedTaste() != null
                ? job.fixedTaste()
                : Taste.byId(rnd.nextInt(Taste.values().length));
        this.entityData.set(DATA_TASTE, taste.ordinal());
        this.pickiness = rnd.nextInt(101);
        this.generosity = rnd.nextInt(101);
        this.timidity = rnd.nextInt(101);
        this.talkativeness = rnd.nextInt(101);
        this.gossip = rnd.nextInt(101);

        if (job.role() != CustomerJob.Role.ROBBER) {
            List<net.minecraft.world.item.Item> items = job.availableItems();
            if (!items.isEmpty()) {
                var item = items.get(rnd.nextInt(items.size()));
                int count = 8 * (1 + rnd.nextInt(8)); // 8, 16, ... 64
                double price = job.unitPrice();
                // A scammer offers a suspiciously good price. That is the tell.
                if (job.role() == CustomerJob.Role.SCAMMER) {
                    price *= 2.5D;
                }
                this.order = new CustomerOrder(item, count, price);
            }
        }
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        // Spawn eggs and /summon create the entity without calling initialize().
        if (!this.level().isClientSide && !initialized) {
            initialize(CustomerJob.randomNormal(this.getRandom()), false);
        }
    }

    public CustomerJob getJob() {
        return CustomerJob.byIndex(this.entityData.get(DATA_JOB));
    }

    public int getBiome() {
        return this.entityData.get(DATA_BIOME);
    }

    public Taste getTaste() {
        return Taste.byId(this.entityData.get(DATA_TASTE));
    }

    public int getTimidity() {
        return timidity;
    }

    public int getGossip() {
        return gossip;
    }

    public int getGenerosity() {
        return generosity;
    }

    public CustomerOrder getOrder() {
        return order;
    }

    public boolean isFleeing() {
        return fleeTicks > 0;
    }

    public void startFleeing() {
        this.fleeTicks = FLEE_TICKS;
    }

    public List<ItemStack> getLoot() {
        return loot;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (fleeTicks > 0) {
            fleeTicks--;
            if (fleeTicks == 0 && getJob().role() != CustomerJob.Role.NORMAL) {
                // The thief got away with it.
                this.discard();
                return;
            }
        }
        if (visitor && this.tickCount > VISITOR_LIFETIME && fleeTicks == 0) {
            this.discard();
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        CustomerJob job = getJob();
        if (job.role() == CustomerJob.Role.ROBBER) {
            player.displayClientMessage(Component.translatable("message.tycoonrogue.robber.talk"), true);
            return InteractionResult.CONSUME;
        }
        if (order == null) {
            player.displayClientMessage(Component.translatable("message.tycoonrogue.no_order"), true);
            return InteractionResult.CONSUME;
        }
        if (isFleeing()) {
            return InteractionResult.CONSUME;
        }

        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.is(order.item())) {
            // Show the order (with the job name) when the player is not holding the wanted item.
            player.displayClientMessage(Component.translatable(
                    "message.tycoonrogue.order",
                    Component.translatable(job.translationKey()),
                    order.item().getDescription(),
                    order.remaining(),
                    (int) Math.ceil(order.remaining() * order.unitPrice())), true);
            return InteractionResult.CONSUME;
        }

        int amount = Math.min(held.getCount(), order.remaining());
        ItemStack handedOver = held.split(amount);
        order.addDelivered(amount);

        if (job.role() == CustomerJob.Role.SCAMMER) {
            // Takes the goods, never pays.
            loot.add(handedOver);
            if (order.isComplete()) {
                player.displayClientMessage(Component.translatable("message.tycoonrogue.scammer.escape"), false);
                startFleeing();
            }
            return InteractionResult.CONSUME;
        }

        // Generosity gives up to +30% on the price.
        double multiplier = 1.0D + generosity / 100.0D * 0.3D;
        int emeralds = order.takeOwedPayment(multiplier);
        giveEmeralds(player, emeralds);
        player.displayClientMessage(Component.translatable(
                order.isComplete() ? "message.tycoonrogue.order.done" : "message.tycoonrogue.order.partial",
                emeralds, order.remaining()), true);
        if (order.isComplete()) {
            // The customer leaves happy. For now they simply walk away and despawn later.
            this.visitor = true;
            this.tickCount = Math.max(this.tickCount, VISITOR_LIFETIME - 600);
        }
        return InteractionResult.CONSUME;
    }

    private static void giveEmeralds(Player player, int amount) {
        int left = amount;
        while (left > 0) {
            int batch = Math.min(64, left);
            ItemStack stack = new ItemStack(Items.EMERALD, batch);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            left -= batch;
        }
    }

    /** Whatever a thief carries drops when they are killed. */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide) {
            for (ItemStack stack : loot) {
                this.spawnAtLocation(stack);
            }
            loot.clear();
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Initialized", initialized);
        tag.putBoolean("Visitor", visitor);
        tag.putInt("Biome", getBiome());
        tag.putInt("Job", getJob().ordinal());
        tag.putInt("Taste", this.entityData.get(DATA_TASTE));
        tag.putInt("Pickiness", pickiness);
        tag.putInt("Generosity", generosity);
        tag.putInt("Timidity", timidity);
        tag.putInt("Talkativeness", talkativeness);
        tag.putInt("Gossip", gossip);
        tag.putInt("FleeTicks", fleeTicks);
        if (order != null) {
            tag.put("Order", order.save());
        }
        ListTag lootTag = new ListTag();
        for (ItemStack stack : loot) {
            if (!stack.isEmpty()) {
                lootTag.add(stack.save(this.registryAccess()));
            }
        }
        tag.put("Loot", lootTag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.initialized = tag.getBoolean("Initialized");
        this.visitor = tag.getBoolean("Visitor");
        this.entityData.set(DATA_BIOME, tag.getInt("Biome"));
        this.entityData.set(DATA_JOB, tag.getInt("Job"));
        this.entityData.set(DATA_TASTE, tag.getInt("Taste"));
        this.pickiness = tag.getInt("Pickiness");
        this.generosity = tag.getInt("Generosity");
        this.timidity = tag.getInt("Timidity");
        this.talkativeness = tag.getInt("Talkativeness");
        this.gossip = tag.getInt("Gossip");
        this.fleeTicks = tag.getInt("FleeTicks");
        this.order = tag.contains("Order") ? CustomerOrder.load(tag.getCompound("Order")) : null;
        this.loot.clear();
        for (Tag entry : tag.getList("Loot", Tag.TAG_COMPOUND)) {
            ItemStack.parse(this.registryAccess(), entry).ifPresent(loot::add);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.VILLAGER_DEATH;
    }
}
