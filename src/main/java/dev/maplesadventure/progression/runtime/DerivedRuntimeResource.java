package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.DerivedStatCalculator;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.stats.CharacterStat;

/** The three progression resources that may be backed by a live game/mod attribute. */
public enum DerivedRuntimeResource {
    HEALTH(CharacterStat.MAX_HEALTH, 20.0D) {
        @Override public double formula(PlayerAttributeState state) {
            return DerivedStatCalculator.maxHealth(state.get(Attribute.VIGOR));
        }
    },
    MANA(CharacterStat.MAX_MANA, 100.0D) {
        @Override public double formula(PlayerAttributeState state) {
            return DerivedStatCalculator.mana(state.get(Attribute.MIND));
        }
    },
    STAMINA(CharacterStat.MAX_STAMINA, 20.0D) {
        @Override public double formula(PlayerAttributeState state) {
            return DerivedStatCalculator.stamina(state.get(Attribute.ENDURANCE));
        }
    };

    private final CharacterStat stat;
    private final double progressionBaseline;

    DerivedRuntimeResource(CharacterStat stat, double progressionBaseline) {
        this.stat = stat;
        this.progressionBaseline = progressionBaseline;
    }

    public CharacterStat stat() { return stat; }
    public double progressionBaseline() { return progressionBaseline; }
    public abstract double formula(PlayerAttributeState state);
}
