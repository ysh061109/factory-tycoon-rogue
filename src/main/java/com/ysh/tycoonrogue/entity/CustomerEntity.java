package com.ysh.tycoonrogue.entity;

import net.minecraft.core.BlockPos;
import com.ysh.tycoonrogue.data.RunStats;
import com.ysh.tycoonrogue.data.TycoonData;
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
 * Customers talk in speech bubbles above their heads and panic when a gun is pointed at them.
 */
public class CustomerEntity extends PathfinderMob {
    /** Villager biome skins (vanilla textures). */
    public static final String[] BIOMES = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};

    /** Animation / particle state, synced to clients. */
    public static final int STATE_IDLE = 0;
    public static final int STATE_CHARGE = 1;
    public static final int STATE_RANSACK = 2;
    public static final int STATE_FLEE = 3;
    public static final int STATE_SCARED = 4;

    /** How long a visitor stays before leaving on its own (ticks). */
    private static final int VISITOR_LIFETIME = 12000;
    private static final int FLEE_TICKS = 500;
    private static final int SCARED_TICKS = 160;
    /** A customer that was scared once is not scared again (and the owner not punished again) for this long. */
    private static final int SCARE_COOLDOWN = 600;
    private static final int SPEECH_TICKS = 100;

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
    /** What the customer is saying right now. Empty means silent. Shown in a bubble above the head. */
    private static final EntityDataAccessor<Component> DATA_SPEECH =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.COMPONENT);

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
    private int scaredTicks;
    private int scareCooldown;
    /** True for customers created by the visitor spawner. They leave after a while. */
    private boolean visitor;
    /** The Tycoon Counter this customer is queuing at, or null when not waiting in line. */
    private BlockPos queueCounter;
    private long queueSince;
    private boolean initialized;

    private int speechTimer;
    private boolean greeted;
    private int chatCooldown = 200;
    private Dialogue.Situation queuedSituation;
    private int queuedDelay;

    public CustomerEntity(EntityType<? extends CustomerEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new CustomerFleeGoal(this));
        this.goalSelector.addGoal(1, new GuardGoal(this));
        this.goalSelector.addGoal(2, new RaiderGoal(this));
        this.goalSelector.addGoal(3, new DisguisedRobberGoal(this));
        this.goalSelector.addGoal(3, new QueueGoal(this));
        this.goalSelector.addGoal(4, new BrowseGoal(this));
        this.goalSelector.addGoal(4, new VisitCounterGoal(this));
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
        builder.define(DATA_SPEECH, Component.empty());
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

        if (role == CustomerRole.GUARD) {
            equipGuard();
            return;
        }
        if (role == CustomerRole.RAIDER) {
            equipRaider();
            return;
        }
        List<Item> items = job.availableItems();
        if (!items.isEmpty()) {
            Item item = items.get(rnd.nextInt(items.size()));
            // Orders get bigger as the rounds go on: up to 64 items at first, up to 128 later.
            int round = this.level().getServer() == null ? 1 : TycoonData.get(this.level().getServer()).getRound();
            int steps = 8 + Math.min(8, (round - 1) / 2);
            int count = 8 * (1 + rnd.nextInt(steps));
            // Every customer's price varies by -15%..+25%, so a scammer's price is not a tell.
            double price = job.unitPrice() * (0.85D + rnd.nextDouble() * 0.40D);
            this.order = new CustomerOrder(item, count, price);
        }
    }

    private void equipGuard() {
        getAttribute(Attributes.MAX_HEALTH).setBaseValue(40.0D);
        getAttribute(Attributes.ARMOR).setBaseValue(6.0D);
        getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7.0D);
        getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(0.3D);
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.55D);
        this.setHealth(40.0F);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setPersistenceRequired();
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

    private boolean toured;

    /** Each customer likes one colour. It comes from the customer's id, so it never changes. */
    public net.minecraft.world.item.DyeColor getFavoriteColor() {
        return net.minecraft.world.item.DyeColor.byId(Math.floorMod(this.getUUID().hashCode(), 16));
    }

    // ---- shopping at display stands ----
    private boolean browsed;
    private ItemStack purchase = ItemStack.EMPTY;
    private int purchasePrice;

    public boolean hasBrowsed() {
        return browsed;
    }

    public void setBrowsed() {
        this.browsed = true;
    }

    public ItemStack getPurchase() {
        return purchase;
    }

    public int getPurchasePrice() {
        return purchasePrice;
    }

    /** The customer picked this item and owes this much. Empty stack clears it. */
    public void setPurchase(ItemStack stack, int price) {
        this.purchase = stack;
        this.purchasePrice = stack.isEmpty() ? 0 : price;
        this.setItemInHand(InteractionHand.MAIN_HAND, stack);
    }

    public boolean hasToured() {
        return toured;
    }

    public void setToured() {
        this.toured = true;
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

    public int getPickiness() {
        return pickiness;
    }

    public int getTimidity() {
        return timidity;
    }

    public int getTalkativeness() {
        return talkativeness;
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

    public Component getSpeech() {
        return this.entityData.get(DATA_SPEECH);
    }

    public boolean isQueued() {
        return queueCounter != null;
    }

    public BlockPos getQueueCounter() {
        return queueCounter;
    }

    public long getQueueSince() {
        return queueSince;
    }

    public void joinQueue(BlockPos counter, long since) {
        this.queueCounter = counter.immutable();
        this.queueSince = since;
    }

    public void leaveQueue() {
        this.queueCounter = null;
        this.getNavigation().stop();
    }

    public boolean isFleeing() {
        return fleeTicks > 0;
    }

    public boolean isScared() {
        return scaredTicks > 0;
    }

    public void startFleeing() {
        this.fleeTicks = FLEE_TICKS;
        setState(STATE_FLEE);
    }

    public List<ItemStack> getLoot() {
        return loot;
    }

    // ---- Speech ----

    public void say(Component line) {
        if (this.level().isClientSide) {
            return;
        }
        this.entityData.set(DATA_SPEECH, line);
        this.speechTimer = SPEECH_TICKS;
    }

    public void say(Dialogue.Situation situation, Object... args) {
        say(Dialogue.line(this, situation, args));
    }

    /** Says the line after a short delay, for example gossip after seeing something. */
    public void sayLater(Dialogue.Situation situation, int delayTicks) {
        this.queuedSituation = situation;
        this.queuedDelay = delayTicks;
    }

    // ---- Fear ----

    /**
     * A gun was pointed at this customer. Returns true if the customer got scared just now
     * (false when already scared or still on cooldown), so the caller can punish the owner once.
     */
    public boolean scare() {
        if (role == CustomerRole.RAIDER || role == CustomerRole.GUARD || scaredTicks > 0 || scareCooldown > 0) {
            return false;
        }
        this.scaredTicks = SCARED_TICKS + timidity;
        this.scareCooldown = SCARE_COOLDOWN;
        setState(STATE_SCARED);
        say(Dialogue.Situation.SCARED);
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.2F);
        this.getNavigation().stop();
        return true;
    }

    /** A disguised thief drops the disguise: shows a mask or sunglasses and puffs of smoke. */
    public boolean isStreamer() {
        return getReveal() == 3;
    }

    /** Turns this customer into a YouTuber guest: triple order, 1.5x pay, camera headset overlay. */
    public void makeStreamer() {
        this.entityData.set(DATA_REVEAL, 3);
        if (order != null) {
            this.order = new CustomerOrder(order.item(), order.count() * 3, order.unitPriceValue());
        }
    }

    public void reveal() {
        int look = role == CustomerRole.ROBBER ? 1 : role == CustomerRole.SCAMMER ? 2 : 0;
        if (look == 0 || getReveal() != 0 || isStreamer()) {
            return;
        }
        this.entityData.set(DATA_REVEAL, look);
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SMOKE, getX(), getY() + 1.5D, getZ(), 24, 0.3D, 0.5D, 0.3D, 0.05D);
        }
        this.playSound(SoundEvents.VILLAGER_NO, 1.0F, 0.6F);
        say(look == 1 ? Dialogue.Situation.ROBBER_REVEAL : Dialogue.Situation.SCAMMER_REVEAL);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.level() instanceof ServerLevel serverLevel) {
            if (this.tickCount % 4 == 0) {
                spawnStateParticles(serverLevel);
            }
            tickSpeech(serverLevel);
        }
        if (scareCooldown > 0) {
            scareCooldown--;
        }
        if (scaredTicks > 0 && --scaredTicks == 0 && fleeTicks == 0) {
            setState(STATE_IDLE);
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

    private void tickSpeech(ServerLevel serverLevel) {
        if (speechTimer > 0 && --speechTimer == 0) {
            this.entityData.set(DATA_SPEECH, Component.empty());
        }
        if (queuedSituation != null && --queuedDelay <= 0) {
            say(queuedSituation);
            queuedSituation = null;
        }
        if (this.tickCount % 20 != 0 || role == CustomerRole.RAIDER || isScared() || isFleeing()) {
            return;
        }
        Player near = serverLevel.getNearestPlayer(this, 7.0D);
        if (near == null || near.isSpectator()) {
            return;
        }
        if (!greeted) {
            greeted = true;
            say(isStreamer() ? Dialogue.Situation.STREAMER : Dialogue.Situation.GREETING);
            return;
        }
        // Talkative customers chat about their favourite topic now and then.
        if (speechTimer == 0 && --chatCooldown <= 0) {
            chatCooldown = 300 - talkativeness * 2 + this.getRandom().nextInt(200);
            if (this.getRandom().nextInt(100) < talkativeness) {
                say(Dialogue.tasteLine(this));
            }
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
            // Cold sweat while scared.
            case STATE_SCARED -> serverLevel.sendParticles(
                    ParticleTypes.SPLASH, getX(), getY() + 1.9D, getZ(), 3, 0.3D, 0.2D, 0.3D, 0.0D);
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
        if (role == CustomerRole.GUARD) {
            say(Dialogue.Situation.GUARD_HELLO);
            return InteractionResult.CONSUME;
        }
        if (purchasePrice > 0 && !purchase.isEmpty() && !isFleeing() && !isScared()) {
            // Checkout: the customer asked to pay for something from a display stand.
            TycoonData d = TycoonData.get(player.getServer());
            d.addRevenue(purchasePrice);
            Currency.pay(player, purchasePrice);
            player.displayClientMessage(Component.translatable("message.tycoonrogue.purchase",
                    purchase.getHoverName(), purchasePrice), true);
            d.changeReputation(1, player, "reason.tycoonrogue.sold");
            this.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
            say(Dialogue.Situation.THANKS);
            purchasePrice = 0;
            browsed = true;
            return InteractionResult.CONSUME;
        }
        if (order == null) {
            player.displayClientMessage(Component.translatable("message.tycoonrogue.no_order"), true);
            return InteractionResult.CONSUME;
        }
        if (isFleeing() || isScared()) {
            return InteractionResult.CONSUME;
        }

        CustomerJob job = getJob();
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty() || !held.is(order.item())) {
            // Show the order (with the job name) when the player is not holding the wanted item.
            if (!held.isEmpty()) {
                say(Dialogue.Situation.COMPLAIN);
            } else {
                say(Dialogue.Situation.ORDER, order.item().getDescription(), order.remaining());
            }
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
                TycoonData.get(player.getServer()).changeReputation(-3, player, "reason.tycoonrogue.scammed");
                com.ysh.tycoonrogue.data.Feed.news(player.getServer(), "scam");
                startFleeing();
            }
            return InteractionResult.CONSUME;
        }

        // Payment = price x generosity bonus (up to +30%) x cards x reputation (-20%..+20%).
        TycoonData data = TycoonData.get(player.getServer());
        double reputationFactor = 1.0D + (data.getReputation() - 50) / 250.0D;
        double multiplier = (1.0D + generosity / 100.0D * 0.3D)
                * RunStats.rewardMultiplier(data, job) * reputationFactor;
        if (isStreamer()) {
            multiplier *= 1.5D;
        }
        int emeralds = order.takeOwedPayment(multiplier);
        data.addRevenue(emeralds);
        Currency.pay(player, emeralds);
        player.displayClientMessage(Component.translatable(
                order.isComplete() ? "message.tycoonrogue.order.done" : "message.tycoonrogue.order.partial",
                emeralds, order.remaining()), true);
        if (order.isComplete()) {
            say(Dialogue.Situation.THANKS);
            if (isStreamer()) {
                data.addBuzz(30);
                data.addRumor(-10);
                com.ysh.tycoonrogue.data.Feed.post(player.getServer(), this, "viral", 5);
                com.ysh.tycoonrogue.data.Feed.news(player.getServer(), "streamer");
            }
            data.changeReputation(2 + RunStats.reputationGain(data), player, "reason.tycoonrogue.order_done");
            // The customer leaves happy. For now they simply walk away and despawn later.
            this.visitor = true;
            this.tickCount = Math.max(this.tickCount, VISITOR_LIFETIME - 600);
            sayLater(Dialogue.Situation.LEAVING, 80);
        } else {
            say(Dialogue.Situation.PARTIAL);
        }
        return InteractionResult.CONSUME;
    }

    /** Whatever a thief carries drops when they are killed. */
    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide) {
            for (ItemStack stack : loot) {
                this.spawnAtLocation(stack);
            }
            boolean hadLoot = !loot.isEmpty();
            loot.clear();
            if (role != CustomerRole.NORMAL && role != CustomerRole.GUARD
                    && source.getEntity() instanceof CustomerEntity guard && guard.getRole() == CustomerRole.GUARD
                    && this.level().getServer() != null) {
                TycoonData data = TycoonData.get(this.level().getServer());
                data.changeReputation(2, null, "reason.tycoonrogue.guard_caught");
                com.ysh.tycoonrogue.data.Feed.news(this.level().getServer(), "guard_catch");
                guard.say(Dialogue.Situation.GUARD_CATCH);
            }
            if (hadLoot && role != CustomerRole.NORMAL && source.getEntity() instanceof Player player
                    && player.getServer() != null) {
                TycoonData.get(player.getServer()).changeReputation(2, player, "reason.tycoonrogue.thief_caught");
            }
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Initialized", initialized);
        tag.putBoolean("Visitor", visitor);
        tag.putBoolean("Greeted", greeted);
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
        if (queueCounter != null) {
            tag.putLong("QueueCounter", queueCounter.asLong());
            tag.putLong("QueueSince", queueSince);
        }
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
        this.greeted = tag.getBoolean("Greeted");
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
        this.queueCounter = tag.contains("QueueCounter") ? BlockPos.of(tag.getLong("QueueCounter")) : null;
        this.queueSince = tag.getLong("QueueSince");
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
