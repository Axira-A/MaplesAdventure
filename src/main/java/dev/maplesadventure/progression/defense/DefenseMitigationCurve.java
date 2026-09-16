package dev.maplesadventure.progression.defense;

public final class DefenseMitigationCurve {
    public static final double DEFAULT_PRESSURE = .35;
    public static final double MIN_PENETRATION = .10, MIN_MULTIPLIER = .05, MAX_MULTIPLIER = 1.50;
    public static double penetration(double attackRating, double defense) {
        return penetration(attackRating, defense, DEFAULT_PRESSURE);
    }
    public static double penetration(double attackRating, double defense, double pressure) {
        if (!Double.isFinite(attackRating) || attackRating < 0 || !Double.isFinite(pressure) || pressure < .01 || pressure > 2)
            throw new IllegalArgumentException("Invalid attack rating/defense pressure");
        new ChannelDefense(defense, 0);
        // In particular 0 AR / 0 defense is identity, not NaN.
        return defense == 0 ? 1 : Math.clamp(attackRating / (attackRating + defense * pressure), MIN_PENETRATION, 1);
    }
    public static double absorptionMultiplier(double absorption) {
        new ChannelDefense(0, absorption);
        return 1 - absorption;
    }
    public static double finalChannelMultiplier(double attackRating, ChannelDefense defense, double pressure) {
        return Math.clamp(penetration(attackRating, defense.defense(), pressure) * absorptionMultiplier(defense.absorption()),
                MIN_MULTIPLIER, MAX_MULTIPLIER);
    }
    private DefenseMitigationCurve() {}
}
