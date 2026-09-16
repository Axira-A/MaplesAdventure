package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.stats.*;
import org.junit.jupiter.api.Test;

class CharacterStatsCalculatorTest {
    @Test void truthfullySeparatesPreviewAndUnavailableSystems() {
        CharacterStatsSnapshot stats = CharacterStatCalculator.calculate(PlayerAttributeState.defaultsState());
        assertEquals(5, stats.level());
        assertEquals(20D, stats.value(CharacterStat.MAX_HEALTH).value(), 0.0001D);
        assertEquals(StatImplementationState.PREVIEW_ONLY,
                stats.value(CharacterStat.MAX_HEALTH).implementation());
        assertEquals(40D, stats.equipLoad().maxWeight().value(), 0.0001D);
        assertEquals(StatImplementationState.UNAVAILABLE, stats.equipLoad().currentWeight().implementation());
        assertEquals(StatImplementationState.UNAVAILABLE,
                stats.attackProfile().mainHand().implementation());
        assertEquals(StatImplementationState.UNAVAILABLE,
                stats.spellScaling().power().implementation());
    }

    @Test void equipLoadCurveUsesDiminishingReturns() {
        assertEquals(40D, EquipLoadCalculator.maxEquipLoad(5), 0.0001D);
        assertEquals(55D, EquipLoadCalculator.maxEquipLoad(20), 0.0001D);
        assertEquals(70D, EquipLoadCalculator.maxEquipLoad(40), 0.0001D);
        assertEquals(80D, EquipLoadCalculator.maxEquipLoad(60), 0.0001D);
        assertEquals(90D, EquipLoadCalculator.maxEquipLoad(99), 0.0001D);
    }

    @Test void previewDefenseKeepsExplainableSources() {
        PlayerAttributeState state = PlayerAttributeState.defaultsState().with(Attribute.VIGOR, 8, 99);
        CharacterStatValue physical = CharacterStatCalculator.calculate(state).value(CharacterStat.PHYSICAL_DEFENSE);
        assertEquals(StatImplementationState.ACTIVE, physical.implementation());
        assertEquals(10D, physical.breakdown().base(), 0.0001D);
        assertEquals(.75D, physical.breakdown().attribute(), 0.0001D);
        assertEquals(0D, physical.breakdown().equipment(), 0.0001D);
        assertEquals(physical.value(), physical.breakdown().total(), 0.0001D);
    }
}
