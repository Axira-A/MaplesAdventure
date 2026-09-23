package dev.maplesadventure.api.weapon;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** The five stable attributes used for weapon requirements and scaling. */
public enum MaplesWeaponAttribute {
    STRENGTH, DEXTERITY, INTELLIGENCE, FAITH, ARCANE;
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("maplesadventure", name().toLowerCase(Locale.ROOT)); }
    public static Optional<MaplesWeaponAttribute> find(ResourceLocation id) {
        return Arrays.stream(values()).filter(type -> type.id().equals(id)).findFirst();
    }
}
