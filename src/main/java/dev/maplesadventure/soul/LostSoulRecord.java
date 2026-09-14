package dev.maplesadventure.soul;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The server-authoritative active soul entry. Entity NBT is only a persistent mirror. */
public record LostSoulRecord(
        UUID ownerUuid,
        String ownerName,
        UUID soulId,
        ResourceKey<Level> dimension,
        Vec3 position,
        int storedExperience,
        long creationTime,
        long generation
) {
    public LostSoulRecord {
        storedExperience = Math.max(0, storedExperience);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", ownerUuid);
        tag.putString("OwnerName", ownerName);
        tag.putUUID("SoulId", soulId);
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putInt("StoredExperience", storedExperience);
        tag.putLong("CreationTime", creationTime);
        tag.putLong("Generation", generation);
        return tag;
    }

    public static Optional<LostSoulRecord> load(CompoundTag tag) {
        if (!tag.hasUUID("Owner") || !tag.hasUUID("SoulId")) {
            return Optional.empty();
        }
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (dimensionId == null) {
            return Optional.empty();
        }
        return Optional.of(new LostSoulRecord(
                tag.getUUID("Owner"),
                tag.getString("OwnerName"),
                tag.getUUID("SoulId"),
                ResourceKey.create(Registries.DIMENSION, dimensionId),
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                Math.max(0, tag.getInt("StoredExperience")),
                tag.getLong("CreationTime"),
                Math.max(0L, tag.getLong("Generation"))
        ));
    }
}
