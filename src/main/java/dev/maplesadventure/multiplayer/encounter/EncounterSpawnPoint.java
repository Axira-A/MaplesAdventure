package dev.maplesadventure.multiplayer.encounter;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record EncounterSpawnPoint(UUID spawnPointId, Vec3 position, float yaw, ResourceLocation entityType,
                                  EncounterSpawnRole role) {
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", spawnPointId);
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putFloat("Yaw", yaw);
        tag.putString("EntityType", entityType.toString());
        tag.putString("Role", role.name());
        return tag;
    }

    public static Optional<EncounterSpawnPoint> load(CompoundTag tag, EncounterSpawnRole legacyRole) {
        ResourceLocation type = ResourceLocation.tryParse(tag.getString("EntityType"));
        if (!tag.hasUUID("Id") || type == null) return Optional.empty();
        EncounterSpawnRole role = legacyRole;
        if (tag.contains("Role")) {
            try { role = EncounterSpawnRole.valueOf(tag.getString("Role")); }
            catch (IllegalArgumentException ignored) { role = legacyRole; }
        }
        return Optional.of(new EncounterSpawnPoint(tag.getUUID("Id"),
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                tag.getFloat("Yaw"), type, role));
    }
}
