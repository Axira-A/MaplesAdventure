package dev.maplesadventure.progression.weapon;

public enum WeaponInfusionBuildup {
    NONE, BLEED, POISON;
    public static WeaponInfusionBuildup parse(String value) {
        try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException error) { throw new IllegalArgumentException("Unknown future buildup " + value); }
    }
}
