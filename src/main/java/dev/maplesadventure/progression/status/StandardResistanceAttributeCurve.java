package dev.maplesadventure.progression.status;

public final class StandardResistanceAttributeCurve {
    public static double evaluate(int stat) {
        return Math.clamp(stat - 30, 0, 10) * 3.0
                + Math.clamp(stat - 40, 0, 20) * .5 + Math.clamp(stat - 60, 0, 39) * .25;
    }
    public static double vitality(int arcane) {
        return Math.clamp(arcane - 5, 0, 10) + Math.clamp(arcane - 15, 0, 25) * .6
                + Math.clamp(arcane - 40, 0, 20) * .5 + Math.clamp(arcane - 60, 0, 39) * .25;
    }
    private StandardResistanceAttributeCurve() {}
}
