package dev.maplesadventure.multiplayer.encounter;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class EncounterDebugService {
    private static final Set<UUID> ENABLED = new HashSet<>();

    public static boolean toggle(ServerPlayer player) {
        if (!ENABLED.add(player.getUUID())) { ENABLED.remove(player.getUUID()); return false; }
        return true;
    }
    public static void forget(UUID player) { ENABLED.remove(player); }
    public static void clear() { ENABLED.clear(); }
    public static boolean enabled(UUID player) { return ENABLED.contains(player); }

    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        EncounterSavedData data = EncounterSavedData.get(server);
        for (UUID uuid : Set.copyOf(ENABLED)) {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) { ENABLED.remove(uuid); continue; }
            ServerLevel level = player.serverLevel();
            showFogGateReason(player);
            for (EncounterDefinition definition : data.definitions()) {
                if (!definition.dimension().equals(level.dimension())
                        || player.distanceToSqr(definition.anchor()) > 128.0D * 128.0D) continue;
                level.sendParticles(player, ParticleTypes.END_ROD, true,
                        definition.anchor().x, definition.anchor().y + 0.2D, definition.anchor().z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                for (EncounterSpawnPoint point : definition.spawnPoints()) level.sendParticles(player,
                        definition.type() == EncounterType.BOSS ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME,
                        true, point.position().x, point.position().y + 0.25D, point.position().z,
                        1, 0.0D, 0.0D, 0.0D, 0.0D);
                double radius = definition.activationRadius();
                for (int i = 0; i < 16; i++) {
                    double angle = Math.PI * 2.0D * i / 16.0D;
                    level.sendParticles(player, ParticleTypes.WAX_ON, true,
                            definition.anchor().x + Math.cos(angle) * radius,
                            definition.anchor().y + 0.05D,
                            definition.anchor().z + Math.sin(angle) * radius,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
        }
    }
    private static void showFogGateReason(ServerPlayer player) {
        if (!player.isCreative() || !player.hasPermissions(2)) return;
        var hit = player.level().clip(new ClipContext(player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(8.0D)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK
                || !player.level().getBlockState(hit.getBlockPos()).is(dev.maplesadventure.registry.ModBlocks.BOSS_FOG_GATE.get())) return;
        var result = dev.maplesadventure.multiplayer.encounter.fog.FogGateDiagnostics.evaluate(player, hit.getBlockPos());
        player.displayClientMessage(Component.literal("FogGate: " + result.reason()), true);
    }
    private EncounterDebugService() {}
}
