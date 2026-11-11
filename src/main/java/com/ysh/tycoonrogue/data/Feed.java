package com.ysh.tycoonrogue.data;

import com.ysh.tycoonrogue.entity.CustomerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;

import java.util.List;

/** Helpers that write SNS posts and news headlines into the run's feed. */
public final class Feed {
    private Feed() {
    }

    /** A customer posts a review. {@code kind} picks the wording, for example "review.4". */
    public static void post(MinecraftServer server, CustomerEntity author, String kind, int stars, String... args) {
        TycoonData data = TycoonData.get(server);
        RandomSource random = author.getRandom();
        int variant = 1 + random.nextInt(2);
        data.addFeed(FeedEntry.sns("feed.tycoonrogue." + kind + "." + variant, List.of(args), stars,
                author.getJob().id(), 100 + random.nextInt(900), data.getRound()));
    }

    /** A news headline. Arguments starting with "$" are translation keys. */
    public static void news(MinecraftServer server, String kind, String... args) {
        TycoonData data = TycoonData.get(server);
        data.addFeed(FeedEntry.news("feed.tycoonrogue.news." + kind, List.of(args), data.getRound()));
    }

    /** Average stars of the latest reviews, or 0 if there are none yet. */
    public static double averageRating(TycoonData data) {
        int total = 0;
        int count = 0;
        for (FeedEntry entry : data.getFeed()) {
            if (!entry.isNews() && entry.stars() > 0 && count < 20) {
                total += entry.stars();
                count++;
            }
        }
        return count == 0 ? 0.0D : (double) total / count;
    }
}
