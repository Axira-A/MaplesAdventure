package dev.maplesadventure.interaction;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

public record BlockInteractionTarget(BlockPos pos, Block expectedBlock) implements InteractionTarget {
    public BlockInteractionTarget {
        pos = pos.immutable();
    }

    @Override
    public Kind kind() {
        return Kind.BLOCK;
    }

    @Override
    public Vec3 markerPosition(ClientLevel level, float partialTick) {
        return Vec3.atCenterOf(pos);
    }

    @Override
    public long stableSortKey() {
        return pos.asLong();
    }

    @Override
    public String debugDescription(ClientLevel level) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()) + "@" + pos.toShortString();
    }
}
