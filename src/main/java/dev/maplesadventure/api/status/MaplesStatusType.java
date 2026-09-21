package dev.maplesadventure.api.status;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/** Stable canonical ailments. IDs, not enum ordinals, are the persistence/interchange contract. */
public enum MaplesStatusType {
    /** Burst blood loss. */ BLEED,
    /** Persistent poison. */ POISON,
    /** Persistent scarlet rot. */ SCARLET_ROT,
    /** Frost burst and vulnerability. */ FROSTBITE,
    /** Sleep or stagger. */ SLEEP,
    /** Madness for eligible targets. */ MADNESS,
    /** Death blight for explicitly eligible targets. */ DEATH_BLIGHT;

    /**
     * @return stable namespaced ID */
    public ResourceLocation id() {
        return ResourceLocation.fromNamespaceAndPath("maplesadventure", name().toLowerCase(java.util.Locale.ROOT));
    }
    /**
     * @param id canonical ID, or null
     * @return empty for null/unknown IDs */
    public static Optional<MaplesStatusType> find(ResourceLocation id) {
        return Arrays.stream(values()).filter(t -> t.id().equals(id)).findFirst();
    }
}
