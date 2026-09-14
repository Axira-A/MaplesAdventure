package dev.maplesadventure.progression.spell;

/** Snapshot of vanilla AttributeInstance evaluation excluding our own ADD_VALUE modifier.
 * Keep unclamped operands: subtracting a bonus from the final clamped value is not equivalent. */
public record SpellPowerContext(double additiveBase, double multiplication, double minimum, double maximum,
        double globalPower, double observedSchoolPower, boolean available) {
    public SpellPowerContext {
        for (double v : new double[]{additiveBase, multiplication, minimum, maximum, globalPower, observedSchoolPower})
            if (!Double.isFinite(v)) throw new IllegalArgumentException("Non-finite spell power context");
        if (minimum > maximum) throw new IllegalArgumentException("Invalid attribute range");
    }
    public double schoolPower(double bonus) {
        return Math.clamp((additiveBase + bonus) * multiplication, minimum, maximum);
    }
    public double effectivePower(double bonus) { return schoolPower(bonus) * globalPower; }
    public static SpellPowerContext unavailable() { return new SpellPowerContext(1, 1, -100, 100, 1, 1, false); }
}
