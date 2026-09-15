package dev.maplesadventure.progression.defense;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Persistent reference only; resolved from current server data at hit time. Never synced to clients. */
public final class EntityDefenseProfileRef implements INBTSerializable<CompoundTag> {
    public static final int CURRENT_VERSION = 1;
    private ResourceLocation profileId = EntityDefenseProfile.NONE.profileId();
    private int dataVersion = CURRENT_VERSION;
    public EntityDefenseProfileRef() {}
    public EntityDefenseProfileRef(ResourceLocation id) {
        if (id == null || id.toString().length() > 256) throw new IllegalArgumentException("Profile ID bounds");
        profileId = id;
    }
    public ResourceLocation profileId() { return profileId; }
    public int dataVersion() { return dataVersion; }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        var tag = new CompoundTag(); tag.putString("profile", profileId.toString()); tag.putInt("dataVersion", dataVersion); return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        String id = tag.getString("profile");
        var parsed = id.length() <= 256 ? ResourceLocation.tryParse(id) : null;
        profileId = parsed == null ? EntityDefenseProfile.NONE.profileId() : parsed;
        dataVersion = tag.contains("dataVersion") ? tag.getInt("dataVersion") : CURRENT_VERSION;
        if (parsed == null) dev.maplesadventure.MaplesAdventure.LOGGER.warn("Invalid entity defense reference; using NONE");
    }
}
