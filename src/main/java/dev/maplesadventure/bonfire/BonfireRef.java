package dev.maplesadventure.bonfire;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

/** A placement identity, not merely a copied block-entity UUID. */
public record BonfireRef(ResourceLocation dimension, BlockPos pos, UUID generation) {
    public BonfireRef {
        pos = pos.immutable();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Dimension", dimension.toString());
        tag.putInt("X", pos.getX());
        tag.putInt("Y", pos.getY());
        tag.putInt("Z", pos.getZ());
        tag.putUUID("Generation", generation);
        return tag;
    }

    public static Optional<BonfireRef> load(CompoundTag tag) {
        if (!tag.hasUUID("Generation") || !tag.contains("Dimension", Tag.TAG_STRING)
                || !tag.contains("X", Tag.TAG_INT) || !tag.contains("Y", Tag.TAG_INT)
                || !tag.contains("Z", Tag.TAG_INT)) return Optional.empty();
        String raw = tag.getString("Dimension");
        if (raw.length() > 128) return Optional.empty();
        ResourceLocation dimension = ResourceLocation.tryParse(raw);
        if (dimension == null) return Optional.empty();
        return Optional.of(new BonfireRef(dimension,
                new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")),
                tag.getUUID("Generation")));
    }
}
