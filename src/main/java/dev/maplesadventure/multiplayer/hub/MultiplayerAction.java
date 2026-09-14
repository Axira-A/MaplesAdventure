package dev.maplesadventure.multiplayer.hub;

/** Bounded intent-only actions. No action carries a phase, role, player target or position. */
public enum MultiplayerAction {
    PLACE_COOP_SIGN,
    REMOVE_COOP_SIGN,
    PLACE_DUEL_SIGN,
    REMOVE_DUEL_SIGN,
    SEEK_INVASION,
    CANCEL_INVASION
}
