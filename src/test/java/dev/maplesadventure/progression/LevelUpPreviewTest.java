package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LevelUpPreviewTest {
    @Test void draftDoesNotChangeBaselineAndCanOnlyRetractNewPoints() {
        var baseline = PlayerAttributeState.defaultsState();
        var draft = new LevelUpDraft();
        draft.adjust(Attribute.VIGOR, 3, baseline, 99);
        assertEquals(5, baseline.get(Attribute.VIGOR));
        assertEquals(3, draft.get(Attribute.VIGOR));
        var preview = LevelUpPreviewCalculator.calculate(baseline, draft.deltas(), 1000, 99, 1.0);
        assertEquals(8, preview.level());
        assertEquals(23.0, preview.maxHealth(), 0.00001);
        assertEquals(382, preview.totalCost());
        draft.adjust(Attribute.VIGOR, -1, baseline, 99);
        assertEquals(245, LevelUpPreviewCalculator.calculate(baseline, draft.deltas(), 1000, 99, 1.0).totalCost());
        draft.adjust(Attribute.VIGOR, -99, baseline, 99);
        assertEquals(0, draft.get(Attribute.VIGOR));
        assertEquals(5, baseline.get(Attribute.VIGOR));
    }
    @Test void priceUsesSuccessiveLevelsAndServerMultiplier() {
        var baseline = PlayerAttributeState.defaultsState().with(Attribute.VIGOR, 20, 99);
        var plan = LevelUpPreviewCalculator.calculate(baseline,
                Map.of(Attribute.VIGOR, 2, Attribute.ENDURANCE, 1), 5000, 99, 1.0);
        assertEquals(23, plan.level());
        assertEquals(1071, plan.totalCost());
        assertEquals(AttributeProgression.costForNextLevel(20, 1.0)
                + AttributeProgression.costForNextLevel(21, 1.0)
                + AttributeProgression.costForNextLevel(22, 1.0), plan.totalCost());
        assertEquals(3929, plan.remainingXp());
        assertNotEquals(AttributeProgression.costForNextLevel(20, 1.0) * 3, plan.totalCost());
        assertEquals(237, AttributeProgression.costForNextLevel(5, 2.001));
    }
    @Test void capAndMalformedDeltasAreRejectedWithoutChangingState() {
        var state = PlayerAttributeState.defaultsState();
        var capped = state.with(Attribute.VIGOR, 99, 99);
        var draft = new LevelUpDraft();
        draft.adjust(Attribute.VIGOR, 1, capped, 99);
        assertEquals(0, draft.get(Attribute.VIGOR));
        assertThrows(IllegalArgumentException.class, () -> LevelUpPreviewCalculator.calculate(
                capped, Map.of(Attribute.VIGOR, 1), 10000, 99, 1));
        assertThrows(IllegalArgumentException.class, () -> LevelUpPreviewCalculator.calculate(
                state, Map.of(Attribute.VIGOR, -1), 10000, 99, 1));
        assertThrows(IllegalArgumentException.class, () -> LevelUpPreviewCalculator.calculate(
                state, Map.of(Attribute.VIGOR, Integer.MAX_VALUE), 10000, 99, 1));
        assertEquals(5, state.get(Attribute.VIGOR));
    }
    @Test void insufficientXpIsVisibleWithoutMutationAndCostsStayBounded() {
        var state = PlayerAttributeState.defaultsState();
        var plan = LevelUpPreviewCalculator.calculate(state, Map.of(Attribute.VIGOR, 3), 0, 99, 1);
        assertTrue(plan.remainingXp() < 0);
        assertEquals(5, state.get(Attribute.VIGOR));
        assertEquals(0, LevelUpPreviewCalculator.calculate(state, Map.of(), 100, 99, 1).points());
        var all = new java.util.EnumMap<Attribute, Integer>(Attribute.class);
        for (Attribute a : Attribute.values()) all.put(a, 94);
        var max = LevelUpPreviewCalculator.calculate(state, all, Integer.MAX_VALUE, 99, 100);
        assertEquals(757, max.level());
        assertTrue(max.totalCost() > Integer.MAX_VALUE);
        assertTrue(max.remainingXp() < 0);
    }
}
