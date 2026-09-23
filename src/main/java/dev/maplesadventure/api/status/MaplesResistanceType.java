package dev.maplesadventure.api.status;

import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Four stable resistance families. Armor contributes to families, not individual ailments. */
public enum MaplesResistanceType {
    IMMUNITY, ROBUSTNESS, FOCUS, VITALITY;
    public ResourceLocation id() { return ResourceLocation.fromNamespaceAndPath("maplesadventure", name().toLowerCase(Locale.ROOT)); }
    public static Optional<MaplesResistanceType> find(ResourceLocation id) {
        return Arrays.stream(values()).filter(type -> type.id().equals(id)).findFirst();
    }
    /** Canonical ailments sharing this threshold family. */
    public Set<MaplesStatusType> statuses() {
        return switch (this) {
            case IMMUNITY -> Set.of(MaplesStatusType.POISON, MaplesStatusType.SCARLET_ROT);
            case ROBUSTNESS -> Set.of(MaplesStatusType.BLEED, MaplesStatusType.FROSTBITE);
            case FOCUS -> Set.of(MaplesStatusType.SLEEP, MaplesStatusType.MADNESS);
            case VITALITY -> Set.of(MaplesStatusType.DEATH_BLIGHT);
        };
    }
}
