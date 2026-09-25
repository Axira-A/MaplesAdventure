package dev.maplesadventure.bonfire;

import dev.maplesadventure.MaplesAdventure;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Player-owned progress. Activation and last successful rest are separate transactions. */
public final class PlayerBonfireState implements INBTSerializable<CompoundTag> {
    public static final int DATA_VERSION = 1;
    public static final int MAX_ACTIVATED = 4096;
    private final Set<BonfireRef> activated = new LinkedHashSet<>();
    private BonfireRestPoint lastRested;

    public boolean isActivated(BonfireRef ref) { return activated.contains(ref); }
    public Set<BonfireRef> activated() { return Set.copyOf(activated); }
    public Optional<BonfireRestPoint> lastRested() { return Optional.ofNullable(lastRested); }
    public boolean activate(BonfireRef ref) {
        if (activated.contains(ref) || activated.size() >= MAX_ACTIVATED) return false;
        return activated.add(ref);
    }
    public boolean rest(BonfireRef ref, float yaw) {
        if (!activated.contains(ref)) return false;
        lastRested = new BonfireRestPoint(ref, yaw);
        return true;
    }
    public void reset() { activated.clear(); lastRested = null; }
    /** Only forget a confirmed stale placement, never a temporarily obstructed spawn point. */
    public void clearLastRested(BonfireRef expected) {
        if (lastRested != null && lastRested.ref().equals(expected)) lastRested = null;
    }

    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("DataVersion", DATA_VERSION);
        ListTag list = new ListTag();
        for (BonfireRef ref : activated) list.add(ref.save());
        tag.put("Activated", list);
        if (lastRested != null) tag.put("LastRested", lastRested.save());
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries, CompoundTag tag) {
        reset();
        ListTag list = tag.getList("Activated", Tag.TAG_COMPOUND);
        for (int i = 0; i < Math.min(list.size(), MAX_ACTIVATED); i++) {
            if (BonfireRef.load(list.getCompound(i)).map(activated::add).orElse(false)) continue;
            MaplesAdventure.LOGGER.warn("Discarded invalid player bonfire activation entry {}", i);
        }
        if (list.size() > MAX_ACTIVATED) MaplesAdventure.LOGGER.warn("Truncated oversized player bonfire progress");
        if (tag.contains("LastRested", Tag.TAG_COMPOUND))
            lastRested = BonfireRestPoint.load(tag.getCompound("LastRested")).filter(point -> activated.contains(point.ref())).orElse(null);
    }
}
