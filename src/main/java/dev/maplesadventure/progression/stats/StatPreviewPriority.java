package dev.maplesadventure.progression.stats;

import java.util.*;

/** Selects a compact subset without allowing unavailable placeholders to consume space. */
public final class StatPreviewPriority {
    public record Comparison(CharacterStat stat, CharacterStatValue current,
                             CharacterStatValue preview, boolean changed) {}

    public static List<Comparison> select(CharacterStatsSnapshot current,
                                          CharacterStatsSnapshot preview, int limit) {
        if (current == null || preview == null || limit < 0) throw new IllegalArgumentException("Invalid preview");
        ArrayList<Comparison> candidates = new ArrayList<>();
        for (CharacterStat stat : CharacterStat.values()) {
            CharacterStatValue before = current.value(stat), after = preview.value(stat);
            if (!before.available() && !after.available()) continue;
            if(stat.section()==CharacterStatSection.RESISTANCE) {
                double levelDelta=dev.maplesadventure.progression.status.StatusLevelResistanceCurve.evaluate(preview.level())
                        -dev.maplesadventure.progression.status.StatusLevelResistanceCurve.evaluate(current.level());
                // Full stats retain all four; the compact panel suppresses incidental level-only changes.
                if(Math.abs(after.value()-before.value()-levelDelta)<1e-8) continue;
            }
            if ((stat.section()==CharacterStatSection.DEFENSE || stat.section()==CharacterStatSection.ELEMENTAL)
                    && !after.differsFrom(before)) continue;
            candidates.add(new Comparison(stat, before, after, after.differsFrom(before)));
        }
        candidates.sort(Comparator.<Comparison, Boolean>comparing(Comparison::changed).reversed()
                .thenComparingInt(row -> row.stat.section()==CharacterStatSection.RESISTANCE?45:row.stat.compactPriority())
                .thenComparingInt(row -> row.stat.ordinal()));
        return List.copyOf(candidates.subList(0, Math.min(limit, candidates.size())));
    }

    private StatPreviewPriority() {}
}
