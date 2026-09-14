package dev.maplesadventure.soul;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** One global map, stored in the overworld, so a player has at most one soul across all dimensions. */
public final class LostSoulSavedData extends SavedData {
    private static final String DATA_NAME = "maplesadventure_lost_souls";
    private static final Factory<LostSoulSavedData> FACTORY = new Factory<>(
            LostSoulSavedData::new,
            LostSoulSavedData::load
    );

    private final Map<UUID, LostSoulRecord> activeSouls = new HashMap<>();
    private long nextGeneration = 1L;

    public static LostSoulSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<LostSoulRecord> getActive(UUID ownerUuid) {
        return Optional.ofNullable(activeSouls.get(ownerUuid));
    }

    public boolean isActive(UUID ownerUuid, UUID soulId, long generation) {
        LostSoulRecord record = activeSouls.get(ownerUuid);
        return record != null && record.soulId().equals(soulId) && record.generation() == generation;
    }

    public long nextGeneration() {
        long generation = Math.max(1L, nextGeneration++);
        setDirty();
        return generation;
    }

    public void setActive(LostSoulRecord record) {
        activeSouls.put(record.ownerUuid(), record);
        nextGeneration = Math.max(nextGeneration, record.generation() + 1L);
        setDirty();
    }

    public Optional<LostSoulRecord> removeActive(UUID ownerUuid) {
        LostSoulRecord removed = activeSouls.remove(ownerUuid);
        if (removed != null) {
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    /** Compare-and-remove is the atomic claim operation used before XP restoration. */
    public Optional<LostSoulRecord> claim(UUID ownerUuid, UUID soulId, long generation) {
        LostSoulRecord record = activeSouls.get(ownerUuid);
        if (record == null || !record.soulId().equals(soulId) || record.generation() != generation) {
            return Optional.empty();
        }
        activeSouls.remove(ownerUuid);
        setDirty();
        return Optional.of(record);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("NextGeneration", nextGeneration);
        ListTag entries = new ListTag();
        for (LostSoulRecord record : activeSouls.values()) {
            entries.add(record.save());
        }
        tag.put("ActiveSouls", entries);
        return tag;
    }

    private static LostSoulSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        LostSoulSavedData data = new LostSoulSavedData();
        data.nextGeneration = Math.max(1L, tag.getLong("NextGeneration"));
        ListTag entries = tag.getList("ActiveSouls", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            LostSoulRecord.load(entries.getCompound(index)).ifPresent(record -> {
                data.activeSouls.put(record.ownerUuid(), record);
                data.nextGeneration = Math.max(data.nextGeneration, record.generation() + 1L);
            });
        }
        return data;
    }
}
