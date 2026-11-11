package com.ysh.tycoonrogue.client;

import com.ysh.tycoonrogue.data.FeedEntry;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

/** Turns feed entries into text for the phone screen and the notification banners. */
public final class FeedText {
    private FeedText() {
    }

    public static Component text(FeedEntry entry) {
        Object[] args = new Object[entry.args().size()];
        for (int i = 0; i < args.length; i++) {
            String arg = entry.args().get(i);
            args[i] = arg.startsWith("$") ? Component.translatable(arg.substring(1)) : arg;
        }
        return Component.translatable(entry.key(), args);
    }

    public static String jobName(FeedEntry entry) {
        return I18n.get("job.tycoonrogue." + entry.job());
    }

    /** "@Barista482" style user name. */
    public static String user(FeedEntry entry) {
        return "@" + jobName(entry).replace(" ", "") + entry.num();
    }

    /** First letter of the job name, shown on the round profile picture. */
    public static String initial(FeedEntry entry) {
        String name = jobName(entry);
        return name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1));
    }

    public static int avatarColor(FeedEntry entry) {
        int h = Math.abs((entry.job() + entry.num()).hashCode());
        float hue = (h % 360) / 360.0F;
        return 0xFF000000 | java.awt.Color.HSBtoRGB(hue, 0.55F, 0.85F) & 0xFFFFFF;
    }

    public static int likes(FeedEntry entry) {
        return entry.isNews() ? 0 : (entry.num() * 7) % 380 + entry.stars() * 25;
    }

    public static String stars(FeedEntry entry) {
        return "★".repeat(Math.max(0, entry.stars())) + "☆".repeat(Math.max(0, 5 - entry.stars()));
    }
}
