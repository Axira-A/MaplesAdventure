package dev.maplesadventure.progression;

import static dev.maplesadventure.progression.ProgressionCurve.Segment;

/** Pure previews only; no third-party attributes are modified in Round 1. */
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

    public static double stamina(int endurance) {
        return ProgressionCurve.piecewise(endurance, 20.0D,
                new Segment(20, 0.80D), new Segment(40, 0.50D),
                new Segment(60, 0.25D), new Segment(99, 0.08D));
    }

    private DerivedStatCalculator() {}
}
