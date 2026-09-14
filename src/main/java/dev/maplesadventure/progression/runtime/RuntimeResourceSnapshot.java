package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.progression.PlayerAttributeState;

/** Immutable local-player runtime context sent with the attribute snapshot. */
public record RuntimeResourceSnapshot(RuntimeResourceValue health, RuntimeResourceValue mana,
                                      RuntimeResourceValue stamina) {
    public RuntimeResourceSnapshot {
        if (health == null || mana == null || stamina == null) throw new IllegalArgumentException("Missing resource");
    }

    public RuntimeResourceValue value(DerivedRuntimeResource resource) {
        return switch (resource) {
            case HEALTH -> health;
            case MANA -> mana;
            case STAMINA -> stamina;
        };
    }

    public static RuntimeResourceSnapshot progressionOnly(PlayerAttributeState state) {
        return new RuntimeResourceSnapshot(
                RuntimeResourceValue.previewOnly(DerivedRuntimeResource.HEALTH.formula(state)),
                RuntimeResourceValue.previewOnly(DerivedRuntimeResource.MANA.formula(state)),
                RuntimeResourceValue.previewOnly(DerivedRuntimeResource.STAMINA.formula(state)));
    }
}
