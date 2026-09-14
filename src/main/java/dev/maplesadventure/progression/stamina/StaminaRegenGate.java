package dev.maplesadventure.progression.stamina;

/** Transient reasons that completely suppress Maples-owned stamina regeneration. */
public enum StaminaRegenGate {
    ATTACK,
    DODGE,
    SPRINT,
    GUARD_HIT,
    EXTERNAL
}
