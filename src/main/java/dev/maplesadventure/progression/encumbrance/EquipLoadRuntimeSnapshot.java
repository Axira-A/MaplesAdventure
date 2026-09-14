package dev.maplesadventure.progression.encumbrance;

/** Compact server-authored context used for character UI and END level-up previews. */
public record EquipLoadRuntimeSnapshot(double currentLoad, boolean available,
                                       EquipLoadTier tier, DodgeMode currentDodgeMode,
                                       EncumbrancePolicySnapshot policy) {
    public EquipLoadRuntimeSnapshot {
        if (!Double.isFinite(currentLoad) || currentLoad < 0.0D || tier == null || currentDodgeMode == null
                || policy == null)
            throw new IllegalArgumentException("Invalid equipment-load snapshot");
    }
    public EquipLoadRuntimeSnapshot(double currentLoad, boolean available,
                                    EquipLoadTier tier, DodgeMode currentDodgeMode) {
        this(currentLoad, available, tier, currentDodgeMode, EncumbrancePolicySnapshot.fromServerConfig());
    }
    public static EquipLoadRuntimeSnapshot unavailable() {
        return new EquipLoadRuntimeSnapshot(0.0D, false, EquipLoadTier.NORMAL, DodgeMode.NONE,
                EncumbrancePolicySnapshot.fromServerConfig());
    }
}
