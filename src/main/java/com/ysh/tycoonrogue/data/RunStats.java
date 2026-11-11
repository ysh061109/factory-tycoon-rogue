package com.ysh.tycoonrogue.data;

import com.ysh.tycoonrogue.entity.CustomerJob;
import com.ysh.tycoonrogue.entity.Taste;
import net.minecraft.resources.ResourceLocation;

/** Adds up the effects of all cards the player has picked in this run. */
public final class RunStats {
    private RunStats() {
    }

    private static double sum(TycoonData data, String key) {
        double total = 0.0D;
        for (ResourceLocation id : data.getCards()) {
            CardDefinition card = CardManager.INSTANCE.get(id);
            if (card != null) {
                total += card.effect(key);
            }
        }
        return total;
    }

    /** Multiplier for how often visitors come. */
    public static double visitorMultiplier(TycoonData data) {
        return Math.max(0.2D, 1.0D + sum(data, "visitor_rate"));
    }

    /** Multiplier for what customers of this job pay, from cards only. */
    public static double rewardMultiplier(TycoonData data, CustomerJob job) {
        double multiplier = 1.0D + sum(data, "reward_mult");
        for (ResourceLocation id : data.getCards()) {
            CardDefinition card = CardManager.INSTANCE.get(id);
            if (card != null && card.effect("demand_bonus") != 0.0D
                    && (card.jobs().isEmpty() || card.jobs().contains(job.id()))) {
                multiplier += card.effect("demand_bonus");
            }
        }
        return Math.max(0.3D, multiplier);
    }

    public static int reputationGain(TycoonData data) {
        return (int) Math.round(sum(data, "reputation_gain"));
    }

    public static double quotaMultiplier(TycoonData data) {
        return Math.max(0.3D, 1.0D + sum(data, "quota_mult"));
    }

    /** Multiplier for how often robbers, scammers and raids come. */
    public static double threatMultiplier(TycoonData data) {
        return Math.max(0.05D, 1.0D + sum(data, "threat_mult"));
    }

    public static double tipMultiplier(TycoonData data) {
        return Math.max(0.0D, 1.0D + sum(data, "tip_mult"));
    }

    /** Extra rating (as a fraction) from customers who love this taste. */
    public static double tasteBonus(TycoonData data, Taste taste) {
        double total = 0.0D;
        for (ResourceLocation id : data.getCards()) {
            CardDefinition card = CardManager.INSTANCE.get(id);
            if (card != null && card.tastes().contains(taste.id())) {
                total += card.effect("taste_bonus");
            }
        }
        return total;
    }
}
