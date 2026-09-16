package dev.maplesadventure.progression.status;

public record StatusResistance(double threshold, boolean immune, double procDamageMultiplier) {
    public static final StatusResistance DEFAULT = new StatusResistance(100, false, 1);
    public StatusResistance {
        bounded(threshold, .001, 100000);
        bounded(procDamageMultiplier, 0, 10);
    }
    public static double bounded(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Status value out of bounds");
        return value;
    }
}
