package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.progression.Attribute;
import java.util.List;

/** Type-level rules, never written into ItemStack NBT. */
public record WeaponRequirementProfile(int strength, int dexterity, int intelligence, int faith, int arcane,
        String source, String weaponClass, String debugReason) {
    public static final List<Attribute> ATTRIBUTES = List.of(Attribute.STRENGTH, Attribute.DEXTERITY,
            Attribute.INTELLIGENCE, Attribute.FAITH, Attribute.ARCANE);
    public static final WeaponRequirementProfile NONE = new WeaponRequirementProfile(0,0,0,0,0,"NONE","NONE","Not a weapon");
    public WeaponRequirementProfile {
        for (int v : new int[]{strength,dexterity,intelligence,faith,arcane})
            if (v < 0 || v > 99) throw new IllegalArgumentException("Requirement outside 0..99");
        if (source == null || source.length()>32 || weaponClass == null || weaponClass.length()>64
                || debugReason == null || debugReason.length()>512) throw new IllegalArgumentException("Requirement metadata too long");
    }
    public int get(Attribute attribute) { return switch(attribute) {
        case STRENGTH -> strength; case DEXTERITY -> dexterity; case INTELLIGENCE -> intelligence;
        case FAITH -> faith; case ARCANE -> arcane; default -> 0;
    }; }
    public boolean enabled() { return !source.equals("NONE") && !source.equals("DISABLED"); }
}
