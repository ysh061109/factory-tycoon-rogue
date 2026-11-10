package com.ysh.tycoonrogue.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * A customer who visits the player's factory. Looks like a villager but is its own entity.
 *
 * Ordinary customers order goods for their job. Robbers and scammers look exactly the same
 * until they strike. Raiders are openly armed and cause much bigger losses.
 */
public class CustomerEntity extends PathfinderMob {
    /** Villager biome skins (vanilla textures). */
    public static final String[] BIOMES = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};

    /** Animation / particle state, synced to clients. */
    public static final int STATE_IDLE = 0;
    public static final int STATE_CHARGE = 1;
    public static final int STATE_RANSACK = 2;
    public static final int STATE_FLEE = 3;

    /** How long a visitor stays before leaving on its own (ticks). */
    private static final int VISITOR_LIFETIME = 12000;
    private static final int FLEE_TICKS = 500;

    private static final EntityDataAccessor<Integer> DATA_BIOME =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_JOB =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASTE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STATE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    /** 0 = still disguised, 1 = robber mask shown, 2 = scammer sunglasses shown. */
    private static final EntityDataAccessor<Integer> DATA_REVEAL =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);

    // Personality, each 0..100.
    private int pickiness;
    private int generosity;
    private int timidity;
    private int talkativeness;
    private int gossip;

    private CustomerRole role = CustomerRole.NORMAL;
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
                .add(Attributes.FOLLOW_RANGE, 48.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CustomerFleeGoal(this));
        this.goalSelector.addGoal(2, new RaiderGoal(this));
        this.goalSelector.addGoal(3, new DisguisedRobberGoal(this));
        this.goalSelector.addGoal(4, new ApproachPlayerGoal(this));
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
        builder.define(DATA_STATE, STATE_IDLE);
        builder.define(DATA_REVEAL, 0);
    }

    /**
     * Rolls looks, personality and, unless this is a raider, an order.
     * For robbers and scammers {@code job} is only the disguise.
     */
    public void initialize(CustomerJob job, CustomerRole role, boolean asVisitor) {
        var rnd = this.getRandom();
        this.initialized = true;
        this.visitor = asVisitor;
        this.role = role;
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

        if (role == CustomerRole.RAIDER) {
            equipRaider();
            return;
        }
        List<Item> items = job.availableItems();
        if (!items.isEmpty()) {
            Item item = items.get(rnd.nextInt(items.size()));
            int count = 8 * (1 + rnd.nextInt(8)); // 8, 16, ... 64
            // Every customer's price varies by -15%..+25%, so a scammer's price is not a tell.
            double price = job.unitPrice() * (0.85D + rnd.nextDouble() * 0.40D);
            this.order = new CustomerOrder(item, count, price);
        }
    }

    private void equipRaider() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(60.0D);
        getAttribute(Attributes.ARMOR).setBaseValue(8.0D);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.4D);
        this.setHealth(60.0F);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    @Override
    public void onAddedToLevel() {
        super.onAddedToLevel();
        // Spawn eggs and /summon create the entity without calling initialize().
        if (!this.level().isClientSide && !initialized) {
            initialize(CustomerJob.randomCivilian(this.getRandom()), CustomerRole.NORMAL, false);
        }
    }

    public CustomerJob getJob() {
        return CustomerJob.byIndex(this.entityData.get(DATA_JOB));
    }

    public CustomerRole getRole() {
        return role;
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

    public int getState() {
        return this.entityData.get(DATA_STATE);
    }

    public void setState(int state) {
        this.entityData.set(DATA_STATE, state);
    }

    public int getReveal() {
        return this.entityData.get(DATA_REVEAL);
    }

    public boolean isFleeing() {
        return fleeTicks > 0;
    }

    public void startFleeing() {
        this.fleeTicks = FLEE_TICKS;
        setState(STATE_FLEE);
    }

    public List<ItemStack> getLoot() {
        return loot;
    }

    /** A disguised thief drops the disguise: shows a mask or sunglasses and puffs of smoke. */
    public void reveal() {
        int look = role == CustomerRole.ROBBER ? 1 : role == CustomerRole.SCAMMER ? 2 : 0;
        if (look == 0 || getReveal() != 0) {
            return;
        }
        this.entityData.set(DATA_REVEAL, look);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.5D, getZ(), 24, 0.3D, 0.5D, 0.3D, 0.05D);
        }
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.6F);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 4 == 0) {
            spawnStateParticles(serverLevel);
        }
        if (fleeTicks > 0) {
            fleeTicks--;
            if (fleeTicks == 0) {
                if (role != CustomerRole.NORMAL) {
                    // The thief got away with it.
                    this.discard();
                    return;
                }
                setState(STATE_IDLE);
            }
        }
        if (visitor && this.tickCount > VISITOR_LIFETIME && fleeTicks == 0) {
            this.discard();
        }
    }

    private void spawnStateParticles(ServerLevel serverLevel) {
        switch (getState()) {
            case STATE_CHARGE -> serverLevel.sendParticles(
                    ParticleTypes.ANGRY_VILLAGER, getX(), getY() + 2.1D, getZ(), 1, 0.25D, 0.1D, 0.25D, 0.0D);
            case STATE_RANSACK -> serverLevel.sendParticles(
                    ParticleTypes.SMOKE, getX(), getY() + 1.0D, getZ(), 4, 0.3D, 0.4D, 0.3D, 0.02D);
            case STATE_FLEE -> {
                if (role != CustomerRole.NORMAL) {
                    serverLevel.sendParticles(
                            ParticleTypes.CLOUD, getX(), getY() + 0.1D, getZ(), 2, 0.2D, 0.05D, 0.2D, 0.02D);
                }
            }
            default -> {
            }
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
        if (role == CustomerRole.RAIDER) {
            player.displayClientMessage(Component.translatable("message.tycoonrogue.raider.talk"), true);
            return InteractionResult.CONSUME;
        }
        if (order == null) {
            player.displayClientMessage(Component.translatable("message.tycoonrogue.no_order"), true);
            return InteractionResult.CONSUME;
        }
        if (isFleeing()) {
            return InteractionResult.CONSUME;
        }

        CustomerJob job = getJob();
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

        if (role == CustomerRole.SCAMMER) {
            // Takes the goods, never pays.
            loot.add(handedOver);
            if (order.isComplete()) {
                reveal();
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
        tag.putInt("Role", role.ordinal());
        tag.putInt("Reveal", getReveal());
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
        CustomerRole[] roles = CustomerRole.values();
        this.role = roles[Math.floorMod(tag.getInt("Role"), roles.length)];
        this.entityData.set(DATA_REVEAL, tag.getInt("Reveal"));
        this.entityData.set(DATA_BIOME, tag.getInt("Biome"));
        this.entityData.set(DATA_JOB, tag.getInt("Job"));
        this.entityData.set(DATA_TASTE, tag.getInt("Taste"));
        this.pickiness = tag.getInt("Pickiness");
        this.generosity = tag.getInt("Generosity");
        this.timidity = tag.getInt("Timidity");
        this.talkativeness = tag.getInt("Talkativeness");
        this.gossip = tag.getInt("Gossip");
        this.fleeTicks = tag.getInt("FleeTicks");
        if (fleeTicks > 0) {
            setState(STATE_FLEE);
        }
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
        return role == CustomerRole.RAIDER ? SoundEvents.PILLAGER_AMBIENT : SoundEvents.VILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return role == CustomerRole.RAIDER ? SoundEvents.PILLAGER_HURT : SoundEvents.VILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return role == CustomerRole.RAIDER ? SoundEvents.PILLAGER_DEATH : SoundEvents.VILLAGER_DEATH;
    }
}
