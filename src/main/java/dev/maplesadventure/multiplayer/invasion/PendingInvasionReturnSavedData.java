package dev.maplesadventure.multiplayer.invasion;

import dev.maplesadventure.multiplayer.coop.ReturnContext;
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

/** Crash-safe invader return records. Live invasion sessions deliberately remain transient. */
public final class PendingInvasionReturnSavedData extends SavedData {
    private static final String DATA_NAME = "maplesadventure_pending_invasion_returns";
    private static final Factory<PendingInvasionReturnSavedData> FACTORY = new Factory<>(
            PendingInvasionReturnSavedData::new, PendingInvasionReturnSavedData::load);
    private final Map<UUID, ReturnContext> pending = new HashMap<>();

    public static PendingInvasionReturnSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
    }
    public Optional<ReturnContext> get(UUID player) { return Optional.ofNullable(pending.get(player)); }
    public boolean contains(UUID player) { return pending.containsKey(player); }
    public void put(UUID player, ReturnContext context) { pending.put(player, context); setDirty(); }
    public Optional<ReturnContext> remove(UUID player) {
        ReturnContext removed = pending.remove(player);
        if (removed != null) setDirty();
        return Optional.ofNullable(removed);
    }

    @Override public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        pending.forEach((uuid, context) -> {
            CompoundTag entry = context.save();
            entry.putUUID("Player", uuid);
            list.add(entry);
        });
        tag.put("PendingReturns", list);
        return tag;
    }

    private static PendingInvasionReturnSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingInvasionReturnSavedData data = new PendingInvasionReturnSavedData();
        ListTag list = tag.getList("PendingReturns", Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) {
            CompoundTag entry = list.getCompound(index);
            if (entry.hasUUID("Player")) ReturnContext.load(entry)
                    .ifPresent(context -> data.pending.put(entry.getUUID("Player"), context));
        }
        return data;
    }
}
