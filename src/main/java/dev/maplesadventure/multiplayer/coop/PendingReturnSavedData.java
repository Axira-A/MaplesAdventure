package dev.maplesadventure.multiplayer.coop;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Crash-safe return locations; live co-op sessions themselves intentionally remain transient. */
public final class PendingReturnSavedData extends SavedData {
    private static final String DATA_NAME = "maplesadventure_pending_coop_returns";
    private static final Factory<PendingReturnSavedData> FACTORY = new Factory<>(
            PendingReturnSavedData::new, PendingReturnSavedData::load
    );
    private final Map<UUID, ReturnContext> pending = new HashMap<>();

    public static PendingReturnSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }

    public Optional<ReturnContext> get(UUID playerUuid) { return Optional.ofNullable(pending.get(playerUuid)); }

    public int size() { return pending.size(); }
    public boolean contains(UUID playerUuid) { return pending.containsKey(playerUuid); }

    public void put(UUID playerUuid, ReturnContext context) {
        pending.put(playerUuid, context);
        setDirty();
    }

    public Optional<ReturnContext> remove(UUID playerUuid) {
        ReturnContext removed = pending.remove(playerUuid);
        if (removed != null) setDirty();
        return Optional.ofNullable(removed);
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        pending.forEach((playerUuid, context) -> {
            CompoundTag entry = context.save();
            entry.putUUID("Player", playerUuid);
            entries.add(entry);
        });
        tag.put("PendingReturns", entries);
        return tag;
    }

    private static PendingReturnSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingReturnSavedData data = new PendingReturnSavedData();
        ListTag entries = tag.getList("PendingReturns", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            CompoundTag entry = entries.getCompound(index);
            if (!entry.hasUUID("Player")) continue;
            ReturnContext.load(entry).ifPresent(context -> data.pending.put(entry.getUUID("Player"), context));
        }
        return data;
    }
}
