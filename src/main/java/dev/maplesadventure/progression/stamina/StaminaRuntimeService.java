package dev.maplesadventure.progression.stamina;

import dev.maplesadventure.MaplesAdventure;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/**
 * Server-authoritative stamina regeneration/debt owner.
 * Epic Fight remains the visible stamina container; its native regeneration is bypassed separately.
 */
public final class StaminaRuntimeService {
    private static final String EPIC_BRIDGE =
            "dev.maplesadventure.integration.epicfight.progression.EpicFightStaminaRuntimeBridge";
    private static final Map<UUID, StaminaRuntimeState> STATES = new ConcurrentHashMap<>();
    private static volatile StaminaRuntimeBridge bridge;
    private static volatile boolean bridgeResolved;

    public static StaminaRuntimeState state(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), ignored -> new StaminaRuntimeState());
    }

    public static void tick(MinecraftServer server) {
        StaminaRuntimeBridge runtime = bridge();
        if (runtime == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) tickPlayer(player, runtime);
    }

    private static void tickPlayer(ServerPlayer player, StaminaRuntimeBridge runtime) {
        if (!runtime.available(player)) return;
        StaminaRuntimeState state = state(player);
        if (state.regenBlocked() || runtime.actionBlocksRegen(player)) return;

        double current = runtime.current(player);
        double maximum = runtime.maximum(player);
        if (!Double.isFinite(current) || !Double.isFinite(maximum) || maximum <= 0.0D) return;
        if (current >= maximum && !state.hasDebt()) return;

        double multiplier = Math.max(0.0D, runtime.regenMultiplier(player));
        double budget = StaminaPolicy.BASE_REGEN_PER_TICK * multiplier;
        if (!(budget > 0.0D) || !Double.isFinite(budget)) return;

        if (state.hasDebt()) {
            double repaid = Math.min(state.debt(), budget);
            state.debt(state.debt() - repaid);
            budget -= repaid;
        }
        if (budget > 0.0D && !state.hasDebt() && current < maximum) {
            runtime.setCurrent(player, Math.min(maximum, current + budget));
        }
    }

    /**
     * Souls-style final-action overspend. Intended for discrete actions only (attacks/dodges), never guard damage or drains.
     */
    public static boolean consumeDiscrete(ServerPlayer player, double cost) {
        StaminaRuntimeBridge runtime = bridge();
        if (runtime == null || !runtime.available(player) || !(cost > 0.0D) || !Double.isFinite(cost)) return false;
        StaminaRuntimeState state = state(player);
        if (state.hasDebt()) return false;
        double current = Math.max(0.0D, runtime.current(player));
        if (current <= 0.0D) return false;
        double deficit = Math.max(0.0D, cost - current);
        if (deficit > StaminaPolicy.MAX_STAMINA_DEBT) return false;
        runtime.setCurrent(player, Math.max(0.0D, current - cost));
        state.debt(deficit);
        return true;
    }

    public static void setGate(ServerPlayer player, StaminaRegenGate gate, boolean closed) {
        state(player).setGate(gate, closed);
    }

    public static void reset(ServerPlayer player, boolean refill) {
        StaminaRuntimeState state = state(player);
        state.reset();
        if (!refill) return;
        StaminaRuntimeBridge runtime = bridge();
        if (runtime != null && runtime.available(player)) runtime.setCurrent(player, runtime.maximum(player));
    }

    public static void forget(UUID playerId) { STATES.remove(playerId); }
    public static void clear() { STATES.clear(); bridge = null; bridgeResolved = false; }

    private static StaminaRuntimeBridge bridge() {
        if (bridgeResolved) return bridge;
        synchronized (StaminaRuntimeService.class) {
            if (bridgeResolved) return bridge;
            bridgeResolved = true;
            if (!ModList.get().isLoaded("epicfight")) return null;
            try {
                bridge = (StaminaRuntimeBridge) Class.forName(EPIC_BRIDGE).getConstructor().newInstance();
                MaplesAdventure.LOGGER.info("Enabled Maples stamina runtime bridge");
            } catch (ReflectiveOperationException | LinkageError failure) {
                MaplesAdventure.LOGGER.error("Maples stamina runtime bridge is unavailable", failure);
            }
            return bridge;
        }
    }

    private StaminaRuntimeService() {}
}
