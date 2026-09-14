package dev.maplesadventure.message;

import dev.maplesadventure.MaplesAdventure;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

/** Global persistent store with secondary indexes for dimension/chunk and author. */
public final class MessageSavedData extends SavedData {
    private static final String DATA_NAME = "maplesadventure_messages";
    private static final Factory<MessageSavedData> FACTORY = new Factory<>(MessageSavedData::new, MessageSavedData::load);

    private final Map<UUID, AdventureMessage> messages = new HashMap<>();
    private final Map<ResourceKey<Level>, Map<Long, Set<UUID>>> byDimensionChunk = new HashMap<>();
    private final Map<UUID, Set<UUID>> byAuthor = new HashMap<>();
    private final Map<UUID, Long> lastCreationByAuthor = new HashMap<>();

    public static MessageSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<AdventureMessage> get(UUID messageId) {
        return Optional.ofNullable(messages.get(messageId));
    }

    public int size() {
        return messages.size();
    }

    public Collection<AdventureMessage> allMessages() {
        return List.copyOf(messages.values());
    }

    public void add(AdventureMessage message) {
        AdventureMessage replaced = messages.put(message.messageId(), message);
        if (replaced != null) {
            deindex(replaced);
        }
        index(message);
        setDirty();
    }

    public Optional<AdventureMessage> remove(UUID messageId) {
        AdventureMessage removed = messages.remove(messageId);
        if (removed != null) {
            deindex(removed);
            setDirty();
        }
        return Optional.ofNullable(removed);
    }

    public void ratingChanged() {
        setDirty();
    }

    public long lastCreationTime(UUID authorUuid) {
        return lastCreationByAuthor.getOrDefault(authorUuid, 0L);
    }

    public void recordCreation(UUID authorUuid, long createdAt) {
        lastCreationByAuthor.put(authorUuid, Math.max(createdAt, lastCreationTime(authorUuid)));
        setDirty();
    }

    public List<AdventureMessage> byAuthor(UUID authorUuid) {
        Set<UUID> ids = byAuthor.get(authorUuid);
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        ArrayList<AdventureMessage> result = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            AdventureMessage message = messages.get(id);
            if (message != null) result.add(message);
        }
        result.sort(Comparator.comparingLong(AdventureMessage::createdAt).thenComparing(AdventureMessage::messageId));
        return result;
    }

    public List<AdventureMessage> nearby(ResourceKey<Level> dimension, Vec3 center, double radius, int limit) {
        Map<Long, Set<UUID>> chunks = byDimensionChunk.get(dimension);
        if (chunks == null || chunks.isEmpty() || limit <= 0) {
            return List.of();
        }
        int minChunkX = ((int) Math.floor(center.x - radius)) >> 4;
        int maxChunkX = ((int) Math.floor(center.x + radius)) >> 4;
        int minChunkZ = ((int) Math.floor(center.z - radius)) >> 4;
        int maxChunkZ = ((int) Math.floor(center.z + radius)) >> 4;
        double radiusSqr = radius * radius;
        ArrayList<AdventureMessage> result = new ArrayList<>();
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                Set<UUID> ids = chunks.get(ChunkPos.asLong(chunkX, chunkZ));
                if (ids == null) continue;
                for (UUID id : ids) {
                    AdventureMessage message = messages.get(id);
                    if (message != null && message.position().distanceToSqr(center) <= radiusSqr) {
                        result.add(message);
                    }
                }
            }
        }
        result.sort(Comparator.comparingDouble(message -> message.position().distanceToSqr(center)));
        if (result.size() > limit) {
            result.subList(limit, result.size()).clear();
        }
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("DataVersion", AdventureMessage.DATA_VERSION);
        ListTag list = new ListTag();
        for (AdventureMessage message : messages.values()) {
            list.add(message.save());
        }
        tag.put("Messages", list);
        ListTag cooldowns = new ListTag();
        for (Map.Entry<UUID, Long> entry : lastCreationByAuthor.entrySet()) {
            CompoundTag cooldown = new CompoundTag();
            cooldown.putUUID("Author", entry.getKey());
            cooldown.putLong("CreatedAt", entry.getValue());
            cooldowns.add(cooldown);
        }
        tag.put("LastCreation", cooldowns);
        return tag;
    }

    private static MessageSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        MessageSavedData data = new MessageSavedData();
        ListTag list = tag.getList("Messages", Tag.TAG_COMPOUND);
        int invalid = 0;
        for (int index = 0; index < list.size(); index++) {
            Optional<AdventureMessage> loaded = AdventureMessage.load(list.getCompound(index));
            if (loaded.isPresent()) {
                AdventureMessage message = loaded.get();
                data.messages.put(message.messageId(), message);
                data.index(message);
            } else {
                invalid++;
            }
        }
        if (invalid > 0) {
            MaplesAdventure.LOGGER.warn("Skipped {} invalid or obsolete structured adventure messages while loading", invalid);
        }
        ListTag cooldowns = tag.getList("LastCreation", Tag.TAG_COMPOUND);
        for (int index = 0; index < cooldowns.size(); index++) {
            CompoundTag cooldown = cooldowns.getCompound(index);
            if (cooldown.hasUUID("Author")) {
                data.lastCreationByAuthor.put(cooldown.getUUID("Author"), cooldown.getLong("CreatedAt"));
            }
        }
        // Migration for version-1 worlds saved before the separate cooldown ledger existed.
        for (AdventureMessage message : data.messages.values()) {
            data.lastCreationByAuthor.merge(message.authorUuid(), message.createdAt(), Math::max);
        }
        return data;
    }

    private void index(AdventureMessage message) {
        long chunk = ChunkPos.asLong(message.supportPos().getX() >> 4, message.supportPos().getZ() >> 4);
        byDimensionChunk.computeIfAbsent(message.dimension(), ignored -> new HashMap<>())
                .computeIfAbsent(chunk, ignored -> new HashSet<>()).add(message.messageId());
        byAuthor.computeIfAbsent(message.authorUuid(), ignored -> new HashSet<>()).add(message.messageId());
    }

    private void deindex(AdventureMessage message) {
        long chunk = ChunkPos.asLong(message.supportPos().getX() >> 4, message.supportPos().getZ() >> 4);
        Map<Long, Set<UUID>> chunks = byDimensionChunk.get(message.dimension());
        if (chunks != null) {
            Set<UUID> ids = chunks.get(chunk);
            if (ids != null) {
                ids.remove(message.messageId());
                if (ids.isEmpty()) chunks.remove(chunk);
            }
            if (chunks.isEmpty()) byDimensionChunk.remove(message.dimension());
        }
        Set<UUID> authorIds = byAuthor.get(message.authorUuid());
        if (authorIds != null) {
            authorIds.remove(message.messageId());
            if (authorIds.isEmpty()) byAuthor.remove(message.authorUuid());
        }
    }
}
