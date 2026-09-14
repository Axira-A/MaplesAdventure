package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.progression.Attribute;

/** Language-neutral weapon balance, independent of requirements and of the wielder. */
public record WeaponScalingProfile(double strength, double dexterity, double intelligence, double faith,
        double arcane, double maxBonus, String source, String weaponClass, String debugReason) {
    public static final double DEFAULT_MAX_BONUS = 1.15;
    public static final WeaponScalingProfile NONE = new WeaponScalingProfile(0,0,0,0,0,DEFAULT_MAX_BONUS,"NONE","NONE","");
    public WeaponScalingProfile {
        for (double coefficient : new double[]{strength,dexterity,intelligence,faith,arcane})
            if (!Double.isFinite(coefficient) || coefficient < 0 || coefficient > 1.5)
                throw new IllegalArgumentException("Scaling coefficient outside 0..1.5");
        if (!Double.isFinite(maxBonus) || maxBonus < 0 || maxBonus > 2)
            throw new IllegalArgumentException("max_bonus outside 0..2");
        if (source == null || source.length()>32 || weaponClass == null || weaponClass.length()>64
                || debugReason == null || debugReason.length()>512) throw new IllegalArgumentException("Scaling metadata bounds");
    }
    public boolean enabled() { return !source.equals("NONE") && !source.equals("DISABLED"); }
    public double get(Attribute attribute) {
        return switch(attribute) {
            case STRENGTH -> strength; case DEXTERITY -> dexterity; case INTELLIGENCE -> intelligence;
            case FAITH -> faith; case ARCANE -> arcane; default -> 0;
        };
    }
    public static WeaponScalingProfile automatic(WeaponRequirementArchetype type) {
        double[] c = switch(type) {
            case NONE -> new double[]{0,0}; case TOOL -> new double[]{.15,.05};
            case DAGGER -> new double[]{.10,.65}; case SWORD, GENERIC -> new double[]{.35,.35};
            case LONGSWORD -> new double[]{.50,.30}; case GREATSWORD -> new double[]{.75,.15};
            case AXE -> new double[]{.65,.10}; case GREATAXE -> new double[]{.80,.10};
            case SPEAR -> new double[]{.25,.55}; case SCYTHE -> new double[]{.25,.60};
            case TRIDENT -> new double[]{.30,.55}; case UCHIGATANA -> new double[]{.15,.70};
            case TACHI -> new double[]{.25,.65}; case FIST -> new double[]{.15,.60};
            case BOW -> new double[]{.10,.70}; case CROSSBOW -> new double[]{.25,.45}; case MACE -> new double[]{.80,0};
        };
        return type == WeaponRequirementArchetype.NONE ? NONE : new WeaponScalingProfile(c[0],c[1],0,0,0,
                DEFAULT_MAX_BONUS,"ARCHETYPE",type.name(),"Material-independent category default");
    }
}
