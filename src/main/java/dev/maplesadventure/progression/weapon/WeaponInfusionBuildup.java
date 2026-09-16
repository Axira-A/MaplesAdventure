package dev.maplesadventure.progression.weapon;

/** Legacy infusion metadata. Runtime immediately converts to the canonical StatusEffectType. */
@Deprecated
public enum WeaponInfusionBuildup {
    NONE, BLEED, POISON;
    public java.util.Optional<dev.maplesadventure.progression.status.StatusEffectType> status() {
        return this == NONE ? java.util.Optional.empty() : java.util.Optional.of(dev.maplesadventure.progression.status.StatusEffectType.valueOf(name()));
    }
    public static WeaponInfusionBuildup parse(String value) {
        try { return valueOf(value.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException error) { throw new IllegalArgumentException("Unknown future buildup " + value); }
    }
}
