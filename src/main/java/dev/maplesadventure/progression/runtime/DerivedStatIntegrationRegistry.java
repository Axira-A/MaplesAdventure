package dev.maplesadventure.progression.runtime;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.PlayerAttributeState;
import dev.maplesadventure.progression.stats.StatImplementationState;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.OptionalDouble;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.ModList;

/** Owns optional integration lifecycle and turns a broken adapter into an honest preview-only resource. */
public final class DerivedStatIntegrationRegistry {
    private static final Map<DerivedRuntimeResource, DerivedStatRuntimeAdapter> ADAPTERS =
            new EnumMap<>(DerivedRuntimeResource.class);
    private static final EnumSet<DerivedRuntimeResource> FAILED = EnumSet.noneOf(DerivedRuntimeResource.class);
    private static boolean initialized;

    private static synchronized void initialize() {
        if (initialized) return;
        initialized = true;
        ADAPTERS.put(DerivedRuntimeResource.HEALTH, new VanillaHealthAdapter());
        loadOptional("epicfight", "dev.maplesadventure.integration.epicfight.progression.EpicFightStaminaAdapter");
        loadOptional("irons_spellbooks", "dev.maplesadventure.integration.ironsspellbooks.progression.IronsManaAdapter");
    }

    private static void loadOptional(String modId, String className) {
        if (!ModList.get().isLoaded(modId)) return;
        try {
            var adapter = (DerivedStatRuntimeAdapter) Class.forName(className).getConstructor().newInstance();
            ADAPTERS.put(adapter.resource(), adapter);
            MaplesAdventure.LOGGER.info("Enabled derived-stat runtime adapter {} for {}", className, modId);
        } catch (ReflectiveOperationException | LinkageError failure) {
            MaplesAdventure.LOGGER.error("Could not initialize derived-stat adapter for {}; resource stays preview-only",
                    modId, failure);
        }
    }

    static RuntimeResourceValue refresh(DerivedRuntimeResource resource, ServerPlayer player,
                                        PlayerAttributeState state, DerivedStatRefreshReason reason) {
        initialize();
        if (FAILED.contains(resource)) return RuntimeResourceValue.previewOnly(resource.formula(state));
        DerivedStatRuntimeAdapter adapter = ADAPTERS.get(resource);
        if (adapter == null) return RuntimeResourceValue.previewOnly(resource.formula(state));
        try {
            return adapter.refresh(player, state, reason);
        } catch (RuntimeException | LinkageError failure) {
            FAILED.add(resource);
            MaplesAdventure.LOGGER.error("Disabled failing {} runtime integration for this server session; progression data remains valid",
                    resource, failure);
            return RuntimeResourceValue.previewOnly(resource.formula(state));
        }
    }

    static RuntimeResourceValue inspect(DerivedRuntimeResource resource, ServerPlayer player,
                                        PlayerAttributeState state) {
        initialize();
        if (FAILED.contains(resource)) return RuntimeResourceValue.previewOnly(resource.formula(state));
        DerivedStatRuntimeAdapter adapter = ADAPTERS.get(resource);
        if (adapter == null) return RuntimeResourceValue.previewOnly(resource.formula(state));
        try {
            return adapter.inspect(player, state);
        } catch (RuntimeException | LinkageError failure) {
            FAILED.add(resource);
            MaplesAdventure.LOGGER.error("Failed to inspect {} runtime integration; resource stays preview-only",
                    resource, failure);
            return RuntimeResourceValue.previewOnly(resource.formula(state));
        }
    }

    public static StatImplementationState state(DerivedRuntimeResource resource) {
        initialize();
        return ADAPTERS.containsKey(resource) && !FAILED.contains(resource)
                ? StatImplementationState.ACTIVE : StatImplementationState.PREVIEW_ONLY;
    }

    static OptionalDouble captureCurrentRatio(DerivedRuntimeResource resource, ServerPlayer player) {
        initialize();
        if (FAILED.contains(resource)) return OptionalDouble.empty();
        DerivedStatRuntimeAdapter adapter = ADAPTERS.get(resource);
        if (adapter == null) return OptionalDouble.empty();
        try {
            return adapter.captureCurrentRatio(player);
        } catch (RuntimeException | LinkageError failure) {
            MaplesAdventure.LOGGER.warn("Could not checkpoint {} current ratio for {}", resource,
                    player.getGameProfile().getName(), failure);
            return OptionalDouble.empty();
        }
    }

    static void restoreCurrentRatio(DerivedRuntimeResource resource, ServerPlayer player, double ratio) {
        initialize();
        if (FAILED.contains(resource)) return;
        DerivedStatRuntimeAdapter adapter = ADAPTERS.get(resource);
        if (adapter == null) return;
        try {
            adapter.restoreCurrentRatio(player, ratio);
        } catch (RuntimeException | LinkageError failure) {
            MaplesAdventure.LOGGER.warn("Could not restore {} current ratio for {}", resource,
                    player.getGameProfile().getName(), failure);
        }
    }

    static synchronized void clearFailures() { FAILED.clear(); }
    static boolean consumeExact(DerivedRuntimeResource resource,ServerPlayer player,double amount) {
        initialize(); var adapter=ADAPTERS.get(resource);
        if(adapter==null||FAILED.contains(resource)) return false;
        try { return adapter.consumeExact(player,amount); }
        catch(RuntimeException|LinkageError error) { MaplesAdventure.LOGGER.warn("Cannot drain {}: {}",resource,error.getMessage()); return false; }
    }
    private DerivedStatIntegrationRegistry() {}
}
