package dev.maplesadventure.progression.upgrade;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

/** One BONFIRE validator delegates to exact, separately registered sources. */
public final class BonfireUpgradeSourceRegistry {
    private static final Map<ResourceLocation, UpgradeAccessValidator> SOURCES = new HashMap<>();
    private static boolean installed;
    public static synchronized void register(ResourceLocation source, UpgradeAccessValidator validator) {
        if (!installed) {
            UpgradeAccessRegistry.register(UpgradeAccessType.BONFIRE, BonfireUpgradeSourceRegistry::validate);
            installed = true;
        }
        if (SOURCES.putIfAbsent(source, validator) != null)
            throw new IllegalStateException("Duplicate bonfire upgrade source: " + source);
    }
    private static boolean validate(net.minecraft.server.level.ServerPlayer player, UpgradeAccessContext context) {
        UpgradeAccessValidator source = SOURCES.get(context.sourceKey());
        return source != null && source.validate(player, context);
    }
    private BonfireUpgradeSourceRegistry() {}
}
