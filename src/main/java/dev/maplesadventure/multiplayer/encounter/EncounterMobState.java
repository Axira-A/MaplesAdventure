package dev.maplesadventure.multiplayer.encounter;

import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent provenance for a fixed encounter entity; phase membership remains a separate attachment. */
public final class EncounterMobState implements INBTSerializable<CompoundTag> {
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    public static final StreamCodec<RegistryFriendlyByteBuf, EncounterMobState> STREAM_CODEC = new StreamCodec<>() {
        @Override public EncounterMobState decode(RegistryFriendlyByteBuf buffer) {
            return new EncounterMobState(buffer.readResourceLocation(), buffer.readUUID(),
                    buffer.readVarLong(), buffer.readBoolean(), buffer.readEnum(EncounterSpawnRole.class),
                    buffer.readUUID(), buffer.readVarInt(), buffer.readBoolean());
        }
        @Override public void encode(RegistryFriendlyByteBuf buffer, EncounterMobState state) {
            buffer.writeResourceLocation(state.encounterId);
            buffer.writeUUID(state.spawnPointId);
            buffer.writeVarLong(state.generation);
            buffer.writeBoolean(state.bossEncounter);
            buffer.writeEnum(state.role);
            buffer.writeUUID(state.attemptId);
            buffer.writeVarInt(state.bossStage);
            buffer.writeBoolean(state.completionRelevant);
        }
    };

    private ResourceLocation encounterId;
    private UUID spawnPointId;
    private long generation;
    private boolean bossEncounter;
    private EncounterSpawnRole role;
    private UUID attemptId;
    private int bossStage;
    private boolean completionRelevant;

    public EncounterMobState() {
        this(ResourceLocation.fromNamespaceAndPath("maplesadventure", "invalid"), NIL_UUID, 0L, false,
                EncounterSpawnRole.NORMAL, NIL_UUID, 1, true);
    }
    public EncounterMobState(ResourceLocation encounterId, UUID spawnPointId, long generation, boolean bossEncounter,
                             EncounterSpawnRole role, UUID attemptId, int bossStage,
                             boolean completionRelevant) {
        this.encounterId = encounterId;
        this.spawnPointId = spawnPointId;
        this.generation = Math.max(0L, generation);
        this.bossEncounter = bossEncounter;
        this.role = role;
        this.attemptId = attemptId == null ? NIL_UUID : attemptId;
        this.bossStage = Math.max(1, bossStage);
        this.completionRelevant = completionRelevant;
    }
    public ResourceLocation encounterId() { return encounterId; }
    public UUID spawnPointId() { return spawnPointId; }
    public long generation() { return generation; }
    public boolean boss() { return bossEncounter; }
    public EncounterSpawnRole role() { return role; }
    public UUID attemptId() { return attemptId; }
    public int bossStage() { return bossStage; }
    public boolean completionRelevant() { return completionRelevant; }
    public boolean isPrimary() { return role == EncounterSpawnRole.BOSS_PRIMARY; }

    public EncounterMobState withRoleAndAttempt(EncounterSpawnRole nextRole, UUID nextAttempt, int nextStage) {
        return new EncounterMobState(encounterId, spawnPointId, generation, bossEncounter, nextRole,
                nextAttempt, nextStage, completionRelevant);
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("Encounter", encounterId.toString());
        tag.putUUID("SpawnPoint", spawnPointId);
        tag.putLong("Generation", generation);
        tag.putBoolean("Boss", bossEncounter);
        tag.putString("Role", role.name());
        tag.putUUID("AttemptId", attemptId);
        tag.putInt("BossStage", bossStage);
        tag.putBoolean("CompletionRelevant", completionRelevant);
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Encounter"));
        if (parsed != null) encounterId = parsed;
        if (tag.hasUUID("SpawnPoint")) spawnPointId = tag.getUUID("SpawnPoint");
        generation = Math.max(0L, tag.getLong("Generation"));
        bossEncounter = tag.getBoolean("Boss");
        role = bossEncounter ? EncounterSpawnRole.BOSS_PRIMARY : EncounterSpawnRole.NORMAL;
        if (tag.contains("Role")) {
            try { role = EncounterSpawnRole.valueOf(tag.getString("Role")); }
            catch (IllegalArgumentException ignored) { /* legacy fallback above */ }
        }
        attemptId = tag.hasUUID("AttemptId") ? tag.getUUID("AttemptId") : NIL_UUID;
        bossStage = Math.max(1, tag.contains("BossStage") ? tag.getInt("BossStage") : 1);
        completionRelevant = !tag.contains("CompletionRelevant") || tag.getBoolean("CompletionRelevant");
    }
}
