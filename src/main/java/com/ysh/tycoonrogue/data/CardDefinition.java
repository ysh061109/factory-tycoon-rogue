package com.ysh.tycoonrogue.data;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;

/** One upgrade card, loaded from a JSON file in data/&lt;namespace&gt;/tycoonrogue/cards/. */
public record CardDefinition(ResourceLocation id, CardRarity rarity, Map<String, Double> effects,
                             List<String> jobs, List<String> tastes) {

    public double effect(String key) {
        return effects.getOrDefault(key, 0.0D);
    }

    public String nameKey() {
        return "card." + id.getNamespace() + "." + id.getPath() + ".name";
    }

    public String descriptionKey() {
        return "card." + id.getNamespace() + "." + id.getPath() + ".desc";
    }
}
