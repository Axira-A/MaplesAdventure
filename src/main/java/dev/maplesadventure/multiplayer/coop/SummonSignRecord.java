package dev.maplesadventure.multiplayer.coop;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public record SummonSignRecord(
        UUID signId,
        UUID ownerUuid,
        ResourceKey<Level> dimension,
        Vec3 position,
        BlockPos supportPos,
        float yaw,
        long createdAt,
        SummonSignType type,
        SummonSignState state
) {
    public SummonSignRecord claimed() {
        return new SummonSignRecord(signId, ownerUuid, dimension, position, supportPos, yaw, createdAt, type,
                SummonSignState.CLAIMED);
    }

    public SummonSignSummary summary() {
        return new SummonSignSummary(signId, ownerUuid, position, supportPos, yaw, type);
    }
}
