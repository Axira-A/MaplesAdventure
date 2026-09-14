package dev.maplesadventure.progression.encumbrance;

import dev.maplesadventure.config.EquipLoadConfig;

/** Server-authored load bands. Boundary behavior is part of the saved gameplay contract. */
public enum EquipLoadTier {
    LIGHT,
    NORMAL,
    HEAVY,
    OVERLOADED;

    public static EquipLoadTier fromRatio(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0.0D) return NORMAL;
        if (ratio < EquipLoadConfig.lightThreshold()) return LIGHT;
        if (ratio < EquipLoadConfig.heavyThreshold()) return NORMAL;
        if (ratio <= EquipLoadConfig.overloadedThreshold()) return HEAVY;
        return OVERLOADED;
    }

    public String translationKey() {
        return "encumbrance.maplesadventure.tier." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
