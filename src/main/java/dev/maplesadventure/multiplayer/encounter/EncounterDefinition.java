package dev.maplesadventure.multiplayer.encounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record EncounterDefinition(ResourceLocation encounterId, ResourceKey<Level> dimension,
                                  EncounterType type, Vec3 anchor, double activationRadius,
                                  List<EncounterSpawnPoint> spawnPoints) {
    public EncounterDefinition {
        spawnPoints = List.copyOf(spawnPoints);
    }

    public EncounterDefinition withSpawn(EncounterSpawnPoint point) {
        ArrayList<EncounterSpawnPoint> points = new ArrayList<>(spawnPoints);
        points.add(point);
        return new EncounterDefinition(encounterId, dimension, type, anchor, activationRadius, points);
    }

    public EncounterDefinition withRadius(double radius) {
        return new EncounterDefinition(encounterId, dimension, type, anchor, radius, spawnPoints);
    }

    public Optional<EncounterDefinition> withSpawnRole(java.util.UUID spawnPointId, EncounterSpawnRole role) {
        if (type == EncounterType.COMMON && role != EncounterSpawnRole.NORMAL) return Optional.empty();
        ArrayList<EncounterSpawnPoint> points = new ArrayList<>(spawnPoints.size());
        boolean found = false;
        for (EncounterSpawnPoint point : spawnPoints) {
            EncounterSpawnRole next = point.role();
            if (point.spawnPointId().equals(spawnPointId)) {
                next = role;
                found = true;
            } else if (type == EncounterType.BOSS && role == EncounterSpawnRole.BOSS_PRIMARY
                    && point.role() == EncounterSpawnRole.BOSS_PRIMARY) {
                next = EncounterSpawnRole.BOSS_ADD;
            }
            points.add(new EncounterSpawnPoint(point.spawnPointId(), point.position(), point.yaw(), point.entityType(), next));
        }
        return found ? Optional.of(new EncounterDefinition(encounterId, dimension, type, anchor,
                activationRadius, points)) : Optional.empty();
    }

    public long primaryCount() {
        return spawnPoints.stream().filter(point -> point.role() == EncounterSpawnRole.BOSS_PRIMARY).count();
    }

    public boolean validForActivation() {
        return !spawnPoints.isEmpty() && (type != EncounterType.BOSS || primaryCount() == 1L);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Id", encounterId.toString());
        tag.putString("Dimension", dimension.location().toString());
        tag.putString("Type", type.name());
        tag.putDouble("X", anchor.x);
        tag.putDouble("Y", anchor.y);
        tag.putDouble("Z", anchor.z);
        tag.putDouble("Radius", activationRadius);
        ListTag points = new ListTag();
        spawnPoints.forEach(point -> points.add(point.save()));
        tag.put("SpawnPoints", points);
        return tag;
    }

    public static Optional<EncounterDefinition> load(CompoundTag tag) {
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("Id"));
        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
        if (id == null || dimensionId == null) return Optional.empty();
        EncounterType type;
        try { type = EncounterType.valueOf(tag.getString("Type")); }
        catch (IllegalArgumentException exception) { return Optional.empty(); }
        List<EncounterSpawnPoint> points = new ArrayList<>();
        ListTag pointTags = tag.getList("SpawnPoints", Tag.TAG_COMPOUND);
        for (int i = 0; i < pointTags.size(); i++) {
            EncounterSpawnRole legacyRole = type == EncounterType.COMMON ? EncounterSpawnRole.NORMAL
                    : i == 0 ? EncounterSpawnRole.BOSS_PRIMARY : EncounterSpawnRole.BOSS_ADD;
            EncounterSpawnPoint.load(pointTags.getCompound(i), legacyRole).ifPresent(points::add);
        }
        return Optional.of(new EncounterDefinition(id, ResourceKey.create(Registries.DIMENSION, dimensionId), type,
                new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                Math.max(1.0D, tag.getDouble("Radius")), points));
    }
}
