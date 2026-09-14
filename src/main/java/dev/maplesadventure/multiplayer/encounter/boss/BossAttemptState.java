package dev.maplesadventure.multiplayer.encounter.boss;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

/** Persistent, immutable-scaling identity for one boss attempt. */
public final class BossAttemptState {
    public static final UUID NIL_UUID = new UUID(0L, 0L);

    private final UUID attemptId;
    private final long generation;
    private UUID primaryBossUuid;
    private final int partySize;
    private final double healthMultiplier;
    private final long startedAt;
    private int bossStage;
    private final Map<UUID, BossParticipantEntryState> participants = new LinkedHashMap<>();

    public BossAttemptState(UUID attemptId, long generation, UUID primaryBossUuid, int partySize,
                            double healthMultiplier, long startedAt, int bossStage) {
        this.attemptId = attemptId;
        this.generation = Math.max(0L, generation);
        this.primaryBossUuid = primaryBossUuid == null ? NIL_UUID : primaryBossUuid;
        this.partySize = Math.max(1, Math.min(2, partySize));
        this.healthMultiplier = Math.max(1.0D, healthMultiplier);
        this.startedAt = startedAt;
        this.bossStage = Math.max(1, bossStage);
    }

    public static BossAttemptState create(long generation, int partySize, double multiplier) {
        return new BossAttemptState(UUID.randomUUID(), generation, NIL_UUID, partySize, multiplier,
                System.currentTimeMillis(), 1);
    }

    public static BossAttemptState legacy(long generation, int stage) {
        return new BossAttemptState(UUID.randomUUID(), generation, NIL_UUID, 1, 1.0D,
                System.currentTimeMillis(), stage);
    }

    public UUID attemptId() { return attemptId; }
    public long generation() { return generation; }
    public UUID primaryBossUuid() { return primaryBossUuid; }
    public int partySize() { return partySize; }
    public double healthMultiplier() { return healthMultiplier; }
    public long startedAt() { return startedAt; }
    public int bossStage() { return bossStage; }
    public boolean hasPrimary() { return !NIL_UUID.equals(primaryBossUuid); }
    public void setPrimaryBossUuid(UUID uuid) { primaryBossUuid = uuid == null ? NIL_UUID : uuid; }
    public void setBossStage(int stage) { bossStage = Math.max(1, stage); }
    public BossParticipantEntryState entryState(UUID player) {
        return participants.getOrDefault(player, BossParticipantEntryState.OUTSIDE);
    }
    public boolean isEntered(UUID player) { return entryState(player) == BossParticipantEntryState.ENTERED; }
    public void setEntryState(UUID player, BossParticipantEntryState state) { participants.put(player, state); }
    public Map<UUID, BossParticipantEntryState> participants() { return Map.copyOf(participants); }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("AttemptId", attemptId);
        tag.putLong("Generation", generation);
        tag.putUUID("PrimaryBoss", primaryBossUuid);
        tag.putInt("PartySize", partySize);
        tag.putDouble("HealthMultiplier", healthMultiplier);
        tag.putLong("StartedAt", startedAt);
        tag.putInt("BossStage", bossStage);
        ListTag participantTags = new ListTag();
        participants.forEach((player, state) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", player);
            entry.putString("EntryState", state.name());
            participantTags.add(entry);
        });
        tag.put("Participants", participantTags);
        return tag;
    }

    public static BossAttemptState load(CompoundTag tag, long fallbackGeneration, int fallbackStage) {
        UUID id = tag.hasUUID("AttemptId") ? tag.getUUID("AttemptId") : UUID.randomUUID();
        UUID primary = tag.hasUUID("PrimaryBoss") ? tag.getUUID("PrimaryBoss") : NIL_UUID;
        long generation = tag.contains("Generation") ? Math.max(0L, tag.getLong("Generation")) : fallbackGeneration;
        int party = tag.contains("PartySize") ? tag.getInt("PartySize") : 1;
        double multiplier = tag.contains("HealthMultiplier") ? tag.getDouble("HealthMultiplier") : 1.0D;
        long started = tag.contains("StartedAt") ? tag.getLong("StartedAt") : System.currentTimeMillis();
        int stage = tag.contains("BossStage") ? tag.getInt("BossStage") : fallbackStage;
        BossAttemptState state = new BossAttemptState(id, generation, primary, party, multiplier, started, stage);
        ListTag participantTags = tag.getList("Participants", Tag.TAG_COMPOUND);
        for (int index = 0; index < participantTags.size(); index++) {
            CompoundTag entry = participantTags.getCompound(index);
            if (!entry.hasUUID("Player")) continue;
            try {
                state.participants.put(entry.getUUID("Player"),
                        BossParticipantEntryState.valueOf(entry.getString("EntryState")));
            } catch (IllegalArgumentException ignored) {
                state.participants.put(entry.getUUID("Player"), BossParticipantEntryState.OUTSIDE);
            }
        }
        return state;
    }
}
