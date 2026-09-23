package dev.maplesadventure.progression.stats;

import java.util.Map;

/** Same pure calculator as server gameplay, including upgrade drafts. */
final class ResistanceCalculator {
    static void calculate(dev.maplesadventure.progression.PlayerAttributeState attributes,
                          Map<CharacterStat, CharacterStatValue> target) {
        calculate(attributes, dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot.EMPTY, target);
    }
    static void calculate(dev.maplesadventure.progression.PlayerAttributeState attributes,
                          dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot armor,
                          Map<CharacterStat, CharacterStatValue> target) {
        dev.maplesadventure.progression.status.PlayerStatusResistanceCalculator.calculate(attributes, armor).values().forEach((type,value)-> {
            CharacterStat stat=switch(type) {
                case IMMUNITY -> CharacterStat.IMMUNITY;
                case ROBUSTNESS -> CharacterStat.ROBUSTNESS;
                case FOCUS -> CharacterStat.FOCUS;
                case VITALITY -> CharacterStat.VITALITY;
            };
            target.put(stat,CharacterStatValue.active(new StatBreakdown(value.base(),value.level()+value.attribute(),value.equipment(),value.effect())));
        });
    }
    private ResistanceCalculator() {}
}
