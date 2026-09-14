package dev.maplesadventure.multiplayer.coop;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public record SummonSignSummary(UUID signId, UUID ownerUuid, Vec3 position, BlockPos supportPos, float yaw,
                                SummonSignType type) {
}
