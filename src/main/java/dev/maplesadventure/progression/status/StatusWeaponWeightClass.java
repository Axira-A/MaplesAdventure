package dev.maplesadventure.progression.status;

import dev.maplesadventure.progression.weapon.WeaponRequirementArchetype;

public enum StatusWeaponWeightClass {
    THROWING, NORMAL, GREAT, COLOSSAL;
    public static StatusWeaponWeightClass archetype(String name) {
        return switch(WeaponRequirementArchetype.category(name)) {
            case GREATSWORD, GREATAXE, MACE -> COLOSSAL;
            case LONGSWORD, TACHI, SCYTHE -> GREAT;
            default -> NORMAL;
        };
    }
    public double buildup(StatusEffectType type) {
        return switch(type) {
            case BLEED -> new double[]{12,28,34,41}[ordinal()];
            case POISON, FROSTBITE -> new double[]{14,30,36,43}[ordinal()];
            case SLEEP -> new double[]{10,22,26,30}[ordinal()];
            case MADNESS, SCARLET_ROT -> new double[]{8,18,22,26}[ordinal()];
            case DEATH_BLIGHT -> new double[]{4,8,10,12}[ordinal()];
        };
    }
}
