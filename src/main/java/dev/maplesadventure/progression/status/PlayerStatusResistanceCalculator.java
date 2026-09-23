package dev.maplesadventure.progression.status;

import java.util.EnumMap;
import dev.maplesadventure.progression.*;

/** Shared by authoritative gameplay and drafts; no entity or client dependencies. */
public final class PlayerStatusResistanceCalculator {
    public static final double BASE_RESISTANCE = 160;
    public static PlayerStatusResistanceSnapshot calculate(PlayerAttributeState state) {
        return calculate(state, dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot.EMPTY);
    }
    public static PlayerStatusResistanceSnapshot calculate(PlayerAttributeState state,
            dev.maplesadventure.progression.armor.ArmorEquipmentSnapshot armor) {
        var values = new EnumMap<StatusResistanceType, PlayerStatusResistanceSnapshot.Breakdown>(StatusResistanceType.class);
        double level = StatusLevelResistanceCurve.evaluate(AttributeProgression.level(state));
        for (var type : StatusResistanceType.values()) {
            int stat = state.get(type.attribute());
            double attribute = type == StatusResistanceType.VITALITY ? StandardResistanceAttributeCurve.vitality(stat)
                    : StandardResistanceAttributeCurve.evaluate(stat);
            values.put(type, new PlayerStatusResistanceSnapshot.Breakdown(BASE_RESISTANCE, level, attribute, armor.resistance(type), 0));
        }
        return new PlayerStatusResistanceSnapshot(values);
    }
    private PlayerStatusResistanceCalculator() {}
}
