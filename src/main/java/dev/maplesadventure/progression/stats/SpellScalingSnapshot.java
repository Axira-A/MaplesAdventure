package dev.maplesadventure.progression.stats;

/** Legacy compact ratings. Actual schools live in CharacterStatsSnapshot.spellSchools(). */
public record SpellScalingSnapshot(CharacterStatValue power, CharacterStatValue intelligence,
                                   CharacterStatValue faith, CharacterStatValue arcane) {}
