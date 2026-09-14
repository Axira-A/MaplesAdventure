package dev.maplesadventure.progression.stats;

import java.util.Map;

/** Ratings are dimensionless 0..100, never a claimed damage percentage. */
final class SpellScalingCalculator {
    static void calculate(dev.maplesadventure.progression.PlayerAttributeState attributes, Map<CharacterStat, CharacterStatValue> target) {
        target.put(CharacterStat.SPELL_POWER, CharacterStatValue.unavailable());
        target.put(CharacterStat.INTELLIGENCE_SCALING, rating(attributes, dev.maplesadventure.progression.Attribute.INTELLIGENCE));
        target.put(CharacterStat.FAITH_SCALING, rating(attributes, dev.maplesadventure.progression.Attribute.FAITH));
        target.put(CharacterStat.ARCANE_SCALING, rating(attributes, dev.maplesadventure.progression.Attribute.ARCANE));
    }
    private static CharacterStatValue rating(dev.maplesadventure.progression.PlayerAttributeState attributes,
                                            dev.maplesadventure.progression.Attribute attribute) {
        return CharacterStatValue.active(new StatBreakdown(0,
                dev.maplesadventure.progression.OffensiveScalingCurve.evaluate(attributes.get(attribute)) * 100, 0, 0));
    }
    private SpellScalingCalculator() {}
}
