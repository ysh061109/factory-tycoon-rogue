package com.ysh.tycoonrogue.data;

import java.util.Locale;

public enum CardRarity {
    COMMON(60, 1, 0xFFB8BEC8),
    RARE(30, 2, 0xFF4FA3FF),
    LEGENDARY(10, 4, 0xFFFFB92E);

    private final int weight;
    private final int firstRound;
    private final int color;

    CardRarity(int weight, int firstRound, int color) {
        this.weight = weight;
        this.firstRound = firstRound;
        this.color = color;
    }

    /** How likely this rarity is to be offered (higher = more often). */
    public int weight() {
        return weight;
    }

    /** Rarer cards only show up in later rounds. */
    public int firstRound() {
        return firstRound;
    }

    public int color() {
        return color;
    }

    public String translationKey() {
        return "rarity.tycoonrogue." + name().toLowerCase(Locale.ROOT);
    }

    public static CardRarity byIndex(int index) {
        CardRarity[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
