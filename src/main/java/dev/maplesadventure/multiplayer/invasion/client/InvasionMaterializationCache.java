package dev.maplesadventure.multiplayer.invasion.client;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.world.phys.Vec3;

public final class InvasionMaterializationCache {
    private static final Map<UUID, Visual> VISUALS = new LinkedHashMap<>();
    public static void add(UUID source, Vec3 position, float yaw, int ticks) {
        VISUALS.put(source, new Visual(source, position, yaw, ticks, ticks));
    }
    public static void tick() {
        VISUALS.replaceAll((id, visual) -> visual.tick());
        VISUALS.values().removeIf(Visual::finished);
    }
    public static void remove(UUID source) { VISUALS.remove(source); }
    public static Collection<Visual> all() { return List.copyOf(VISUALS.values()); }
    public static int playerAlpha(UUID source) {
        Visual visual = VISUALS.get(source);
        if (visual == null) return 255;
        int elapsed = visual.total - visual.remaining;
        return Math.max(12, Math.min(255, Math.round(255.0F * elapsed / Math.max(1, visual.total))));
    }
    public static void clear() { VISUALS.clear(); }
    public record Visual(UUID source, Vec3 position, float yaw, int remaining, int total) {
        Visual tick() { return new Visual(source, position, yaw, remaining - 1, total); }
        boolean finished() { return remaining <= 0; }
        public float alpha() {
            if (total <= 0) return 0.0F;
            int elapsed = total - remaining;
            float fadeIn = Math.min(1.0F, elapsed / 8.0F);
            float fadeOut = Math.min(1.0F, remaining / 10.0F);
            return Math.max(0.0F, Math.min(fadeIn, fadeOut));
        }
    }
    private InvasionMaterializationCache() {}
}
