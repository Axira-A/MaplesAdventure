package dev.maplesadventure.progression.status;

/** Maples balance conversion, not Elden Ring's exact numerical formula. */
public final class StatusLevelResistanceCurve {
    public static double evaluate(int level) {
        return Math.clamp(level - 5, 0, 66) * .25
                + Math.clamp(level - 71, 0, 40) * .15 + Math.clamp(level - 111, 0, 50) * .10;
    }
    private StatusLevelResistanceCurve() {}
}
