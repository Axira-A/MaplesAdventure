package dev.maplesadventure.progression;

import java.util.Map;

/** One pure plan shared by UI and the server transaction. No formula duplication. */
public final class LevelUpPreviewCalculator {
    public record Preview(PlayerAttributeState state, int level, double maxHealth, double mana,
                          double stamina, dev.maplesadventure.progression.stats.CharacterStatsSnapshot characterStats,
                          int points, long totalCost, long remainingXp) {}
    public static Preview calculate(AttributeSnapshot baseline, Map<Attribute, Integer> deltas,
                                    int xp, int cap, double multiplier) {
        Preview plan = calculate(baseline.state(), baseline.runtimeResources(), baseline.equipLoad(), deltas, xp, cap, multiplier);
        var stats = dev.maplesadventure.progression.stats.CharacterStatsService.preview(plan.state(),
                baseline.runtimeResources(), baseline.equipLoad(), baseline.spellSchools())
                .withWeapons(baseline.weapons().evaluate(plan.state()));
        return new Preview(plan.state(), plan.level(), plan.maxHealth(), plan.mana(), plan.stamina(), stats,
                plan.points(), plan.totalCost(), plan.remainingXp());
    }
    public static Preview calculate(PlayerAttributeState baseline, Map<Attribute, Integer> deltas,
                                    int xp, int cap, double multiplier) {
        return finish(baseline, dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot
                .progressionOnly(baseline), dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable(),
                deltas, xp, cap, multiplier);
    }

    private static Preview calculate(PlayerAttributeState baseline,
                                     dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtime,
                                     dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
                                     Map<Attribute, Integer> deltas, int xp, int cap, double multiplier) {
        return finish(baseline, runtime, equipLoad, deltas, xp, cap, multiplier);
    }

    private static Preview finish(PlayerAttributeState baseline,
                                  dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot runtime,
                                  dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot equipLoad,
                                  Map<Attribute, Integer> deltas, int xp, int cap, double multiplier) {
        if (cap < 5 || cap > 99 || xp < 0 || deltas == null || deltas.size() > Attribute.values().length)
            throw new IllegalArgumentException("Invalid upgrade plan");
        PlayerAttributeState after = baseline.cleanCopy();
        int points = 0;
        for (var entry : deltas.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null || entry.getValue() < 0
                    || entry.getValue() > 94) throw new IllegalArgumentException("Invalid delta");
        }
        for (Attribute attribute : Attribute.values()) {
            int delta = deltas.getOrDefault(attribute, 0);
            if ((long) baseline.get(attribute) + delta > cap) throw new IllegalArgumentException("Attribute cap");
            points += delta;
            after = after.with(attribute, baseline.get(attribute) + delta, cap);
        }
        long cost = AttributeProgression.costForLevels(AttributeProgression.level(baseline), points, multiplier);
        var characterStats = dev.maplesadventure.progression.stats.CharacterStatsService.preview(after, runtime, equipLoad);
        return new Preview(after, AttributeProgression.level(after),
                characterStats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH).value(),
                characterStats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_MANA).value(),
                characterStats.value(dev.maplesadventure.progression.stats.CharacterStat.MAX_STAMINA).value(), characterStats,
                points, cost, (long) xp - cost);
    }
    private LevelUpPreviewCalculator() {}
}
