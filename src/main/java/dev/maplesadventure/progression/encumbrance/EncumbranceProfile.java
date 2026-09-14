package dev.maplesadventure.progression.encumbrance;

import dev.maplesadventure.config.EquipLoadConfig;

/** The single balance table consumed by movement, stamina, dodge and UI code. */
public record EncumbranceProfile(double movementMultiplier, double staminaRegenMultiplier,
                                 double staminaCostMultiplier, DodgeMode dodgeMode,
                                 double dodgeDistanceMultiplier, boolean canSprint,
                                 boolean canDodge) {
    public EncumbranceProfile {
        if (!Double.isFinite(movementMultiplier) || !Double.isFinite(staminaRegenMultiplier)
                || !Double.isFinite(staminaCostMultiplier) || !Double.isFinite(dodgeDistanceMultiplier)
                || movementMultiplier < 0.0D || staminaRegenMultiplier < 0.0D
                || staminaCostMultiplier < 0.0D || dodgeDistanceMultiplier < 0.0D
                || dodgeMode == null) throw new IllegalArgumentException("Invalid encumbrance profile");
    }

    public static EncumbranceProfile forTier(EquipLoadTier tier) {
        return switch (tier) {
            case LIGHT -> new EncumbranceProfile(1.0D + EquipLoadConfig.lightMovementBonus(),
                    EquipLoadConfig.lightStaminaRegenMultiplier(),
                    EquipLoadConfig.lightStaminaCostMultiplier(), DodgeMode.STEP, 1.0D, true, true);
            case NORMAL -> new EncumbranceProfile(1.0D, 1.0D, 1.0D,
                    DodgeMode.ROLL, 1.0D, true, true);
            case HEAVY -> new EncumbranceProfile(1.0D - EquipLoadConfig.heavyMovementPenalty(),
                    EquipLoadConfig.heavyStaminaRegenMultiplier(),
                    EquipLoadConfig.heavyStaminaCostMultiplier(), DodgeMode.ROLL,
                    EquipLoadConfig.heavyDodgeDistanceMultiplier(), true, true);
            case OVERLOADED -> new EncumbranceProfile(1.0D - EquipLoadConfig.overloadedMovementPenalty(),
                    EquipLoadConfig.overloadedStaminaRegenMultiplier(),
                    EquipLoadConfig.overloadedStaminaCostMultiplier(), DodgeMode.NONE, 0.0D, false, false);
        };
    }

    public String dodgeDescriptionKey() {
        return dodgeMode == DodgeMode.ROLL && dodgeDistanceMultiplier < 1.0D
                ? "encumbrance.maplesadventure.dodge.short_roll" : dodgeMode.translationKey();
    }
}
