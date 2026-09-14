package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.maplesadventure.progression.stamina.StaminaPolicy;
import org.junit.jupiter.api.Test;

class StaminaPolicyTest {
    @Test void whiteBoxBaselineMatchesEldenRingWretchScale() {
        assertEquals(20.0D, DerivedStatCalculator.stamina(5), 0.000_001D);
        assertEquals(96.0D / 4.8D, DerivedStatCalculator.stamina(5), 0.000_001D);
    }

    @Test void maximumEnduranceMatchesEldenRingEnd99() {
        assertEquals(170.0D / 4.8D, DerivedStatCalculator.stamina(99), 0.000_001D);
    }

    @Test void staminaCurveIsMonotonicAcrossMaplesRange() {
        double previous = DerivedStatCalculator.stamina(5);
        for (int endurance = 6; endurance <= 99; endurance++) {
            double current = DerivedStatCalculator.stamina(endurance);
            org.junit.jupiter.api.Assertions.assertTrue(current >= previous,
                    "stamina decreased at END " + endurance);
            previous = current;
        }
    }

    @Test void baseRegenKeepsExactEldenRingTimeScale() {
        assertEquals(9.375D, StaminaPolicy.BASE_REGEN_PER_SECOND, 0.000_001D);
        assertEquals(0.46875D, StaminaPolicy.BASE_REGEN_PER_TICK, 0.000_001D);
        assertEquals(96.0D / 45.0D,
                StaminaPolicy.BASE_MAX_STAMINA / StaminaPolicy.BASE_REGEN_PER_SECOND,
                0.000_001D);
    }

    @Test void debtCapUsesDs3SixtyPointScale() {
        assertEquals(12.5D, StaminaPolicy.MAX_STAMINA_DEBT, 0.000_001D);
    }
}
