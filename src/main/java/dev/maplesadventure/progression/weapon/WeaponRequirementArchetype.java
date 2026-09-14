package dev.maplesadventure.progression.weapon;

/** No item-name/rarity/namespace inference. Category is from a real capability or Java type. */
public enum WeaponRequirementArchetype {
    NONE(0,0), TOOL(5,5), DAGGER(5,9), SWORD(8,7), LONGSWORD(10,8),
    GREATSWORD(16,9), GREATAXE(18,7), AXE(11,7), SPEAR(9,11), TRIDENT(9,11),
    UCHIGATANA(8,13), TACHI(10,13), FIST(5,8), SCYTHE(10,12),
    BOW(5,12), CROSSBOW(9,10), MACE(16,7), GENERIC(8,7);
    public final int strength, dexterity;
    WeaponRequirementArchetype(int strength, int dexterity) { this.strength=strength; this.dexterity=dexterity; }
    public static WeaponRequirementArchetype category(String name) {
        return switch(name) {
            case "PICKAXE","SHOVEL","HOE" -> TOOL;
            case "KATANA" -> UCHIGATANA;
            case "NOT_WEAPON","SHIELD","RANGED" -> NONE;
            default -> { try { yield valueOf(name); } catch(IllegalArgumentException absent) { yield GENERIC; } }
        };
    }
}
