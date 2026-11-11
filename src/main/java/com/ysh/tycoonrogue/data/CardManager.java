package com.ysh.tycoonrogue.data;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.ysh.tycoonrogue.TycoonRogue;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads the card pool from data packs (data/&lt;namespace&gt;/tycoonrogue/cards/*.json), so other
 * mods and data packs can add their own cards without any code.
 */
@EventBusSubscriber(modid = TycoonRogue.MODID)
public final class CardManager extends SimpleJsonResourceReloadListener {
    public static final CardManager INSTANCE = new CardManager();

    private Map<ResourceLocation, CardDefinition> cards = Map.of();

    private CardManager() {
        super(new Gson(), "tycoonrogue/cards");
    }

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(INSTANCE);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> objects, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<ResourceLocation, CardDefinition> loaded = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : objects.entrySet()) {
            try {
                loaded.put(entry.getKey(), parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException e) {
                TycoonRogue.LOGGER.error("Skipping broken card {}: {}", entry.getKey(), e.toString());
            }
        }
        this.cards = Map.copyOf(loaded);
        TycoonRogue.LOGGER.info("Loaded {} cards", cards.size());
    }

    private static CardDefinition parse(ResourceLocation id, JsonObject json) {
        CardRarity rarity = CardRarity.valueOf(json.get("rarity").getAsString().toUpperCase(Locale.ROOT));
        Map<String, Double> effects = new HashMap<>();
        if (json.has("effects")) {
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("effects").entrySet()) {
                effects.put(e.getKey(), e.getValue().getAsDouble());
            }
        }
        return new CardDefinition(id, rarity, Map.copyOf(effects), stringList(json, "jobs"), stringList(json, "tastes"));
    }

    private static List<String> stringList(JsonObject json, String key) {
        List<String> list = new ArrayList<>();
        if (json.has(key)) {
            json.getAsJsonArray(key).forEach(e -> list.add(e.getAsString()));
        }
        return List.copyOf(list);
    }

    public CardDefinition get(ResourceLocation id) {
        return cards.get(id);
    }

    /**
     * Draws {@code count} different cards for a round-end choice. Rarer cards appear more often
     * as the rounds go on. Returns fewer if the pool is small.
     */
    public List<ResourceLocation> draw(int round, int count, RandomSource random) {
        List<CardDefinition> pool = new ArrayList<>();
        for (CardDefinition card : cards.values()) {
            if (round >= card.rarity().firstRound()) {
                pool.add(card);
            }
        }
        List<ResourceLocation> picked = new ArrayList<>();
        while (picked.size() < count && !pool.isEmpty()) {
            int total = 0;
            for (CardDefinition card : pool) {
                total += card.rarity().weight();
            }
            int roll = random.nextInt(total);
            for (int i = 0; i < pool.size(); i++) {
                roll -= pool.get(i).rarity().weight();
                if (roll < 0) {
                    picked.add(pool.remove(i).id());
                    break;
                }
            }
        }
        return picked;
    }
}
