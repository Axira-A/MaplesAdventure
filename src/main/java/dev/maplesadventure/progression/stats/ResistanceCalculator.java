package dev.maplesadventure.progression.stats;

import java.util.Map;

/** Renders only the authoritative server resistance snapshot; attributes never invent thresholds. */
final class ResistanceCalculator {
    static void calculate(Map<dev.maplesadventure.progression.status.StatusEffectType,Double> thresholds,
                          Map<CharacterStat, CharacterStatValue> target) {
        thresholds.forEach((type,value)-> {
            new dev.maplesadventure.progression.status.StatusResistance(value,false,1);
            CharacterStat stat=switch(type) {
                case BLEED -> CharacterStat.BLEED_RESISTANCE;
                case POISON -> CharacterStat.POISON_RESISTANCE;
                case SCARLET_ROT -> CharacterStat.SCARLET_RESISTANCE;
                case FROSTBITE -> CharacterStat.FROST_RESISTANCE;
            };
            target.put(stat,CharacterStatValue.active(new StatBreakdown(value,0,0,0)));
        });
    }
    private ResistanceCalculator() {}
}
