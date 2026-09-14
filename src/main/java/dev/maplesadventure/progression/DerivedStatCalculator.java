package dev.maplesadventure.progression;

import static dev.maplesadventure.progression.ProgressionCurve.Segment;
import dev.maplesadventure.progression.stamina.StaminaPolicy;

/** Pure derived-stat formulas shared by previews and runtime adapters. */
public final class DerivedStatCalculator {
    public static double maxHealth(int vigor) {
        return ProgressionCurve.piecewise(vigor, 20.0D,
                new Segment(20, 1.00D), new Segment(40, 1.25D),
                new Segment(60, 0.75D), new Segment(99, 0.25D));
    }

    public static double mana(int mind) {
        return ProgressionCurve.piecewise(mind, 100.0D,
                new Segment(20, 6.00D), new Segment(40, 5.00D),
                new Segment(60, 3.00D), new Segment(99, 1.25D));
    }

    /** Maples END 5 is the white-box baseline and maps to Elden Ring END 10 / 96 stamina. */
    public static double stamina(int endurance) {
        return StaminaPolicy.maxStamina(endurance);
    }

    private DerivedStatCalculator() {}
}
