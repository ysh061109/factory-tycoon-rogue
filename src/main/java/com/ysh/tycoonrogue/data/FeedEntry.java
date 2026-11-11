package com.ysh.tycoonrogue.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

/**
 * One post on the phone: either an SNS post written by a customer ({@code kind = "sns"}, with a
 * star rating) or a news headline ({@code kind = "news"}).
 *
 * {@code key} is a translation key, so the text appears in each player's language. An entry in
 * {@code args} that starts with "$" is itself a translation key (for example a job name).
 */
public record FeedEntry(String kind, String key, List<String> args, int stars, String job, int num, int round) {
    public static final String SNS = "sns";
    public static final String NEWS = "news";

    public static FeedEntry sns(String key, List<String> args, int stars, String job, int num, int round) {
        return new FeedEntry(SNS, key, args, stars, job, num, round);
    }

    public static FeedEntry news(String key, List<String> args, int round) {
        return new FeedEntry(NEWS, key, args, 0, "", 0, round);
    }

    public boolean isNews() {
        return NEWS.equals(kind);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind);
        tag.putString("Key", key);
        ListTag list = new ListTag();
        for (String arg : args) {
            list.add(StringTag.valueOf(arg));
        }
        tag.put("Args", list);
        tag.putInt("Stars", stars);
        tag.putString("Job", job);
        tag.putInt("Num", num);
        tag.putInt("Round", round);
        return tag;
    }

    public static FeedEntry load(CompoundTag tag) {
        List<String> args = new ArrayList<>();
        ListTag list = tag.getList("Args", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            args.add(list.getString(i));
        }
        return new FeedEntry(tag.getString("Kind"), tag.getString("Key"), args, tag.getInt("Stars"),
                tag.getString("Job"), tag.getInt("Num"), tag.getInt("Round"));
    }

    public void write(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(kind);
        buf.writeUtf(key);
        buf.writeVarInt(args.size());
        for (String arg : args) {
            buf.writeUtf(arg);
        }
        buf.writeVarInt(stars);
        buf.writeUtf(job);
        buf.writeVarInt(num);
        buf.writeVarInt(round);
    }

    public static FeedEntry read(RegistryFriendlyByteBuf buf) {
        String kind = buf.readUtf();
        String key = buf.readUtf();
        int count = buf.readVarInt();
        List<String> args = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            args.add(buf.readUtf());
        }
        return new FeedEntry(kind, key, args, buf.readVarInt(), buf.readUtf(), buf.readVarInt(), buf.readVarInt());
    }
}
