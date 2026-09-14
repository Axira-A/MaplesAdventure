package dev.maplesadventure.progression;

import static dev.maplesadventure.progression.ProgressionCurve.Segment;

/** Shared dimensionless diminishing-return rating for school progression and future weapon scaling. */
public final class OffensiveScalingCurve {
    private static final double MAX_RAW = 15.0D + 15.0D + 10.0D + 9.75D;

    public static double evaluate(int stat) {
        double raw = ProgressionCurve.piecewise(stat, 0.0D,
                new Segment(20, 1.00D), new Segment(40, 0.75D),
                new Segment(60, 0.50D), new Segment(99, 0.25D));
        return raw / MAX_RAW;
    }

    private OffensiveScalingCurve() {}
}
