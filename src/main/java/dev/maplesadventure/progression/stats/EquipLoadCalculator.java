package dev.maplesadventure.progression.stats;

import static dev.maplesadventure.progression.ProgressionCurve.Segment;
import dev.maplesadventure.progression.Attribute;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.ProgressionCurve;
import java.util.Map;
import dev.maplesadventure.progression.encumbrance.*;

/** Diminishing END capacity curve plus the shared runtime load/tier projection. */
public final class EquipLoadCalculator {
    public static double maxEquipLoad(int endurance) {
        return ProgressionCurve.piecewise(endurance, 40.0D,
                new Segment(20, 1.0D), new Segment(40, 0.75D),
                new Segment(60, 0.50D), new Segment(99, 10.0D / 39.0D));
    }

    static EquipLoadSnapshot calculate(PlayerAttributeState attributes, EquipLoadRuntimeSnapshot runtime,
                                        Map<CharacterStat, CharacterStatValue> target) {
        CharacterStatValue maximum = runtime.available()
                ? CharacterStatValue.active(new StatBreakdown(maxEquipLoad(attributes.get(Attribute.ENDURANCE)), 0, 0, 0))
                : CharacterStatValue.previewOnly(maxEquipLoad(attributes.get(Attribute.ENDURANCE)));
        if (!runtime.available()) {
            target.put(CharacterStat.CURRENT_EQUIP_LOAD, CharacterStatValue.unavailable());
            target.put(CharacterStat.MAX_EQUIP_LOAD, maximum);
            target.put(CharacterStat.EQUIP_LOAD_RATIO, CharacterStatValue.unavailable());
            return EquipLoadSnapshot.unavailable(maximum);
        }
        double current = runtime.currentLoad();
        double ratioValue = maximum.value() <= 0.0D ? 0.0D : current / maximum.value();
        CharacterStatValue currentValue = CharacterStatValue.active(new StatBreakdown(0, 0, current, 0));
        CharacterStatValue ratio = CharacterStatValue.active(new StatBreakdown(ratioValue, 0, 0, 0));
        EquipLoadTier tier = runtime.policy().tier(ratioValue);
        target.put(CharacterStat.CURRENT_EQUIP_LOAD, currentValue);
        target.put(CharacterStat.MAX_EQUIP_LOAD, maximum);
        target.put(CharacterStat.EQUIP_LOAD_RATIO, ratio);
        return new EquipLoadSnapshot(currentValue, maximum, ratio, tier,
                runtime.policy().profile(tier), runtime.currentDodgeMode());
    }

    private EquipLoadCalculator() {}
}
