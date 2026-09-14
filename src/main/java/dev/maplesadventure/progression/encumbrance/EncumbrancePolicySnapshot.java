package dev.maplesadventure.progression.encumbrance;

import dev.maplesadventure.config.EquipLoadConfig;

/** Server-authored thresholds and balance profiles used by both runtime and client previews. */
public record EncumbrancePolicySnapshot(double lightThreshold, double heavyThreshold,
                                        double overloadedThreshold, EncumbranceProfile light,
                                        EncumbranceProfile normal, EncumbranceProfile heavy,
                                        EncumbranceProfile overloaded) {
    public EncumbrancePolicySnapshot {
        if (!Double.isFinite(lightThreshold) || !Double.isFinite(heavyThreshold)
                || !Double.isFinite(overloadedThreshold) || lightThreshold < 0.0D
                || lightThreshold > heavyThreshold || heavyThreshold > overloadedThreshold
                || light == null || normal == null || heavy == null || overloaded == null)
            throw new IllegalArgumentException("Invalid encumbrance policy");
    }

    public static EncumbrancePolicySnapshot fromServerConfig() {
        return new EncumbrancePolicySnapshot(EquipLoadConfig.lightThreshold(), EquipLoadConfig.heavyThreshold(),
                EquipLoadConfig.overloadedThreshold(), EncumbranceProfile.forTier(EquipLoadTier.LIGHT),
                EncumbranceProfile.forTier(EquipLoadTier.NORMAL), EncumbranceProfile.forTier(EquipLoadTier.HEAVY),
                EncumbranceProfile.forTier(EquipLoadTier.OVERLOADED));
    }

    public EquipLoadTier tier(double ratio) {
        if (!Double.isFinite(ratio) || ratio < 0.0D) return EquipLoadTier.NORMAL;
        if (ratio < lightThreshold) return EquipLoadTier.LIGHT;
        if (ratio < heavyThreshold) return EquipLoadTier.NORMAL;
        if (ratio <= overloadedThreshold) return EquipLoadTier.HEAVY;
        return EquipLoadTier.OVERLOADED;
    }

    public EncumbranceProfile profile(EquipLoadTier tier) {
        return switch (tier) {
            case LIGHT -> light;
            case NORMAL -> normal;
            case HEAVY -> heavy;
            case OVERLOADED -> overloaded;
        };
    }
}
