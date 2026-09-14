package dev.maplesadventure.multiplayer.encounter.boss;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Persistent and tracking-synchronized identity for every entity belonging to a boss attempt.
 * It complements MobPhaseState/EncounterMobState and also works for non-Mob effects.
 */
public final class BossEntityLinkState implements INBTSerializable<CompoundTag> {
    public static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final ResourceLocation INVALID_ENCOUNTER =
            ResourceLocation.fromNamespaceAndPath(MaplesAdventure.MOD_ID, "invalid_boss_lineage");

    public static final StreamCodec<RegistryFriendlyByteBuf, BossEntityLinkState> STREAM_CODEC = new StreamCodec<>() {
        @Override public BossEntityLinkState decode(RegistryFriendlyByteBuf buffer) {
            return new BossEntityLinkState(buffer.readUUID(), new PhaseId(buffer.readUUID()),
                    buffer.readResourceLocation(), buffer.readVarLong(), buffer.readUUID(),
                    buffer.readEnum(BossEntityRole.class), buffer.readBoolean());
        }

        @Override public void encode(RegistryFriendlyByteBuf buffer, BossEntityLinkState state) {
            buffer.writeUUID(state.attemptId);
            buffer.writeUUID(state.phaseId.value());
            buffer.writeResourceLocation(state.encounterId);
            buffer.writeVarLong(state.generation);
            buffer.writeUUID(state.primaryBossUuid);
            buffer.writeEnum(state.role);
            buffer.writeBoolean(state.completionRelevant);
        }
    };

    private UUID attemptId;
    private PhaseId phaseId;
    private ResourceLocation encounterId;
    private long generation;
    private UUID primaryBossUuid;
    private BossEntityRole role;
    private boolean completionRelevant;

    public BossEntityLinkState() {
        this(NIL_UUID, new PhaseId(NIL_UUID), INVALID_ENCOUNTER, 0L, NIL_UUID,
                BossEntityRole.EFFECT, false);
    }

    public BossEntityLinkState(UUID attemptId, PhaseId phaseId, ResourceLocation encounterId, long generation,
                               UUID primaryBossUuid, BossEntityRole role, boolean completionRelevant) {
        this.attemptId = attemptId == null ? NIL_UUID : attemptId;
        this.phaseId = phaseId == null ? new PhaseId(NIL_UUID) : phaseId;
        this.encounterId = encounterId == null ? INVALID_ENCOUNTER : encounterId;
        this.generation = Math.max(0L, generation);
        this.primaryBossUuid = primaryBossUuid == null ? NIL_UUID : primaryBossUuid;
        this.role = role == null ? BossEntityRole.EFFECT : role;
        this.completionRelevant = completionRelevant;
    }

    public UUID attemptId() { return attemptId; }
    public PhaseId phaseId() { return phaseId; }
    public ResourceLocation encounterId() { return encounterId; }
    public long generation() { return generation; }
    public UUID primaryBossUuid() { return primaryBossUuid; }
    public BossEntityRole role() { return role; }
    public boolean completionRelevant() { return completionRelevant; }
    public boolean valid() {
        return !NIL_UUID.equals(attemptId) && !NIL_UUID.equals(phaseId.value())
                && !INVALID_ENCOUNTER.equals(encounterId);
    }

    public BossEntityLinkState withRole(BossEntityRole nextRole, boolean relevant) {
        return new BossEntityLinkState(attemptId, phaseId, encounterId, generation,
                primaryBossUuid, nextRole, relevant);
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("AttemptId", attemptId);
        tag.putUUID("PhaseId", phaseId.value());
        tag.putString("Encounter", encounterId.toString());
        tag.putLong("Generation", generation);
        tag.putUUID("PrimaryBoss", primaryBossUuid);
        tag.putString("Role", role.name());
        tag.putBoolean("CompletionRelevant", completionRelevant);
        return tag;
    }

    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        attemptId = tag.hasUUID("AttemptId") ? tag.getUUID("AttemptId") : NIL_UUID;
        phaseId = new PhaseId(tag.hasUUID("PhaseId") ? tag.getUUID("PhaseId") : NIL_UUID);
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Encounter"));
        encounterId = parsed == null ? INVALID_ENCOUNTER : parsed;
        generation = Math.max(0L, tag.getLong("Generation"));
        primaryBossUuid = tag.hasUUID("PrimaryBoss") ? tag.getUUID("PrimaryBoss") : NIL_UUID;
        try { role = BossEntityRole.valueOf(tag.getString("Role")); }
        catch (IllegalArgumentException exception) { role = BossEntityRole.EFFECT; }
        completionRelevant = tag.getBoolean("CompletionRelevant");
    }
}
