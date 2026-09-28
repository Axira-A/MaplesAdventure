package dev.maplesadventure.api.editor;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Bounded scalar union for schema fields. Text encodes a value, never code or NBT. */
public record EditorValue(Kind kind, String text) {
    public enum Kind { BOOLEAN, INTEGER, DOUBLE, STRING, ENUM, RESOURCE_LOCATION, NULL }
    public EditorValue {
        Objects.requireNonNull(kind); Objects.requireNonNull(text);
        if (text.length() > 1024) throw new IllegalArgumentException("Field value too long");
        switch (kind) {
            case BOOLEAN -> { if (!text.equals("true") && !text.equals("false")) throw new IllegalArgumentException("Invalid boolean"); }
            case INTEGER -> Integer.parseInt(text);
            case DOUBLE -> { if (!Double.isFinite(Double.parseDouble(text))) throw new IllegalArgumentException("Non-finite number"); }
            case RESOURCE_LOCATION -> { if (text.length() > 256 || ResourceLocation.tryParse(text) == null) throw new IllegalArgumentException("Invalid resource ID"); }
            case NULL -> { if (!text.isEmpty()) throw new IllegalArgumentException("Invalid null"); }
            default -> { }
        }
    }
    public double number() { return Double.parseDouble(text); }
    public static EditorValue decimal(double value) { return new EditorValue(Kind.DOUBLE, Double.toString(value)); }
    public static EditorValue string(String value) { return new EditorValue(Kind.STRING, value); }
}
