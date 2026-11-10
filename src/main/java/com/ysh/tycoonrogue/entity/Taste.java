package com.ysh.tycoonrogue.entity;

/** What a customer is passionate about. Used later to score how well a factory fits a customer. */
public enum Taste {
    HISTORY("history"),
    CARS("cars"),
    AIRPLANES("airplanes"),
    COFFEE("coffee"),
    COLA("cola");

    private final String id;

    Taste(String id) {
        this.id = id;
    }

    public String translationKey() {
        return "taste.tycoonrogue." + id;
    }

    public static Taste byId(int index) {
        Taste[] values = values();
        return values[Math.floorMod(index, values.length)];
    }
}
