package dev.maplesadventure.progression.stats;

import java.util.Map;

/** Initializes absent equipment only. withWeapons mirrors the canonical WeaponDamageBundle total; no second AR formula. */
final class WeaponAttackCalculator {
    static void calculate(Map<CharacterStat, CharacterStatValue> target) {
        target.put(CharacterStat.MAIN_HAND_ATTACK, CharacterStatValue.unavailable());
        target.put(CharacterStat.OFF_HAND_ATTACK, CharacterStatValue.unavailable());
    }
    private WeaponAttackCalculator() {}
}
