package dev.maplesadventure.message;

public enum MessageRating {
    NONE,
    POSITIVE,
    NEGATIVE;

    public static MessageRating fromNetwork(int ordinal) {
        return ordinal >= 0 && ordinal < values().length ? values()[ordinal] : NONE;
    }
}
