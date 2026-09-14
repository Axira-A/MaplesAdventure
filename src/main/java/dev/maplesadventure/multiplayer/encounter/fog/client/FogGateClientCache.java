package dev.maplesadventure.multiplayer.encounter.fog.client;

import dev.maplesadventure.multiplayer.encounter.fog.FogGateClientView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

public final class FogGateClientCache {
    private static final Map<UUID, FogGateClientView> GATES = new LinkedHashMap<>();
    private static final Map<Long, UUID> BY_BLOCK = new HashMap<>();
    private static boolean initialized;

    public static void replace(List<FogGateClientView> views) {
        GATES.clear(); BY_BLOCK.clear();
        for (FogGateClientView view : views) {
            GATES.put(view.gateId(), view);
            for (long block : view.blocks()) BY_BLOCK.put(block, view.gateId());
        }
        initialized = true;
    }
    public static Optional<FogGateClientView> get(UUID id) { return Optional.ofNullable(GATES.get(id)); }
    public static Optional<FogGateClientView> at(BlockPos pos) {
        UUID id = BY_BLOCK.get(pos.asLong());
        return Optional.ofNullable(id == null ? null : GATES.get(id));
    }
    public static List<FogGateClientView> inBounds(AABB bounds) {
        ArrayList<FogGateClientView> result = new ArrayList<>();
        for (FogGateClientView view : GATES.values()) {
            for (long packed : view.blocks()) if (bounds.contains(BlockPos.of(packed).getCenter())) {
                result.add(view); break;
            }
        }
        return result;
    }
    public static boolean initialized() { return initialized; }
    public static void clear() { GATES.clear(); BY_BLOCK.clear(); initialized = false; }
    private FogGateClientCache() {}
}
