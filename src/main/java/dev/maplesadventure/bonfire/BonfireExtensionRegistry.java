package dev.maplesadventure.bonfire;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.bonfire.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Captures immutable registration metadata; addon callbacks are the only untrusted boundary. */
public final class BonfireExtensionRegistry {
    public record Feature(ResourceLocation id, String translationKey, int order, MaplesBonfireFeatureHandler handler) {}
    private record Reset(ResourceLocation id, int priority, MaplesBonfireRestResetParticipant handler) {}
    private final Map<ResourceLocation, Feature> features = new HashMap<>();
    private final Map<ResourceLocation, Reset> resets = new HashMap<>();
    private boolean frozen;
    public synchronized void freeze() { frozen = true; }
    public synchronized void register(MaplesBonfireFeatureHandler handler) {
        if (frozen) throw new IllegalStateException("Bonfire registrations closed");
        Objects.requireNonNull(handler);
        ResourceLocation id = checkedId(handler.id());
        String key = Objects.requireNonNull(handler.translationKey());
        if (key.isBlank() || key.length() > 256 || !key.matches("[a-zA-Z0-9_.:/-]+"))
            throw new IllegalArgumentException("Invalid bonfire translation key");
        if (features.putIfAbsent(id, new Feature(id, key, handler.order(), handler)) != null)
            throw new IllegalArgumentException("Duplicate bonfire feature " + id);
    }
    public synchronized void register(MaplesBonfireRestResetParticipant handler) {
        if (frozen) throw new IllegalStateException("Bonfire registrations closed");
        Objects.requireNonNull(handler);
        ResourceLocation id = checkedId(handler.id());
        if (resets.putIfAbsent(id, new Reset(id, handler.priority(), handler)) != null)
            throw new IllegalArgumentException("Duplicate bonfire reset participant " + id);
    }
    private static ResourceLocation checkedId(ResourceLocation id) {
        Objects.requireNonNull(id);
        if (BonfireFeatureIds.parse(id.toString()) == null) throw new IllegalArgumentException("Invalid bonfire ID");
        return id;
    }
    public synchronized boolean registered(ResourceLocation id) { return features.containsKey(id); }
    public synchronized List<Feature> available(Set<ResourceLocation> configured, MaplesBonfireContext context) {
        return features.values().stream().filter(f -> configured.contains(f.id()))
                .sorted(Comparator.comparingInt(Feature::order).thenComparing(f -> f.id().toString()))
                .filter(f -> available(f, context)).toList();
    }
    private boolean available(Feature feature, MaplesBonfireContext context) {
        try { return feature.handler().isAvailable(context); }
        catch (RuntimeException | LinkageError failure) {
            MaplesAdventure.LOGGER.warn("Bonfire feature {} availability failed", feature.id(), failure);
            return false;
        }
    }
    public synchronized boolean execute(ResourceLocation id, Set<ResourceLocation> configured, MaplesBonfireContext context) {
        Feature feature = features.get(id);
        if (feature == null || !configured.contains(id) || !available(feature, context)) return false;
        try { feature.handler().execute(context); return true; }
        catch (RuntimeException | LinkageError failure) {
            MaplesAdventure.LOGGER.warn("Bonfire feature {} execution failed", id, failure);
            return false;
        }
    }
    public synchronized Set<ResourceLocation> reset(MaplesBonfireContext context) {
        Set<ResourceLocation> failures = new HashSet<>();
        resets.values().stream().sorted(Comparator.comparingInt(Reset::priority).thenComparing(r -> r.id().toString()))
                .toList().forEach(r -> {
                    try { r.handler().reset(context); }
                    catch (RuntimeException | LinkageError failure) {
                        failures.add(r.id());
                        MaplesAdventure.LOGGER.warn("Bonfire reset participant {} failed", r.id(), failure);
                    }
                });
        return Set.copyOf(failures);
    }
}
