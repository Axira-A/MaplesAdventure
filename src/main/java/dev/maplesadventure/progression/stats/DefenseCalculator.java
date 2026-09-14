package dev.maplesadventure.progression.stats;

import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.PlayerAttributeState;
import java.util.Map;

/** Low-strength base-character defense previews. Armor remains a separate, future dominant contribution. */
public final class DefenseCalculator {
    static void calculate(PlayerAttributeState a, Map<CharacterStat, CharacterStatValue> target) {
        double vig = points(a, Attribute.VIGOR), end = points(a, Attribute.ENDURANCE);
        double str = points(a, Attribute.STRENGTH), dex = points(a, Attribute.DEXTERITY);
        double mind = points(a, Attribute.MIND), intelligence = points(a, Attribute.INTELLIGENCE);
        double faith = points(a, Attribute.FAITH);

        put(target, CharacterStat.PHYSICAL_DEFENSE, 10, vig * .25 + str * .15 + end * .10);
        put(target, CharacterStat.STRIKE_DEFENSE, 10, str * .30 + end * .10 + vig * .05);
        put(target, CharacterStat.SLASH_DEFENSE, 10, dex * .25 + vig * .10);
        put(target, CharacterStat.PIERCE_DEFENSE, 10, dex * .30 + str * .05);
        put(target, CharacterStat.MAGIC_DEFENSE, 10, intelligence * .35 + mind * .05);
        put(target, CharacterStat.FIRE_DEFENSE, 10, faith * .25 + vig * .10);
        put(target, CharacterStat.LIGHTNING_DEFENSE, 10, intelligence * .25 + dex * .10);
        put(target, CharacterStat.ICE_DEFENSE, 10, intelligence * .25 + end * .10);
        put(target, CharacterStat.HOLY_DEFENSE, 10, faith * .35);
    }

    private static double points(PlayerAttributeState attributes, Attribute attribute) {
        return Math.max(0, attributes.get(attribute) - 5);
    }

    private static void put(Map<CharacterStat, CharacterStatValue> target, CharacterStat stat,
                            double base, double attribute) {
        target.put(stat, CharacterStatValue.previewOnly(new StatBreakdown(base, attribute, 0, 0)));
    }

    private DefenseCalculator() {}
}
