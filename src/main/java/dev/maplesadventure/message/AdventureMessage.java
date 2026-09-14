package dev.maplesadventure.message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Server-owned persistent record. Author identity and individual ratings never enter nearby render snapshots. */
public final class AdventureMessage {
    public static final int DATA_VERSION = 2;

    private final UUID messageId;
    private final UUID authorUuid;
    private final String authorCachedName;
    private final ResourceKey<Level> dimension;
    private final Vec3 position;
    private final BlockPos supportPos;
    private final float yaw;
    private final Direction surfaceNormal;
    private final List<MessagePhrase> phrases;
    private final List<String> connectors;
    private final long createdAt;
    private final Map<UUID, MessageRating> ratings;
    private int positiveRatings;
    private int negativeRatings;

    public AdventureMessage(
            UUID messageId,
            UUID authorUuid,
            String authorCachedName,
            ResourceKey<Level> dimension,
            Vec3 position,
            BlockPos supportPos,
            float yaw,
            Direction surfaceNormal,
            List<MessagePhrase> phrases,
            List<String> connectors,
            long createdAt
    ) {
        this(messageId, authorUuid, authorCachedName, dimension, position, supportPos, yaw, surfaceNormal,
                phrases, connectors, createdAt, new HashMap<>());
    }

    public AdventureMessage(UUID messageId, UUID authorUuid, String authorCachedName,
                            ResourceKey<Level> dimension, Vec3 position, BlockPos supportPos, float yaw,
                            Direction surfaceNormal, List<MessagePhrase> phrases, long createdAt) {
        this(messageId, authorUuid, authorCachedName, dimension, position, supportPos, yaw,
                surfaceNormal, phrases, phrases.size() <= 1 ? List.of()
                        : java.util.Collections.nCopies(phrases.size() - 1, "and"), createdAt);
    }

    private AdventureMessage(
            UUID messageId,
            UUID authorUuid,
            String authorCachedName,
            ResourceKey<Level> dimension,
            Vec3 position,
            BlockPos supportPos,
            float yaw,
            Direction surfaceNormal,
            List<MessagePhrase> phrases,
            List<String> connectors,
            long createdAt,
            Map<UUID, MessageRating> ratings
    ) {
        this.messageId = messageId;
        this.authorUuid = authorUuid;
        this.authorCachedName = authorCachedName;
        this.dimension = dimension;
        this.position = position;
        this.supportPos = supportPos.immutable();
        this.yaw = yaw;
        this.surfaceNormal = surfaceNormal;
        this.phrases = List.copyOf(phrases);
        if (connectors.size() != Math.max(0, phrases.size() - 1)) {
            throw new IllegalArgumentException("Connector count must be exactly one less than phrase count");
        }
        this.connectors = List.copyOf(connectors);
        this.createdAt = createdAt;
        this.ratings = ratings;
        recomputeRatingCounts();
    }

    public UUID messageId() { return messageId; }
    public UUID authorUuid() { return authorUuid; }
    public String authorCachedName() { return authorCachedName; }
    public ResourceKey<Level> dimension() { return dimension; }
    public Vec3 position() { return position; }
    public BlockPos supportPos() { return supportPos; }
    public float yaw() { return yaw; }
    public Direction surfaceNormal() { return surfaceNormal; }
    public List<MessagePhrase> phrases() { return phrases; }
    public List<String> connectors() { return connectors; }
    public long createdAt() { return createdAt; }
    public int positiveRatings() { return positiveRatings; }
    public int negativeRatings() { return negativeRatings; }

    public MessageRating ratingBy(UUID playerUuid) {
        return ratings.getOrDefault(playerUuid, MessageRating.NONE);
    }

    public boolean updateRating(UUID playerUuid, MessageRating requested) {
        MessageRating previous = ratingBy(playerUuid);
        if (previous == requested) {
            return false;
        }
        MessageRatingMath.Counts counts = MessageRatingMath.apply(
                positiveRatings, negativeRatings, previous, requested
        );
        if (requested == MessageRating.NONE) {
            ratings.remove(playerUuid);
        } else {
            ratings.put(playerUuid, requested);
        }
        positiveRatings = counts.positive();
        negativeRatings = counts.negative();
        return true;
    }

    public MessageSummary summary() {
        return new MessageSummary(messageId, position, supportPos, yaw, surfaceNormal, phrases, connectors,
                createdAt, positiveRatings, negativeRatings);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION);
        tag.putUUID("MessageId", messageId);
        tag.putUUID("AuthorUuid", authorUuid);
        tag.putString("AuthorName", authorCachedName);
        tag.putString("Dimension", dimension.location().toString());
        tag.putDouble("X", position.x);
        tag.putDouble("Y", position.y);
        tag.putDouble("Z", position.z);
        tag.putInt("SupportX", supportPos.getX());
        tag.putInt("SupportY", supportPos.getY());
        tag.putInt("SupportZ", supportPos.getZ());
        tag.putFloat("Yaw", yaw);
        tag.putByte("SurfaceNormal", (byte) surfaceNormal.ordinal());
        tag.putLong("CreatedAt", createdAt);

        ListTag phraseTags = new ListTag();
        for (MessagePhrase phrase : phrases) {
            CompoundTag phraseTag = new CompoundTag();
            phraseTag.putString("Template", phrase.templateId());
            CompoundTag slots = new CompoundTag();
            phrase.slots().forEach(slots::putString);
            phraseTag.put("Slots", slots);
            CompoundTag modifiers = new CompoundTag();
            for (Map.Entry<String, List<String>> entry : phrase.modifiers().entrySet()) {
                ListTag values = new ListTag();
                for (String modifier : entry.getValue()) values.add(StringTag.valueOf(modifier));
                modifiers.put(entry.getKey(), values);
            }
            phraseTag.put("Modifiers", modifiers);
            phraseTags.add(phraseTag);
        }
        tag.put("Phrases", phraseTags);
        ListTag connectorTags = new ListTag();
        for (String connector : connectors) connectorTags.add(StringTag.valueOf(connector));
        tag.put("Connectors", connectorTags);

        ListTag ratingTags = new ListTag();
        for (Map.Entry<UUID, MessageRating> entry : ratings.entrySet()) {
            CompoundTag ratingTag = new CompoundTag();
            ratingTag.putUUID("Player", entry.getKey());
            ratingTag.putByte("Rating", (byte) entry.getValue().ordinal());
            ratingTags.add(ratingTag);
        }
        tag.put("Ratings", ratingTags);
        return tag;
    }

    public static Optional<AdventureMessage> load(CompoundTag tag) {
        try {
            if (tag.getInt("DataVersion") > DATA_VERSION || !tag.hasUUID("MessageId") || !tag.hasUUID("AuthorUuid")) {
                return Optional.empty();
            }
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("Dimension"));
            if (dimensionId == null) {
                return Optional.empty();
            }
            ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
            int directionOrdinal = tag.getByte("SurfaceNormal");
            if (directionOrdinal < 0 || directionOrdinal >= Direction.values().length) {
                return Optional.empty();
            }
            ListTag phraseTags = tag.getList("Phrases", Tag.TAG_COMPOUND);
            if (phraseTags.isEmpty() || phraseTags.size() > 2) {
                return Optional.empty();
            }
            java.util.ArrayList<MessagePhrase> phrases = new java.util.ArrayList<>(phraseTags.size());
            for (int index = 0; index < phraseTags.size(); index++) {
                CompoundTag phraseTag = phraseTags.getCompound(index);
                MessagePhrase phrase;
                if (phraseTag.contains("Slots", Tag.TAG_COMPOUND)) {
                    CompoundTag slotTag = phraseTag.getCompound("Slots");
                    Map<String, String> slots = new java.util.LinkedHashMap<>();
                    for (String key : slotTag.getAllKeys()) slots.put(key, slotTag.getString(key));
                    Map<String, List<String>> modifiers = new java.util.LinkedHashMap<>();
                    CompoundTag modifierTag = phraseTag.getCompound("Modifiers");
                    for (String key : modifierTag.getAllKeys()) {
                        ListTag valueTags = modifierTag.getList(key, Tag.TAG_STRING);
                        java.util.ArrayList<String> values = new java.util.ArrayList<>(valueTags.size());
                        for (int valueIndex = 0; valueIndex < valueTags.size(); valueIndex++) {
                            values.add(valueTags.getString(valueIndex));
                        }
                        modifiers.put(key, values);
                    }
                    phrase = new MessagePhrase(phraseTag.getString("Template"), slots, modifiers);
                } else {
                    ListTag tokenTags = phraseTag.getList("Tokens", Tag.TAG_STRING);
                    java.util.ArrayList<String> tokens = new java.util.ArrayList<>(tokenTags.size());
                    for (int tokenIndex = 0; tokenIndex < tokenTags.size(); tokenIndex++) {
                        tokens.add(tokenTags.getString(tokenIndex));
                    }
                    phrase = new MessagePhrase(phraseTag.getString("Template"), tokens);
                }
                if (!MessageTemplateRegistry.isValid(phrase)) {
                    return Optional.empty();
                }
                phrases.add(phrase);
            }
            ListTag connectorTags = tag.getList("Connectors", Tag.TAG_STRING);
            java.util.ArrayList<String> connectors = new java.util.ArrayList<>();
            for (int index = 0; index < connectorTags.size(); index++) connectors.add(connectorTags.getString(index));
            while (connectors.size() < phrases.size() - 1) connectors.add("and");
            if (connectors.size() > Math.max(0, phrases.size() - 1)
                    || connectors.stream().anyMatch(id -> MessageConnectorRegistry.find(id).isEmpty())) {
                return Optional.empty();
            }

            Map<UUID, MessageRating> ratings = new HashMap<>();
            ListTag ratingTags = tag.getList("Ratings", Tag.TAG_COMPOUND);
            for (int index = 0; index < ratingTags.size(); index++) {
                CompoundTag ratingTag = ratingTags.getCompound(index);
                if (!ratingTag.hasUUID("Player")) {
                    continue;
                }
                MessageRating rating = MessageRating.fromNetwork(ratingTag.getByte("Rating"));
                if (rating != MessageRating.NONE) {
                    ratings.put(ratingTag.getUUID("Player"), rating);
                }
            }
            return Optional.of(new AdventureMessage(
                    tag.getUUID("MessageId"),
                    tag.getUUID("AuthorUuid"),
                    tag.getString("AuthorName"),
                    dimension,
                    new Vec3(tag.getDouble("X"), tag.getDouble("Y"), tag.getDouble("Z")),
                    new BlockPos(tag.getInt("SupportX"), tag.getInt("SupportY"), tag.getInt("SupportZ")),
                    tag.getFloat("Yaw"),
                    Direction.values()[directionOrdinal],
                    phrases,
                    connectors,
                    tag.getLong("CreatedAt"),
                    ratings
            ));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    private void recomputeRatingCounts() {
        positiveRatings = 0;
        negativeRatings = 0;
        for (MessageRating rating : ratings.values()) {
            increment(rating);
        }
    }

    private void increment(MessageRating rating) {
        if (rating == MessageRating.POSITIVE) positiveRatings++;
        if (rating == MessageRating.NEGATIVE) negativeRatings++;
    }

}
