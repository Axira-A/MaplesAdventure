package dev.maplesadventure.progression.stats;

import dev.maplesadventure.progression.encumbrance.DodgeMode;
import dev.maplesadventure.progression.encumbrance.EncumbranceProfile;
import dev.maplesadventure.progression.encumbrance.EquipLoadTier;

public record EquipLoadSnapshot(CharacterStatValue currentWeight, CharacterStatValue maxWeight,
                                CharacterStatValue ratio, EquipLoadTier tier,
                                EncumbranceProfile profile, DodgeMode currentDodgeMode) {
    public EquipLoadSnapshot {
        if (currentWeight == null || maxWeight == null || ratio == null || tier == null
                || profile == null || currentDodgeMode == null) throw new IllegalArgumentException("Invalid load view");
    }
    public static EquipLoadSnapshot unavailable(CharacterStatValue maximum) {
        EquipLoadTier tier = EquipLoadTier.NORMAL;
        return new EquipLoadSnapshot(CharacterStatValue.unavailable(), maximum,
                CharacterStatValue.unavailable(), tier, EncumbranceProfile.forTier(tier), DodgeMode.NONE);
    }
}
