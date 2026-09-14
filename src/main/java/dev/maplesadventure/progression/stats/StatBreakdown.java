package dev.maplesadventure.progression.stats;

/** Explainable sources; equipment and effects remain separate from character/attribute defense. */
public record StatBreakdown(double base, double attribute, double equipment, double effect) {
    public static final StatBreakdown ZERO = new StatBreakdown(0, 0, 0, 0);

    public StatBreakdown {
        if (!Double.isFinite(base) || !Double.isFinite(attribute)
                || !Double.isFinite(equipment) || !Double.isFinite(effect)) {
            throw new IllegalArgumentException("Non-finite stat breakdown");
        }
    }

    public double total() { return base + attribute + equipment + effect; }
}
