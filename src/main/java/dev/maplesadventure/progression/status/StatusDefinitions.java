package dev.maplesadventure.progression.status;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

public final class StatusDefinitions extends SimpleJsonResourceReloadListener {
    private static volatile Map<StatusEffectType, StatusEffectDefinition> definitions = defaults();
    public StatusDefinitions() { super(new Gson(), "maplesadventure/status_effects"); }
    private static Map<StatusEffectType, StatusEffectDefinition> defaults() {
        var map = new EnumMap<StatusEffectType, StatusEffectDefinition>(StatusEffectType.class);
        for (var type : StatusEffectType.values()) map.put(type, StatusEffectDefinition.defaults(type));
        return Map.copyOf(map);
    }
    public static StatusEffectDefinition get(StatusEffectType type) { return definitions.get(type); }
    public static void reset() { definitions = defaults(); }
    public static StatusEffectDefinition parse(StatusEffectType type, JsonObject json) {
        fields(json, Set.of("decay_delay", "decay_per_second", "active_duration", "tick_interval", "damage", "damage_taken_multiplier", "stamina_regen_multiplier","control_ticks","deep_sleep_ticks","mana_flat","mana_fraction"));
        var d = StatusEffectDefinition.defaults(type);
        var damage = json.has("damage") ? json.getAsJsonObject("damage") : new JsonObject();
        fields(damage, Set.of("max_health_fraction", "flat"));
        int duration = integer(json, "active_duration", d.duration());
        if (type.durationBar() != (duration > 0)) throw new IllegalArgumentException("Invalid duration for " + type);
        return new StatusEffectDefinition(integer(json,"decay_delay",d.decayDelay()), number(json,"decay_per_second",d.decayPerSecond()),
                duration, integer(json,"tick_interval",d.tickInterval()), number(damage,"max_health_fraction",d.maxHealthFraction()),
                number(damage,"flat",d.flatDamage()), number(json,"damage_taken_multiplier",d.damageTakenMultiplier()),
                number(json,"stamina_regen_multiplier",d.staminaRegenMultiplier()),integer(json,"control_ticks",d.controlTicks()),
                integer(json,"deep_sleep_ticks",d.deepSleepTicks()),number(json,"mana_flat",d.manaFlat()),number(json,"mana_fraction",d.manaFraction()));
    }
    public static void fields(JsonObject json, Set<String> allowed) {
        for (String key : json.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown status field " + key);
    }
    public static double number(JsonObject json, String key, double fallback) {
        if (!json.has(key)) return fallback;
        var e = json.get(key);
        if (!e.isJsonPrimitive() || !e.getAsJsonPrimitive().isNumber() || !Double.isFinite(e.getAsDouble()))
            throw new IllegalArgumentException("Expected finite number " + key);
        return e.getAsDouble();
    }
    public static int integer(JsonObject json, String key, int fallback) {
        double n = number(json,key,fallback);
        if (n != Math.rint(n) || n < 0 || n > 100000) throw new IllegalArgumentException("Integer bounds " + key);
        return (int)n;
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        var next = new EnumMap<StatusEffectType,StatusEffectDefinition>(StatusEffectType.class); next.putAll(defaults());
        input.forEach((id,value) -> {
            try {
                if (!id.getNamespace().equals("maplesadventure")) throw new IllegalArgumentException("Only canonical status IDs supported");
                var type=StatusEffectType.parse(id.getPath()); next.put(type,parse(type,value.getAsJsonObject()));
            } catch(RuntimeException e) { dev.maplesadventure.MaplesAdventure.LOGGER.error("Rejected status definition {}: {}",id,e.getMessage()); }
        });
        definitions=Map.copyOf(next);
    }
}
