package dev.maplesadventure.multiplayer.coop.client;

import dev.maplesadventure.multiplayer.coop.SummonSignSummary;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.phys.AABB;

public final class SummonSignRenderCache {
    private static final Map<UUID, SummonSignSummary> SIGNS = new LinkedHashMap<>();

    public static void replace(List<SummonSignSummary> signs) {
        SIGNS.clear();
        for (SummonSignSummary sign : signs) SIGNS.put(sign.signId(), sign);
    }
    public static void upsert(SummonSignSummary sign) { SIGNS.put(sign.signId(), sign); }
    public static void remove(UUID signId) { SIGNS.remove(signId); }
    public static Optional<SummonSignSummary> get(UUID signId) { return Optional.ofNullable(SIGNS.get(signId)); }
    public static Collection<SummonSignSummary> all() { return List.copyOf(SIGNS.values()); }
    public static List<SummonSignSummary> inBounds(AABB bounds) {
        ArrayList<SummonSignSummary> result = new ArrayList<>();
        for (SummonSignSummary sign : SIGNS.values()) if (bounds.contains(sign.position())) result.add(sign);
        return result;
    }
    public static void clear() { SIGNS.clear(); }
    private SummonSignRenderCache() {}
}
