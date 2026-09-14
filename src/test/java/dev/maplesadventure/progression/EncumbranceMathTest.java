package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;

import dev.maplesadventure.progression.encumbrance.*;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;
import dev.maplesadventure.progression.stats.CharacterStat;
import dev.maplesadventure.progression.stats.CharacterStatCalculator;
import dev.maplesadventure.progression.stats.StatImplementationState;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EncumbranceMathTest {
    @Test void exactTierBoundariesMatchDesign() {
        assertEquals(EquipLoadTier.LIGHT, EquipLoadTier.fromRatio(0.299D));
        assertEquals(EquipLoadTier.NORMAL, EquipLoadTier.fromRatio(0.30D));
        assertEquals(EquipLoadTier.NORMAL, EquipLoadTier.fromRatio(0.699D));
        assertEquals(EquipLoadTier.HEAVY, EquipLoadTier.fromRatio(0.70D));
        assertEquals(EquipLoadTier.HEAVY, EquipLoadTier.fromRatio(1.0D));
        assertEquals(EquipLoadTier.OVERLOADED, EquipLoadTier.fromRatio(1.001D));
    }

    @Test void normalProfileIsAnExactNeutralBaseline() {
        EncumbranceProfile normal = EncumbranceProfile.forTier(EquipLoadTier.NORMAL);
        assertEquals(1.0D, normal.movementMultiplier());
        assertEquals(1.0D, normal.staminaRegenMultiplier());
        assertEquals(1.0D, normal.staminaCostMultiplier());
        assertEquals(1.0D, normal.dodgeDistanceMultiplier());
        assertEquals(DodgeMode.ROLL, normal.dodgeMode());
    }

    @Test void endPreviewCanCrossHeavyToNormalWithoutMutatingBaseline() {
        PlayerAttributeState baseline = PlayerAttributeState.defaultsState().with(Attribute.ENDURANCE, 20, 99);
        AttributeSnapshot snapshot = new AttributeSnapshot(baseline, 20, 20, 100, 32,
                AttributeProgression.costForNextLevel(20, 1.0D), RuntimeResourceSnapshot.progressionOnly(baseline),
                new EquipLoadRuntimeSnapshot(39.0D, true, EquipLoadTier.HEAVY, DodgeMode.ROLL));
        var preview = LevelUpPreviewCalculator.calculate(snapshot, Map.of(Attribute.ENDURANCE, 15),
                100_000, 99, 1.0D);
        assertEquals(20, baseline.get(Attribute.ENDURANCE));
        assertEquals(35, preview.state().get(Attribute.ENDURANCE));
        assertEquals(EquipLoadTier.NORMAL, preview.characterStats().equipLoad().tier());
    }

    @Test void activeEquipmentContextProducesRealCurrentAndRatioValues() {
        var state = PlayerAttributeState.defaultsState();
        var stats = CharacterStatCalculator.calculate(state, RuntimeResourceSnapshot.progressionOnly(state),
                new EquipLoadRuntimeSnapshot(12.0D, true, EquipLoadTier.NORMAL, DodgeMode.STEP));
        assertEquals(12.0D, stats.value(CharacterStat.CURRENT_EQUIP_LOAD).value(), 0.000_001D);
        assertEquals(0.30D, stats.value(CharacterStat.EQUIP_LOAD_RATIO).value(), 0.000_001D);
        assertEquals(StatImplementationState.ACTIVE,
                stats.value(CharacterStat.EQUIP_LOAD_RATIO).implementation());
        assertEquals(EquipLoadTier.NORMAL, stats.equipLoad().tier());
    }

    @Test void levelUpPreviewUsesServerAuthoredThresholdsAndProfiles() {
        EncumbranceProfile light = new EncumbranceProfile(1.04D, 1.2D, 0.8D,
                DodgeMode.STEP, 1.0D, true, true);
        EncumbranceProfile normal = new EncumbranceProfile(1.0D, 1.0D, 1.0D,
                DodgeMode.ROLL, 1.0D, true, true);
        EncumbranceProfile heavy = new EncumbranceProfile(0.9D, 0.7D, 1.3D,
                DodgeMode.ROLL, 0.6D, true, true);
        EncumbranceProfile overloaded = new EncumbranceProfile(0.7D, 0.5D, 1.6D,
                DodgeMode.NONE, 0.0D, false, false);
        EncumbrancePolicySnapshot serverPolicy = new EncumbrancePolicySnapshot(
                0.40D, 0.80D, 1.0D, light, normal, heavy, overloaded);
        PlayerAttributeState baseline = PlayerAttributeState.defaultsState().with(Attribute.ENDURANCE, 20, 99);
        AttributeSnapshot snapshot = new AttributeSnapshot(baseline, 20, 20, 100, 32,
                AttributeProgression.costForNextLevel(20, 1.0D), RuntimeResourceSnapshot.progressionOnly(baseline),
                new EquipLoadRuntimeSnapshot(39.0D, true, EquipLoadTier.NORMAL, DodgeMode.ROLL, serverPolicy));
        var preview = LevelUpPreviewCalculator.calculate(snapshot, Map.of(), 100_000, 99, 1.0D);
        assertEquals(EquipLoadTier.NORMAL, preview.characterStats().equipLoad().tier());
        assertSame(normal, preview.characterStats().equipLoad().profile());
    }
}
