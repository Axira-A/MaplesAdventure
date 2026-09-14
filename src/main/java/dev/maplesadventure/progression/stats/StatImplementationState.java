package dev.maplesadventure.progression.stats;

/** Whether a displayed number is authoritative gameplay data or only design-time information. */
public enum StatImplementationState {
    ACTIVE,
    PREVIEW_ONLY,
    UNAVAILABLE;

    public String translationKey() {
        return "screen.maplesadventure.character_stats.state." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
