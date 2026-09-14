package dev.maplesadventure.progression.stats;

/** A value plus its truthfulness contract. UNAVAILABLE deliberately carries no fabricated number. */
public record CharacterStatValue(double value, StatImplementationState implementation,
                                 StatBreakdown breakdown) {
    private static final double EPSILON = 0.000_001D;

    public CharacterStatValue {
        if (implementation == null || breakdown == null || !Double.isFinite(value))
            throw new IllegalArgumentException("Invalid character stat value");
    }

    public static CharacterStatValue active(StatBreakdown value) {
        return new CharacterStatValue(value.total(), StatImplementationState.ACTIVE, value);
    }

    public static CharacterStatValue previewOnly(StatBreakdown value) {
        return new CharacterStatValue(value.total(), StatImplementationState.PREVIEW_ONLY, value);
    }

    public static CharacterStatValue previewOnly(double value) {
        return previewOnly(new StatBreakdown(value, 0, 0, 0));
    }

    public static CharacterStatValue unavailable() {
        return new CharacterStatValue(0, StatImplementationState.UNAVAILABLE, StatBreakdown.ZERO);
    }

    public boolean available() { return implementation != StatImplementationState.UNAVAILABLE; }
    public boolean differsFrom(CharacterStatValue other) {
        return other == null || implementation != other.implementation
                || available() && Math.abs(value - other.value) > EPSILON;
    }
}
