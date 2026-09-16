package dev.maplesadventure.progression.stats;

import dev.maplesadventure.progression.AttributeProgression;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;
import java.util.EnumMap;
import dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot;

/** Small orchestrator; individual stat domains remain replaceable integrations. */
public final class CharacterStatCalculator {
    public static CharacterStatsSnapshot calculate(PlayerAttributeState attributes) {
        return calculate(attributes, RuntimeResourceSnapshot.progressionOnly(attributes),
                EquipLoadRuntimeSnapshot.unavailable());
    }

    public static CharacterStatsSnapshot calculate(PlayerAttributeState attributes,
                                                    RuntimeResourceSnapshot runtimeResources) {
        return calculate(attributes, runtimeResources, EquipLoadRuntimeSnapshot.unavailable());
    }

    public static CharacterStatsSnapshot calculate(PlayerAttributeState attributes,
                                                    RuntimeResourceSnapshot runtimeResources,
                                                    EquipLoadRuntimeSnapshot equipLoadRuntime) {
        return calculate(attributes, runtimeResources, equipLoadRuntime,
                dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot.empty());
    }

    public static CharacterStatsSnapshot calculate(PlayerAttributeState attributes, RuntimeResourceSnapshot runtimeResources,
            EquipLoadRuntimeSnapshot equipLoadRuntime, dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools) {
        return calculate(attributes,runtimeResources,equipLoadRuntime,spellSchools,java.util.Map.of());
    }
    public static CharacterStatsSnapshot calculate(PlayerAttributeState attributes, RuntimeResourceSnapshot runtimeResources,
            EquipLoadRuntimeSnapshot equipLoadRuntime, dev.maplesadventure.progression.spell.SpellSchoolScalingSnapshot spellSchools,
            java.util.Map<dev.maplesadventure.progression.status.StatusEffectType,Double> thresholds) {
        if (attributes == null) throw new IllegalArgumentException("Missing player attributes");
        if (runtimeResources == null) throw new IllegalArgumentException("Missing runtime resources");
        EnumMap<CharacterStat, CharacterStatValue> values = new EnumMap<>(CharacterStat.class);
        ResourceStatCalculator.calculate(attributes, runtimeResources, values);
        EquipLoadSnapshot equipLoad = EquipLoadCalculator.calculate(attributes, equipLoadRuntime, values);
        WeaponAttackCalculator.calculate(values);
        DefenseCalculator.calculate(attributes, values);
        ResistanceCalculator.calculate(thresholds, values);
        SpellScalingCalculator.calculate(attributes, values);
        return new CharacterStatsSnapshot(AttributeProgression.level(attributes), values, equipLoad, spellSchools.preview(attributes));
    }

    private CharacterStatCalculator() {}
}
