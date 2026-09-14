package dev.maplesadventure.multiplayer.encounter.fog;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.Arrays;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Compact persisted volume with O(1) membership. */
public final class BossRoomVolume {
    private static final BossRoomVolume EMPTY = new BossRoomVolume(new long[0], new long[0]);
    private final long[] packedInterior;
    private final long[] packedBoundary;
    private transient LongOpenHashSet interiorIndex;

    public BossRoomVolume(long[] interior, long[] boundary) {
        this.packedInterior = sortedUnique(interior);
        this.packedBoundary = sortedUnique(boundary);
    }

    public boolean isInside(BlockPos pos) { return index().contains(pos.asLong()); }
    public boolean isInside(Entity entity) { return isInside(BlockPos.containing(entity.getX(), entity.getY() + 0.1D, entity.getZ())); }
    public boolean isOutside(Entity entity) { return !isInside(entity); }
    public long[] interior() { return packedInterior.clone(); }
    public long[] boundary() { return packedBoundary.clone(); }
    public int size() { return packedInterior.length; }
    public boolean isEmpty() { return packedInterior.length == 0 && packedBoundary.length == 0; }
    public static BossRoomVolume empty() { return EMPTY; }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLongArray("Interior", packedInterior);
        tag.putLongArray("Boundary", packedBoundary);
        return tag;
    }

    public static BossRoomVolume load(CompoundTag tag) {
        return new BossRoomVolume(tag.getLongArray("Interior"), tag.getLongArray("Boundary"));
    }

    private LongOpenHashSet index() {
        if (interiorIndex == null) interiorIndex = new LongOpenHashSet(packedInterior);
        return interiorIndex;
    }

    private static long[] sortedUnique(long[] values) {
        long[] copy = values.clone();
        Arrays.sort(copy);
        if (copy.length < 2) return copy;
        int write = 1;
        for (int read = 1; read < copy.length; read++) if (copy[read] != copy[write - 1]) copy[write++] = copy[read];
        return Arrays.copyOf(copy, write);
    }
}
