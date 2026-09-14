package dev.maplesadventure.progression.stats;

import java.util.Map;

/** Status buildup/resistance has no authoritative formula in this round. */
final class ResistanceCalculator {
    static void calculate(Map<CharacterStat, CharacterStatValue> target) {
        target.put(CharacterStat.POISON_RESISTANCE, CharacterStatValue.unavailable());
        target.put(CharacterStat.BLEED_RESISTANCE, CharacterStatValue.unavailable());
        target.put(CharacterStat.FROST_RESISTANCE, CharacterStatValue.unavailable());
        target.put(CharacterStat.SCARLET_RESISTANCE, CharacterStatValue.unavailable());
    }
    private ResistanceCalculator() {}
}
