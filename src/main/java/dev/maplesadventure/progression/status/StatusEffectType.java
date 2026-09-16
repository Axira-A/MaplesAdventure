package dev.maplesadventure.progression.status;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/** Canonical ailments; independent of weapon damage channels. */
public enum StatusEffectType {
    BLEED, POISON, SCARLET_ROT, FROSTBITE;
    public String id() { return name().toLowerCase(Locale.ROOT); }
    public ResourceLocation definitionId() { return ResourceLocation.fromNamespaceAndPath("maplesadventure", id()); }
    public static StatusEffectType parse(String id) { return valueOf(id.toUpperCase(Locale.ROOT)); }
}
