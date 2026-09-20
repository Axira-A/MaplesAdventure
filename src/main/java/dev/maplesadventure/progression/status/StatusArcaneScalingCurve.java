package dev.maplesadventure.progression.status;

/** Status buildup curve, deliberately independent of offensive AR. */
public final class StatusArcaneScalingCurve {
    public static double evaluate(int arcane) {
        int stat = Math.clamp(arcane, 5, 99);
        if (stat <= 25) return (stat - 5) * .10 / 20;
        if (stat <= 45) return .10 + (stat - 25) * .65 / 20;
        if (stat <= 60) return .75 + (stat - 45) * .15 / 15;
        return .90 + (stat - 60) * .10 / 39;
    }
    private StatusArcaneScalingCurve() {}
}
