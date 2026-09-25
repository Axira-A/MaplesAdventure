package dev.maplesadventure.api.bonfire;

import java.util.Objects;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

/** Immutable placement identity. Replacing a bonfire at the same position invalidates its old generation. */
public record MaplesBonfireRef(ResourceLocation dimension, BlockPos position, UUID generation) {
    public MaplesBonfireRef {
        Objects.requireNonNull(dimension);
        Objects.requireNonNull(generation);
        position = Objects.requireNonNull(position).immutable();
        if (dimension.toString().length() > 128) throw new IllegalArgumentException("Dimension ID too long");
    }
}
