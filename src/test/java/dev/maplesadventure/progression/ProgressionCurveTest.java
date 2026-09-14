package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProgressionCurveTest {
    @Test
    void levelIsDerivedOnlyFromInvestedAttributes() {
        assertEquals(5, ProgressionCurve.adventureLevel(5, 5, 5, 5, 5, 5, 5, 5));
        assertEquals(20, ProgressionCurve.adventureLevel(20, 5, 5, 5, 5, 5, 5, 5));
        assertEquals(757, ProgressionCurve.adventureLevel(99, 99, 99, 99, 99, 99, 99, 99));
    }

    @Test
    void nextLevelCostMatchesTheSingleAuthoritativeCurve() {
        assertEquals(118L, ProgressionCurve.baseCostForNextLevel(5));
        assertEquals(172L, ProgressionCurve.baseCostForNextLevel(10));
        assertEquals(336L, ProgressionCurve.baseCostForNextLevel(20));
        assertEquals(928L, ProgressionCurve.baseCostForNextLevel(40));
        assertEquals(1952L, ProgressionCurve.baseCostForNextLevel(60));
        assertEquals(5680L, ProgressionCurve.baseCostForNextLevel(100));
    }

    @Test
    void derivedStatsRespectEverySoftCap() {
        assertEquals(20.0D, DerivedStatCalculator.maxHealth(5), 0.0001D);
        assertEquals(35.0D, DerivedStatCalculator.maxHealth(20), 0.0001D);
        assertEquals(60.0D, DerivedStatCalculator.maxHealth(40), 0.0001D);
        assertEquals(75.0D, DerivedStatCalculator.maxHealth(60), 0.0001D);
        assertEquals(84.75D, DerivedStatCalculator.maxHealth(99), 0.0001D);

        assertEquals(100.0D, DerivedStatCalculator.mana(5), 0.0001D);
        assertEquals(190.0D, DerivedStatCalculator.mana(20), 0.0001D);
        assertEquals(290.0D, DerivedStatCalculator.mana(40), 0.0001D);
        assertEquals(350.0D, DerivedStatCalculator.mana(60), 0.0001D);
        assertEquals(398.75D, DerivedStatCalculator.mana(99), 0.0001D);

        // END is normalized against Elden Ring END 10..99 and divided by the 4.8 stamina scale.
        assertEquals(20.0D, DerivedStatCalculator.stamina(5), 0.0001D);
        assertEquals(25.0421099291D, DerivedStatCalculator.stamina(20), 0.0001D);
        assertEquals(30.4454787234D, DerivedStatCalculator.stamina(40), 0.0001D);
        assertEquals(32.9166666667D, DerivedStatCalculator.stamina(60), 0.0001D);
        assertEquals(35.4166666667D, DerivedStatCalculator.stamina(99), 0.0001D);
    }

    @Test
    void offensivePreviewIsBoundedAndMonotonic() {
        assertEquals(0.0D, OffensiveScalingCurve.evaluate(5), 0.0001D);
        assertEquals(1.0D, OffensiveScalingCurve.evaluate(99), 0.0001D);
        assertTrue(OffensiveScalingCurve.evaluate(40) > OffensiveScalingCurve.evaluate(20));
    }
}
