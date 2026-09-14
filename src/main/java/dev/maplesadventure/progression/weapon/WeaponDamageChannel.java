package dev.maplesadventure.progression.weapon;

import java.util.Locale;
import net.minecraft.network.chat.Component;

/** PHYSICAL is standard physical, not the sum of the physical subtypes. */
public enum WeaponDamageChannel {
    PHYSICAL, SLASH, STRIKE, PIERCE, MAGIC, FIRE, LIGHTNING, ICE, HOLY;
    public boolean physical() { return this == PHYSICAL || this == SLASH || this == STRIKE || this == PIERCE; }
    public boolean elemental() { return !physical(); }
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public Component displayName() { return Component.translatable("screen.maplesadventure.weapon.channel."+id()); }
    public static WeaponDamageChannel parse(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }
    public static WeaponDamageChannel automatic(WeaponRequirementArchetype type) {
        return switch(type) {
            case LONGSWORD, GREATSWORD, AXE, GREATAXE, UCHIGATANA, TACHI, SCYTHE -> SLASH;
            case DAGGER, SPEAR, TRIDENT, BOW, CROSSBOW -> PIERCE;
            case FIST, MACE -> STRIKE;
            default -> PHYSICAL;
        };
    }
}
