package dev.maplesadventure.progression.defense;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.ProgressionAttachments;

public final class EntityDefenseService {
    public record Binding(ResourceLocation profileId, String source) {}
    public record Resolved(EntityDefenseProfile profile, ResourceLocation requestedId, String source, boolean unknown) {}
    private record Cache(Map<EntityType<?>, Binding> bindings, Map<ResourceLocation, EntityDefenseProfile> profiles) {}
    private static volatile Cache cache = new Cache(Map.of(), Map.of());

    /** Called at startup and after reload/tag rebinding; never on the attack hot path. */
    public static void compile() {
        var bindings = new IdentityHashMap<EntityType<?>, Binding>();
        var sorted = EntityDefenseRegistry.rules().stream().sorted(EntityDefenseRegistry.RULE_ORDER).toList();
        for (var type : BuiltInRegistries.ENTITY_TYPE) {
            selectRule(BuiltInRegistries.ENTITY_TYPE.getKey(type),
                    tag -> type.is(TagKey.create(Registries.ENTITY_TYPE, tag)), sorted)
                    .ifPresent(rule -> bindings.put(type, new Binding(rule.profile(), rule.entity() != null ? "EXACT " + rule.file() : "TAG " + rule.file())));
        }
        cache = new Cache(Collections.unmodifiableMap(bindings), EntityDefenseRegistry.profiles());
        MaplesAdventure.LOGGER.info("Compiled {} entity defense type bindings", bindings.size());
    }
    public static Optional<EntityDefenseRegistry.Rule> selectRule(ResourceLocation type, Predicate<ResourceLocation> hasTag,
            List<EntityDefenseRegistry.Rule> rules) {
        // Sorting here is reload-only and also keeps the public selection function deterministic for callers/tests.
        var sorted = rules.stream().sorted(EntityDefenseRegistry.RULE_ORDER).toList();
        var exact = sorted.stream().filter(rule -> type.equals(rule.entity())).findFirst();
        return exact.isPresent() ? exact : sorted.stream().filter(rule -> rule.tag() != null && hasTag.test(rule.tag())).findFirst();
    }
    public static Resolved resolve(LivingEntity entity) {
        if (entity instanceof Player) return new Resolved(EntityDefenseProfile.NONE, EntityDefenseProfile.NONE.profileId(), "PLAYER_EXCLUDED", false);
        var current = cache;
        var ref = entity.getExistingData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE).orElse(null);
        return resolveReference(ref, current.bindings().get(entity.getType()), current.profiles());
    }
    public static Resolved resolveReference(EntityDefenseProfileRef ref, Binding binding, Map<ResourceLocation, EntityDefenseProfile> profiles) {
        if (ref == null && binding == null) return new Resolved(EntityDefenseProfile.NONE, EntityDefenseProfile.NONE.profileId(), "NONE", false);
        var id = ref != null ? ref.profileId() : binding.profileId();
        String source = ref != null ? "EXPLICIT" : binding.source();
        var profile = id.equals(EntityDefenseProfile.NONE.profileId()) ? EntityDefenseProfile.NONE : profiles.get(id);
        boolean unknown = profile == null || ref != null && ref.dataVersion() != EntityDefenseProfileRef.CURRENT_VERSION;
        return new Resolved(unknown ? EntityDefenseProfile.NONE : profile, id, unknown ? source + " UNKNOWN_PROFILE" : source, unknown);
    }
    public static void assign(LivingEntity entity, ResourceLocation id) {
        if (entity.level().isClientSide() || entity instanceof Player) throw new IllegalArgumentException("Server non-player living entity required");
        if (!id.equals(EntityDefenseProfile.NONE.profileId()) && !cache.profiles().containsKey(id)) throw new IllegalArgumentException("Unknown defense profile " + id);
        entity.setData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE, new EntityDefenseProfileRef(id));
    }
    public static void clearOverride(LivingEntity entity) {
        if (entity.level().isClientSide() || entity instanceof Player) throw new IllegalArgumentException("Server non-player living entity required");
        entity.removeData(ProgressionAttachments.ENTITY_DEFENSE_PROFILE);
    }
    public static void clear() { cache = new Cache(Map.of(), Map.of()); }
    private EntityDefenseService() {}
}
