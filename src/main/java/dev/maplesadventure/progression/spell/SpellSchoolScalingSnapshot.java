package dev.maplesadventure.progression.spell;

import dev.maplesadventure.progression.PlayerAttributeState;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public record SpellSchoolScalingSnapshot(Map<ResourceLocation, SpellSchoolStat> schools) {
    public SpellSchoolScalingSnapshot {
        if (schools.size() > SpellSchoolScalingRegistry.MAX_SCHOOLS) throw new IllegalArgumentException("Too many schools");
        schools = Collections.unmodifiableMap(new TreeMap<>(schools));
    }
    public static SpellSchoolScalingSnapshot empty() { return new SpellSchoolScalingSnapshot(Map.of()); }
    public SpellSchoolScalingSnapshot preview(PlayerAttributeState state) {
        Map<ResourceLocation, SpellSchoolStat> result = new LinkedHashMap<>();
        schools.forEach((id, value) -> result.put(id, value.preview(state)));
        return new SpellSchoolScalingSnapshot(result);
    }
    public List<SpellSchoolStat> changedFrom(SpellSchoolScalingSnapshot baseline) {
        return schools.values().stream().filter(s -> baseline.schools.containsKey(s.schoolId()))
                .filter(s -> Math.abs(s.progressionBonus() - baseline.schools.get(s.schoolId()).progressionBonus()) > 1e-8)
                .sorted(Comparator.<SpellSchoolStat>comparingDouble(s -> Math.abs(s.progressionBonus()
                        - baseline.schools.get(s.schoolId()).progressionBonus())).reversed()
                        .thenComparing(SpellSchoolStat::schoolId)).toList();
    }
}
