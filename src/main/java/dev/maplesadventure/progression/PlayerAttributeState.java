package dev.maplesadventure.progression;

import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent player-owned base attributes. Adventure level is intentionally not stored. */
public final class PlayerAttributeState implements INBTSerializable<CompoundTag> {
    private int dataVersion;
    private EnumMap<Attribute, Integer> values;
    private transient boolean correctedOnLoad;
    private transient int sourceVersion;

    public PlayerAttributeState() {
        this(PlayerAttributeMigration.CURRENT_VERSION, defaults(), false, PlayerAttributeMigration.CURRENT_VERSION);
    }

    private PlayerAttributeState(int dataVersion, Map<Attribute, Integer> values,
                                 boolean correctedOnLoad, int sourceVersion) {
        this.dataVersion = dataVersion;
        this.values = new EnumMap<>(Attribute.class);
        for (Attribute attribute : Attribute.values()) {
            this.values.put(attribute, Math.clamp(values.getOrDefault(attribute,
                    PlayerAttributeMigration.MINIMUM_VALUE), PlayerAttributeMigration.MINIMUM_VALUE,
                    PlayerAttributeMigration.ABSOLUTE_HARD_CAP));
        }
        this.correctedOnLoad = correctedOnLoad;
        this.sourceVersion = sourceVersion;
    }

    public static PlayerAttributeState defaultsState() { return new PlayerAttributeState(); }
    public static PlayerAttributeState fromValues(int dataVersion, Map<Attribute, Integer> values) {
        return new PlayerAttributeState(dataVersion, values, false, dataVersion);
    }
    public int dataVersion() { return dataVersion; }
    public int get(Attribute attribute) { return values.get(attribute); }
    public Map<Attribute, Integer> values() { return Map.copyOf(values); }
    public boolean correctedOnLoad() { return correctedOnLoad; }
    public int sourceVersion() { return sourceVersion; }

    public PlayerAttributeState with(Attribute attribute, int value, int hardCap) {
        EnumMap<Attribute, Integer> copy = new EnumMap<>(values);
        copy.put(attribute, Math.clamp(value, PlayerAttributeMigration.MINIMUM_VALUE,
                Math.min(PlayerAttributeMigration.ABSOLUTE_HARD_CAP, Math.max(hardCap,
                        PlayerAttributeMigration.MINIMUM_VALUE))));
        return new PlayerAttributeState(PlayerAttributeMigration.CURRENT_VERSION, copy, false,
                PlayerAttributeMigration.CURRENT_VERSION);
    }

    public PlayerAttributeState cleanCopy() {
        return new PlayerAttributeState(PlayerAttributeMigration.CURRENT_VERSION, values, false,
                PlayerAttributeMigration.CURRENT_VERSION);
    }

    public PlayerAttributeState clampToCap(int hardCap) {
        PlayerAttributeState result = cleanCopy();
        for (Attribute attribute : Attribute.values()) result = result.with(attribute, result.get(attribute), hardCap);
        return result;
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", PlayerAttributeMigration.CURRENT_VERSION);
        for (Attribute attribute : Attribute.values())
            tag.putInt(PlayerAttributeMigration.nbtKey(attribute), get(attribute));
        return tag;
    }

    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        PlayerAttributeMigration.Result migrated = PlayerAttributeMigration.migrate(tag);
        dataVersion = migrated.dataVersion();
        values = new EnumMap<>(Attribute.class);
        values.putAll(migrated.values());
        correctedOnLoad = migrated.corrected();
        sourceVersion = migrated.sourceVersion();
    }

    private static EnumMap<Attribute, Integer> defaults() {
        EnumMap<Attribute, Integer> result = new EnumMap<>(Attribute.class);
        for (Attribute attribute : Attribute.values()) result.put(attribute, PlayerAttributeMigration.MINIMUM_VALUE);
        return result;
    }
}
