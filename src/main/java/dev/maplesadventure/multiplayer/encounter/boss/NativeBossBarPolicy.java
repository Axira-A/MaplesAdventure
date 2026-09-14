package dev.maplesadventure.multiplayer.encounter.boss;

/** Explicit adapter policy; core never suppresses native boss bars globally. */
public enum NativeBossBarPolicy {
    KEEP,
    SUPPRESS_WHEN_MANAGED,
    ADAPTER_HANDLED
}
