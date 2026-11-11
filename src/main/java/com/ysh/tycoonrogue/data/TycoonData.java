package com.ysh.tycoonrogue.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything about the current run that must survive a restart: reputation, rumor, the round
 * timer and revenue, and the cards picked so far. One instance per world.
 */
public class TycoonData extends SavedData {
    public static final String NAME = "tycoonrogue_run";
    public static final int START_REPUTATION = 50;
    /** One round is one Minecraft day. */
    public static final int ROUND_TICKS = 24000;
    /** This many failed rounds in a row ends the run (cards and round reset). */
    public static final int MAX_FAIL_STREAK = 3;

    private int reputation = START_REPUTATION;
    private int rumor;
    private int round = 1;
    private int ticksLeft = ROUND_TICKS;
    private int revenue;
    private int failStreak;
    private final List<ResourceLocation> cards = new ArrayList<>();
    /** Cards offered at the end of a round, waiting for the player to choose one. */
    private final List<ResourceLocation> pending = new ArrayList<>();

    public static TycoonData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TycoonData::new, TycoonData::load, null), NAME);
    }

    // ---- reputation and rumor ----

    public int getReputation() {
        return reputation;
    }

    /** Changes reputation (0..100). Tells the player why, if one is given. */
    public void changeReputation(int delta, Player witness, String reasonKey) {
        int before = reputation;
        reputation = Math.max(0, Math.min(100, reputation + delta));
        setDirty();
        int applied = reputation - before;
        if (witness != null && applied != 0) {
            witness.displayClientMessage(Component.translatable(
                    "message.tycoonrogue.reputation",
                    (applied > 0 ? "+" : "") + applied,
                    Component.translatable(reasonKey),
                    reputation), true);
        }
    }

    /** Rumor 0..100: how much bad word of mouth is going around. It lowers visitor numbers. */
    public int getRumor() {
        return rumor;
    }

    public void addRumor(int amount) {
        rumor = Math.max(0, Math.min(100, rumor + amount));
        setDirty();
    }

    /** Called every 600 ticks. Bad word of mouth fades slowly. */
    public void decayRumor() {
        if (rumor > 0) {
            rumor = Math.max(0, rumor - 2);
            setDirty();
        }
    }

    // ---- rounds ----

    public int getRound() {
        return round;
    }

    public int getTicksLeft() {
        return ticksLeft;
    }

    public int getRevenue() {
        return revenue;
    }

    /** Emeralds that must be earned this round: 40, then 30 more every round, changed by cards. */
    public int getQuota() {
        return (int) Math.round((40.0D + 30.0D * (round - 1)) * RunStats.quotaMultiplier(this));
    }

    public void addRevenue(int emeralds) {
        if (emeralds > 0) {
            revenue += emeralds;
            setDirty();
        }
    }

    public void tickRound(int ticks) {
        ticksLeft = Math.max(0, ticksLeft - ticks);
        setDirty();
    }

    public void forceRoundEnd() {
        ticksLeft = 0;
        setDirty();
    }

    /** Moves to the next round with a fresh timer and revenue. */
    public void startNextRound() {
        round++;
        ticksLeft = ROUND_TICKS;
        revenue = 0;
        setDirty();
    }

    public int getFailStreak() {
        return failStreak;
    }

    public void setFailStreak(int streak) {
        failStreak = streak;
        setDirty();
    }

    /** The run is over: forget cards and start again from round 1. */
    public void resetRun() {
        round = 1;
        ticksLeft = ROUND_TICKS;
        revenue = 0;
        failStreak = 0;
        reputation = START_REPUTATION;
        rumor = 0;
        cards.clear();
        pending.clear();
        setDirty();
    }

    // ---- cards ----

    public List<ResourceLocation> getCards() {
        return cards;
    }

    public List<ResourceLocation> getPending() {
        return pending;
    }

    public void setPending(List<ResourceLocation> offered) {
        pending.clear();
        pending.addAll(offered);
        setDirty();
    }

    /** Takes one of the offered cards. Returns false if it was not on offer. */
    public boolean pick(ResourceLocation card) {
        if (!pending.contains(card)) {
            return false;
        }
        cards.add(card);
        pending.clear();
        setDirty();
        return true;
    }

    // ---- saving ----

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Reputation", reputation);
        tag.putInt("Rumor", rumor);
        tag.putInt("Round", round);
        tag.putInt("TicksLeft", ticksLeft);
        tag.putInt("Revenue", revenue);
        tag.putInt("FailStreak", failStreak);
        tag.put("Cards", toList(cards));
        tag.put("Pending", toList(pending));
        return tag;
    }

    public static TycoonData load(CompoundTag tag, HolderLookup.Provider registries) {
        TycoonData data = new TycoonData();
        data.reputation = tag.contains("Reputation") ? tag.getInt("Reputation") : START_REPUTATION;
        data.rumor = tag.getInt("Rumor");
        data.round = Math.max(1, tag.getInt("Round"));
        data.ticksLeft = tag.contains("TicksLeft") ? tag.getInt("TicksLeft") : ROUND_TICKS;
        data.revenue = tag.getInt("Revenue");
        data.failStreak = tag.getInt("FailStreak");
        readList(tag.getList("Cards", Tag.TAG_STRING), data.cards);
        readList(tag.getList("Pending", Tag.TAG_STRING), data.pending);
        return data;
    }

    private static ListTag toList(List<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    private static void readList(ListTag list, List<ResourceLocation> into) {
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                into.add(id);
            }
        }
    }
}
