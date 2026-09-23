package dev.maplesadventure.bonfire;

import dev.maplesadventure.progression.ProgressionAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Resolves a loaded block at an exact location; never force-loads for interaction. */
public final class BonfireStateService {
    public static PlayerBonfireState state(ServerPlayer player) { return player.getData(ProgressionAttachments.PLAYER_BONFIRES); }
    public static BonfireBlockEntity resolve(ServerLevel level, BlockPos pos) {
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getBlockEntity(pos) instanceof BonfireBlockEntity entity ? entity : null;
    }
    public static boolean matches(ServerLevel level, BonfireRef ref) {
        if (!level.dimension().location().equals(ref.dimension())) return false;
        BonfireBlockEntity entity = resolve(level, ref.pos());
        return entity != null && entity.generation().equals(ref.generation());
    }
    public static boolean closeEnough(ServerPlayer player, BlockPos pos) {
        double x = Math.max(pos.getX(), Math.min(player.getX(), pos.getX() + 1.0));
        double y = Math.max(pos.getY(), Math.min(player.getY() + player.getBbHeight() * .5, pos.getY() + 1.0));
        double z = Math.max(pos.getZ(), Math.min(player.getZ(), pos.getZ() + 1.0));
        return player.position().add(0, player.getBbHeight() * .5, 0).distanceToSqr(x, y, z) <= 1.25 * 1.25;
    }
    private BonfireStateService() {}
}
