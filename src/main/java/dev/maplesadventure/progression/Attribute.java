package dev.maplesadventure.progression;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.network.chat.Component;

public enum Attribute {
    VIGOR("vig"),
    MIND("mnd"),
    ENDURANCE("end"),
    STRENGTH("str"),
    DEXTERITY("dex"),
    INTELLIGENCE("int"),
    FAITH("fth"),
    ARCANE("arc");

    private final String abbreviation;

    Attribute(String abbreviation) { this.abbreviation = abbreviation; }

    public String abbreviation() { return abbreviation; }
    public String serializedName() { return name().toLowerCase(Locale.ROOT); }
    public String translationKey() { return "attribute.maplesadventure." + serializedName(); }
    public Component displayName() { return Component.translatable(translationKey()); }

    public static Optional<Attribute> parse(String value) {
        if (value == null) return Optional.empty();
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(attribute -> attribute.serializedName().equals(normalized)
                || attribute.abbreviation.equals(normalized)).findFirst();
    }
}
