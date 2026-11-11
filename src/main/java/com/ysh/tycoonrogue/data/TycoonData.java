package com.ysh.tycoonrogue.data;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Everything about the current run that must survive a restart: reputation, rumor and
 * (added in later steps) rounds, cards and the social feed. One instance per world.
 */
public class TycoonData extends SavedData {
    public static final String NAME = "tycoonrogue_run";
    public static final int START_REPUTATION = 50;

    private int reputation = START_REPUTATION;
    private int rumor;

    public static TycoonData get(MinecraftServer server) {
        return server.overworld().getDataStorage()
                .computeIfAbsent(new SavedData.Factory<>(TycoonData::new, TycoonData::load, null), NAME);
    }

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

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("Reputation", reputation);
        tag.putInt("Rumor", rumor);
        return tag;
    }

    public static TycoonData load(CompoundTag tag, HolderLookup.Provider registries) {
        TycoonData data = new TycoonData();
        data.reputation = tag.contains("Reputation") ? tag.getInt("Reputation") : START_REPUTATION;
        data.rumor = tag.getInt("Rumor");
        return data;
    }
}
