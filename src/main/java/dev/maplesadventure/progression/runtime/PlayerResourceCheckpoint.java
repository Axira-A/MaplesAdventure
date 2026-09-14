package dev.maplesadventure.progression.runtime;

import java.util.EnumMap;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Persistent logout checkpoint for third-party current-resource ratios.
 *
 * <p>This is not a second mana/stamina store: the installed mod remains authoritative while the
 * player is online. The checkpoint is written only on logout, consumed once after the next login,
 * and exists solely to stop optional integrations from turning a relog into a refill.</p>
 */
public final class PlayerResourceCheckpoint implements INBTSerializable<CompoundTag> {
    private static final int DATA_VERSION = 1;
    private final EnumMap<DerivedRuntimeResource, Double> ratios =
            new EnumMap<>(DerivedRuntimeResource.class);

    public OptionalDouble ratio(DerivedRuntimeResource resource) {
        Double value = ratios.get(resource);
        return value == null ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public void put(DerivedRuntimeResource resource, double ratio) {
        if (Double.isFinite(ratio)) ratios.put(resource, Math.clamp(ratio, 0.0D, 1.0D));
    }

    public boolean isEmpty() { return ratios.isEmpty(); }

    public Map<DerivedRuntimeResource, Double> values() { return Map.copyOf(ratios); }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION);
        for (var entry : ratios.entrySet()) tag.putDouble(entry.getKey().name(), entry.getValue());
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        ratios.clear();
        for (DerivedRuntimeResource resource : DerivedRuntimeResource.values()) {
            if (tag.contains(resource.name())) put(resource, tag.getDouble(resource.name()));
        }
    }
}
