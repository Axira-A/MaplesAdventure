package dev.maplesadventure.progression.encumbrance;

public enum DodgeMode {
    NONE,
    ROLL,
    STEP;

    public String translationKey() {
        return "encumbrance.maplesadventure.dodge." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
