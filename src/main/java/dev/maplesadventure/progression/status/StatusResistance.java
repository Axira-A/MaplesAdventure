package dev.maplesadventure.progression.status;

public record StatusResistance(double threshold, boolean immune, double procDamageMultiplier,
        net.minecraft.resources.ResourceLocation correction, SleepResponse sleepResponse) {
    public StatusResistance(double threshold,boolean immune,double multiplier) {
        this(threshold,immune,multiplier,StatusResistanceCorrections.NONE,SleepResponse.STAGGER_ONLY);
    }
    public static final StatusResistance DEFAULT = new StatusResistance(160, false, 1);
    public StatusResistance {
        bounded(threshold, .001, 100000);
        bounded(procDamageMultiplier, 0, 10);
        java.util.Objects.requireNonNull(correction); java.util.Objects.requireNonNull(sleepResponse);
        if(correction.toString().length()>256) throw new IllegalArgumentException("Correction ID length");
    }
    public static double bounded(double value, double min, double max) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException("Status value out of bounds");
        return value;
    }
}
