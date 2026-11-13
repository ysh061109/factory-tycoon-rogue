package com.ysh.tycoonrogue.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Picks what a customer says. Lines are translation keys, so each player sees them in their own
 * language (Korean and English). Personality decides the variant of the line.
 *
 * Key format: dialogue.tycoonrogue.&lt;situation&gt;.&lt;variant&gt;.&lt;n&gt;
 * Keep the Situation table below in sync with tools/gen_lang.py (the script checks it).
 */
public final class Dialogue {
    /** id, whether personality changes the wording, number of lines per variant. */
    public enum Situation {
        GREETING("greeting", true, 2),
        THANKS("thanks", true, 2),
        SCARED("scared", true, 2),
        COMPLAIN("complain", true, 2),
        ORDER("order", false, 3),
        PARTIAL("partial", false, 2),
        GOSSIP("gossip", false, 3),
        ROBBER_REVEAL("robber_reveal", false, 2),
        SCAMMER_REVEAL("scammer_reveal", false, 2),
        RAIDER_CHARGE("raider_charge", false, 3),
        RAIDER_FLEE("raider_flee", false, 2),
        LEAVING("leaving", false, 3),
        INSPECT("inspect", false, 3),
        RATING_HIGH("rating_high", true, 2),
        RATING_MID("rating_mid", false, 2),
        RATING_LOW("rating_low", true, 2),
        GUARD_HELLO("guard_hello", false, 3),
        COLOR_TALK("color_talk", false, 3),
        GUARD_SPOT("guard_spot", false, 3),
        GUARD_CATCH("guard_catch", false, 2),
        STREAMER("streamer", false, 3),
        BELT_WATCH("belt_watch", false, 3),
        WHEEL_LOOK("wheel_look", false, 3),
        ROCKET_LOOK("rocket_look", false, 3),
        STOP_IDLE("stop_idle", false, 3),
        FLOW_GOOD("flow_good", false, 2),
        FLOW_BAD("flow_bad", false, 2),
        PROPELLER_LOOK("propeller_look", false, 3),
        SAIL_LOOK("sail_look", false, 3),
        ENGINE_LOOK("engine_look", false, 3),
        BALLOON_LOOK("balloon_look", false, 3),
        BROWSE("browse", false, 3),
        BROWSE_BUY("browse_buy", false, 3),
        CHECKOUT("checkout", false, 3);

        private final String id;
        private final boolean personality;
        private final int lines;

        Situation(String id, boolean personality, int lines) {
            this.id = id;
            this.personality = personality;
            this.lines = lines;
        }
    }

    private Dialogue() {
    }

    /** Which wording this customer uses: shy, picky, chatty, generous or plain. */
    public static String variantOf(CustomerEntity customer) {
        if (customer.getTimidity() >= 70) {
            return "shy";
        }
        if (customer.getPickiness() >= 70) {
            return "picky";
        }
        if (customer.getTalkativeness() >= 70) {
            return "chatty";
        }
        if (customer.getGenerosity() >= 70) {
            return "generous";
        }
        return "plain";
    }

    public static Component line(CustomerEntity customer, Situation situation, Object... args) {
        RandomSource random = customer.getRandom();
        String variant = situation.personality ? variantOf(customer) : "plain";
        int n = 1 + random.nextInt(situation.lines);
        return Component.translatable("dialogue.tycoonrogue." + situation.id + "." + variant + "." + n, args);
    }

    /** A line about the customer's favourite topic (history, cars, airplanes, coffee or cola). */
    public static Component tasteLine(CustomerEntity customer) {
        int n = 1 + customer.getRandom().nextInt(2);
        return Component.translatable("dialogue.tycoonrogue.taste_" + customer.getTaste().id() + ".plain." + n);
    }
}
