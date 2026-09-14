package dev.maplesadventure.progression;

/** Central constants and overflow-safe pure mathematics for Round 1 progression. */
public final class ProgressionCurve {
    public static final int BASE_LEVEL = 5;
    public static final int BASE_ATTRIBUTE = 5;
    public static final int ABSOLUTE_HARD_CAP = 99;

    public static int adventureLevel(int... attributes) {
        int invested = 0;
        for (int value : attributes) invested += Math.clamp(value, BASE_ATTRIBUTE, ABSOLUTE_HARD_CAP) - BASE_ATTRIBUTE;
        return BASE_LEVEL + invested;
    }

    public static long baseCostForNextLevel(int level) {
        long value = Math.max(0, level);
        try {
            long square = Math.multiplyExact(value, value);
            long cube = Math.multiplyExact(square, value);
            long numerator = Math.addExact(80_000L, Math.multiplyExact(6_000L, value));
            numerator = Math.addExact(numerator, Math.multiplyExact(300L, square));
            numerator = Math.addExact(numerator, Math.multiplyExact(2L, cube));
            return Math.addExact(numerator, 500L) / 1_000L;
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    public static double piecewise(int rawStat, double base, Segment... segments) {
        int stat = Math.clamp(rawStat, BASE_ATTRIBUTE, ABSOLUTE_HARD_CAP);
        double result = base;
        int cursor = BASE_ATTRIBUTE;
        for (Segment segment : segments) {
            if (stat <= cursor) break;
            int upper = Math.min(stat, segment.upperInclusive());
            if (upper > cursor) result += (upper - cursor) * segment.gainPerPoint();
            cursor = segment.upperInclusive();
        }
        return result;
    }

    public record Segment(int upperInclusive, double gainPerPoint) {}
    private ProgressionCurve() {}
}
