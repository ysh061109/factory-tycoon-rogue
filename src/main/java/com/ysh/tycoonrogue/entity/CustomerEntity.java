package com.ysh.tycoonrogue.entity;

import net.minecraft.nbt.CompoundTag;
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
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * A customer who visits the player's factory. Looks like a villager but is its own entity,
 * so we are free to add speech bubbles, expressions and custom animations later.
 */
public class CustomerEntity extends PathfinderMob {
    /** Villager biome skins (vanilla textures). */
    public static final String[] BIOMES = {"desert", "jungle", "plains", "savanna", "snow", "swamp", "taiga"};
    /** Villager profession outfits (vanilla textures). Index -1 means no outfit. */
    public static final String[] OUTFITS = {
            "armorer", "butcher", "cartographer", "cleric", "farmer", "fisherman", "fletcher",
            "leatherworker", "librarian", "mason", "shepherd", "toolsmith", "weaponsmith"
    };

    private static final EntityDataAccessor<Integer> DATA_BIOME =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_OUTFIT =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_TASTE =
            SynchedEntityData.defineId(CustomerEntity.class, EntityDataSerializers.INT);

    // Personality, each 0..100.
    private int pickiness;
    private int generosity;
    private int timidity;
    private int talkativeness;
    private int gossip;

    public CustomerEntity(EntityType<? extends CustomerEntity> type, Level level) {
        super(type, level);
        if (!level.isClientSide) {
            rollPersonality();
        }
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.6D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BIOME, 0);
        builder.define(DATA_OUTFIT, -1);
        builder.define(DATA_TASTE, 0);
    }

    private void rollPersonality() {
        var rnd = this.getRandom();
        this.entityData.set(DATA_BIOME, rnd.nextInt(BIOMES.length));
        // About one in five customers wears no outfit.
        this.entityData.set(DATA_OUTFIT, rnd.nextInt(5) == 0 ? -1 : rnd.nextInt(OUTFITS.length));
        this.entityData.set(DATA_TASTE, rnd.nextInt(Taste.values().length));
        this.pickiness = rnd.nextInt(101);
        this.generosity = rnd.nextInt(101);
        this.timidity = rnd.nextInt(101);
        this.talkativeness = rnd.nextInt(101);
        this.gossip = rnd.nextInt(101);
    }

    public int getBiome() {
        return this.entityData.get(DATA_BIOME);
    }

    public int getOutfit() {
        return this.entityData.get(DATA_OUTFIT);
    }

    public Taste getTaste() {
        return Taste.byId(this.entityData.get(DATA_TASTE));
    }

    public int getPickiness() {
        return pickiness;
    }

    public int getGenerosity() {
        return generosity;
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

    /** Temporary debug interaction: shows this customer's taste and personality above the hotbar. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && !this.level().isClientSide) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.tycoonrogue.customer.info",
                            Component.translatable(getTaste().translationKey()),
                            pickiness, generosity, timidity, talkativeness, gossip),
                    true);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Biome", getBiome());
        tag.putInt("Outfit", getOutfit());
        tag.putInt("Taste", this.entityData.get(DATA_TASTE));
        tag.putInt("Pickiness", pickiness);
        tag.putInt("Generosity", generosity);
        tag.putInt("Timidity", timidity);
        tag.putInt("Talkativeness", talkativeness);
        tag.putInt("Gossip", gossip);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("Biome")) {
            this.entityData.set(DATA_BIOME, tag.getInt("Biome"));
            this.entityData.set(DATA_OUTFIT, tag.getInt("Outfit"));
            this.entityData.set(DATA_TASTE, tag.getInt("Taste"));
            this.pickiness = tag.getInt("Pickiness");
            this.generosity = tag.getInt("Generosity");
            this.timidity = tag.getInt("Timidity");
            this.talkativeness = tag.getInt("Talkativeness");
            this.gossip = tag.getInt("Gossip");
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
