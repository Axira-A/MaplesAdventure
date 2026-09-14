package dev.maplesadventure.progression.stats;

import java.util.Locale;

public enum CharacterStatSection {
    CORE,
    EQUIPMENT,
    OFFENSE,
    DEFENSE,
    ELEMENTAL,
    RESISTANCE,
    MAGIC;

    public String translationKey() {
        return "screen.maplesadventure.character_stats.section." + name().toLowerCase(Locale.ROOT);
    }
}
