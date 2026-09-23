package dev.maplesadventure.api.damage;

/** Stable typed-pressure channels. PHYSICAL is standard damage, not the sum of other physical types. */
public enum MaplesDamageChannel {
    /** Standard physical. */ PHYSICAL,
    /** Slashing. */ SLASH,
    /** Blunt. */ STRIKE,
    /** Piercing. */ PIERCE,
    /** Magic. */ MAGIC,
    /** Fire. */ FIRE,
    /** Lightning. */ LIGHTNING,
    /** Ice. */ ICE,
    /** Holy. */ HOLY;

    /** Stable namespaced ID; enum ordinal is never an interchange contract. */
    public net.minecraft.resources.ResourceLocation id() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("maplesadventure", name().toLowerCase(java.util.Locale.ROOT));
    }
    /** Empty for an unknown or missing ID. */
    public static java.util.Optional<MaplesDamageChannel> find(net.minecraft.resources.ResourceLocation id) {
        return java.util.Arrays.stream(values()).filter(type -> type.id().equals(id)).findFirst();
    }
}
