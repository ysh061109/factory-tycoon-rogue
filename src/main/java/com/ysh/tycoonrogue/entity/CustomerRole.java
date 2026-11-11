package com.ysh.tycoonrogue.entity;

/**
 * What a customer is really up to. The role is hidden from the player: robbers and scammers look
 * exactly like ordinary customers until they strike.
 */
public enum CustomerRole {
    /** Pays what the order is worth. */
    NORMAL,
    /** Disguised. Stands close to the player, then grabs some emeralds and runs. */
    ROBBER,
    /** Disguised. Takes the delivered goods and never pays. */
    SCAMMER,
    /** Not disguised. Armed, charges the player, takes a large share of the emeralds and ransacks chests. */
    RAIDER,
    /** A hired guard. Patrols near the player, spots disguised thieves and chases down criminals. */
    GUARD
}
