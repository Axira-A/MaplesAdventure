package dev.maplesadventure.multiplayer.encounter.fog;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

public record FogGateClientView(UUID gateId, ResourceLocation encounterId, long[] blocks, Direction facing,
                                boolean render, boolean passable, boolean interactable) {
    public FogGateClientView { blocks = blocks.clone(); }
    @Override public long[] blocks() { return blocks.clone(); }
    public BlockPos representative() { return BlockPos.of(blocks[0]); }
}
