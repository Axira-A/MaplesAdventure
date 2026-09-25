package dev.maplesadventure.api.bonfire;

import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Detached map-author configuration; configured does not imply registered or currently available. */
public record MaplesBonfireView(MaplesBonfireRef ref, String displayName, Set<ResourceLocation> configuredFeatures) {
    public MaplesBonfireView {
        Objects.requireNonNull(ref);
        Objects.requireNonNull(displayName);
        configuredFeatures = Set.copyOf(configuredFeatures);
        if (displayName.length() > 64 || configuredFeatures.size() > 64
                || configuredFeatures.stream().anyMatch(id -> id.toString().length() > 128))
            throw new IllegalArgumentException("Oversized bonfire view");
    }
}
