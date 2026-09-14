package dev.maplesadventure.message;

/** Pure counter transition kept separately so duplicate/switch semantics are unit-testable. */
public final class MessageRatingMath {
    public static Counts apply(int positive, int negative, MessageRating previous, MessageRating requested) {
        int nextPositive = positive;
        int nextNegative = negative;
        if (previous == MessageRating.POSITIVE) nextPositive--;
        if (previous == MessageRating.NEGATIVE) nextNegative--;
        if (requested == MessageRating.POSITIVE) nextPositive++;
        if (requested == MessageRating.NEGATIVE) nextNegative++;
        return new Counts(Math.max(0, nextPositive), Math.max(0, nextNegative));
    }

    public record Counts(int positive, int negative) {
    }

    private MessageRatingMath() {
    }
}
