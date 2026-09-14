package dev.maplesadventure.progression.stats;

import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.DerivedStatCalculator;
import dev.maplesadventure.progression.PlayerAttributeState;
import java.util.Map;
import dev.maplesadventure.progression.runtime.DerivedRuntimeResource;
import dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot;

/** Existing progression curves projected through server-authored runtime contexts. */
final class ResourceStatCalculator {
    static void calculate(PlayerAttributeState attributes, RuntimeResourceSnapshot runtime,
                          Map<CharacterStat, CharacterStatValue> target) {
        put(attributes, runtime, DerivedRuntimeResource.HEALTH, target);
        put(attributes, runtime, DerivedRuntimeResource.MANA, target);
        put(attributes, runtime, DerivedRuntimeResource.STAMINA, target);
    }

    private static void put(PlayerAttributeState attributes, RuntimeResourceSnapshot runtime,
                            DerivedRuntimeResource resource, Map<CharacterStat, CharacterStatValue> target) {
        target.put(resource.stat(), runtime.value(resource).project(
                resource.formula(attributes), resource.progressionBaseline()));
    }

    private ResourceStatCalculator() {}
}
