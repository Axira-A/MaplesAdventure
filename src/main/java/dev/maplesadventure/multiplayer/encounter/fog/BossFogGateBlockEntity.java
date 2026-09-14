package dev.maplesadventure.multiplayer.encounter.fog;

import dev.maplesadventure.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tick-free render anchor; authoritative gate data lives in EncounterSavedData. */
public final class BossFogGateBlockEntity extends BlockEntity {
    public BossFogGateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.BOSS_FOG_GATE_ENTITY.get(), pos, state);
    }
}
