package dev.maplesadventure.bonfire;

import net.minecraft.nbt.CompoundTag;
import java.util.Optional;

public record BonfireRestPoint(BonfireRef ref, float yaw) {
    public CompoundTag save() {
        CompoundTag tag = ref.save();
        tag.putFloat("Yaw", yaw);
        return tag;
    }
    public static Optional<BonfireRestPoint> load(CompoundTag tag) {
        float yaw = tag.getFloat("Yaw");
        if (!Float.isFinite(yaw)) return Optional.empty();
        return BonfireRef.load(tag).map(ref -> new BonfireRestPoint(ref, yaw));
    }
}
