package dev.maplesadventure.multiplayer.phase.mob;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent, synchronized identity attached only to explicitly phased mobs. */
public final class MobPhaseState implements INBTSerializable<CompoundTag> {
    private static final UUID NIL_UUID = new UUID(0L, 0L);
    public static final StreamCodec<RegistryFriendlyByteBuf, MobPhaseState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MobPhaseState decode(RegistryFriendlyByteBuf buffer) {
            return new MobPhaseState(new PhaseId(buffer.readUUID()), buffer.readUUID(), buffer.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MobPhaseState state) {
            buffer.writeUUID(state.phaseId.value());
            buffer.writeUUID(state.instanceGroupId);
            buffer.writeBoolean(state.prototype);
        }
    };

    private PhaseId phaseId;
    private UUID instanceGroupId;
    private boolean prototype;

    public MobPhaseState() {
        this(new PhaseId(NIL_UUID), NIL_UUID, false);
    }

    public MobPhaseState(PhaseId phaseId, UUID instanceGroupId, boolean prototype) {
        this.phaseId = phaseId;
        this.instanceGroupId = instanceGroupId;
        this.prototype = prototype;
    }

    public static MobPhaseState prototype(PhaseId phaseId) {
        return new MobPhaseState(phaseId, UUID.randomUUID(), true);
    }

    public static MobPhaseState encounter(PhaseId phaseId, UUID encounterInstanceId) {
        return new MobPhaseState(phaseId, encounterInstanceId, false);
    }

    public PhaseId phaseId() { return phaseId; }
    public UUID instanceGroupId() { return instanceGroupId; }
    public boolean prototype() { return prototype; }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PhaseId", phaseId.value());
        tag.putUUID("InstanceGroupId", instanceGroupId);
        tag.putBoolean("Prototype", prototype);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.hasUUID("PhaseId")) phaseId = new PhaseId(tag.getUUID("PhaseId"));
        if (tag.hasUUID("InstanceGroupId")) instanceGroupId = tag.getUUID("InstanceGroupId");
        prototype = tag.getBoolean("Prototype");
    }
}
