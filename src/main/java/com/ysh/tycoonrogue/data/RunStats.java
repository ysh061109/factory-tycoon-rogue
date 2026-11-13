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
        // The job that is trending this round pays 30% more.
        if (job.id().equals(data.getTrendJob())) {
            multiplier += 0.30D;
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
    /** How many hired guards should be on duty. */
    public static int guards(TycoonData data) {
        return (int) Math.round(sum(data, "guards"));
    }

    public static double threatMultiplier(TycoonData data) {
        return Math.max(0.05D, 1.0D + sum(data, "threat_mult"));
    }

    /** Speed factor for fleeing criminals: 1.0 normal, lower is slower. */
    public static double fleeMultiplier(TycoonData data) {
        return Math.max(0.35D, 1.0D - sum(data, "flee_slow"));
    }

    /** Flat points added to every factory score (0..100 scale). */
    public static double ratingBonus(TycoonData data) {
        return sum(data, "rating_bonus");
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
