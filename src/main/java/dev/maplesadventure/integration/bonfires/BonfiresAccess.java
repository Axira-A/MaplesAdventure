package dev.maplesadventure.integration.bonfires;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.fml.ModList;

/** Reflection-only bridge to Bonfires 1.2.20b public block-entity API. */
final class BonfiresAccess {
    private record Api(java.lang.reflect.Method lit, java.lang.reflect.Method bonfire, java.lang.reflect.Method id) {}
    private static final ClassValue<Api> API = new ClassValue<>() {
        @Override protected Api computeValue(Class<?> type) {
            try { return new Api(type.getMethod("isLit"), type.getMethod("isBonfire"), type.getMethod("getID")); }
            catch (ReflectiveOperationException failure) { throw new IllegalStateException(failure); }
        }
    };

    static UUID identity(BlockEntity entity) {
        if (!ModList.get().isLoaded("bonfires") || entity == null || entity.isRemoved()
                || !entity.getClass().getName().equals("wehavecookies56.bonfires.tiles.BonfireTileEntity")) return null;
        try {
            Api api = API.get(entity.getClass());
            return Boolean.TRUE.equals(api.lit.invoke(entity)) && Boolean.TRUE.equals(api.bonfire.invoke(entity))
                    ? (UUID) api.id.invoke(entity) : null;
        } catch (ReflectiveOperationException | RuntimeException failure) { return null; }
    }

    static boolean reachable(ServerPlayer player, BlockPos pos) {
        var level = player.serverLevel();
        if (!level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                || !player.canInteractWithBlock(pos, 0.0D)) return false;
        var shape = level.getBlockState(pos).getShape(level, pos, CollisionContext.of(player));
        for (var box : shape.toAabbs()) {
            var target = box.move(pos);
            var body = player.getBoundingBox();
            double dx = Math.max(0, Math.max(body.minX - target.maxX, target.minX - body.maxX));
            double dy = Math.max(0, Math.max(body.minY - target.maxY, target.minY - body.maxY));
            double dz = Math.max(0, Math.max(body.minZ - target.maxZ, target.minZ - body.maxZ));
            if (dx * dx + dy * dy + dz * dz > 1.25D * 1.25D) continue;
            var hit = level.clip(new ClipContext(player.getEyePosition(), target.getCenter(),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
            if (hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos)) return true;
        }
        return false;
    }
    private BonfiresAccess() {}
}
