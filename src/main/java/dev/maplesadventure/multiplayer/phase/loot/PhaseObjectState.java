package dev.maplesadventure.multiplayer.phase.loot;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent and tracking-synchronized provenance for encounter items and experience orbs. */
public final class PhaseObjectState implements INBTSerializable<CompoundTag> {
    public static final StreamCodec<RegistryFriendlyByteBuf, PhaseObjectState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PhaseObjectState decode(RegistryFriendlyByteBuf buffer) {
            return new PhaseObjectState(new PhaseId(buffer.readUUID()), buffer.readResourceLocation(),
                    buffer.readVarLong(), buffer.readEnum(PhaseObjectOrigin.class));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PhaseObjectState state) {
            buffer.writeUUID(state.phaseId.value());
            buffer.writeResourceLocation(state.encounterId);
            buffer.writeVarLong(state.generation);
            buffer.writeEnum(state.origin);
        }
    };

    private PhaseId phaseId;
    private ResourceLocation encounterId;
    private long generation;
    private PhaseObjectOrigin origin;

    public PhaseObjectState() {
        this(PhaseId.solo(new java.util.UUID(0L, 0L)),
                ResourceLocation.fromNamespaceAndPath("maplesadventure", "invalid"),
                0L, PhaseObjectOrigin.COMMON_ENCOUNTER);
    }

    public PhaseObjectState(PhaseId phaseId, ResourceLocation encounterId, long generation,
                            PhaseObjectOrigin origin) {
        this.phaseId = phaseId;
        this.encounterId = encounterId;
        this.generation = Math.max(0L, generation);
        this.origin = origin;
    }

    public PhaseId phaseId() { return phaseId; }
    public ResourceLocation encounterId() { return encounterId; }
    public long generation() { return generation; }
    public PhaseObjectOrigin origin() { return origin; }
    public PhaseObjectState copy() { return new PhaseObjectState(phaseId, encounterId, generation, origin); }

    /** Merge identity includes lifecycle provenance, preventing reset semantics from being contaminated. */
    public boolean canMergeWith(PhaseObjectState other) {
        return phaseId.equals(other.phaseId)
                && encounterId.equals(other.encounterId)
                && generation == other.generation
                && origin == other.origin;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Phase", phaseId.value());
        tag.putString("Encounter", encounterId.toString());
        tag.putLong("Generation", generation);
        tag.putString("Origin", origin.name());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.hasUUID("Phase")) phaseId = new PhaseId(tag.getUUID("Phase"));
        ResourceLocation parsed = ResourceLocation.tryParse(tag.getString("Encounter"));
        if (parsed != null) encounterId = parsed;
        generation = Math.max(0L, tag.getLong("Generation"));
        try {
            origin = PhaseObjectOrigin.valueOf(tag.getString("Origin"));
        } catch (IllegalArgumentException ignored) {
            origin = PhaseObjectOrigin.COMMON_ENCOUNTER;
        }
    }
}
