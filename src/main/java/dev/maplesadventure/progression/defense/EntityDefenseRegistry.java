package dev.maplesadventure.progression.defense;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;

/** Two data layers, compiled to type lookups after tags are available. No production default enemy balance. */
public final class EntityDefenseRegistry {
    public static final int MAX_PROFILES = 4096, MAX_RULES = 4096;
    private static Map<ResourceLocation, EntityDefenseProfile> profiles = Map.of();
    private static List<Rule> rules = List.of();
    public record Rule(ResourceLocation file, ResourceLocation entity, ResourceLocation tag, int priority, ResourceLocation profile) {}
    public static final Comparator<Rule> RULE_ORDER = Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file);
    public static Map<ResourceLocation, EntityDefenseProfile> profiles() { return profiles; }
    public static List<Rule> rules() { return rules; }
    public static void clear() { profiles = Map.of(); rules = List.of(); }

    public static EntityDefenseProfile parseProfile(ResourceLocation id, JsonObject json) {
        fields(json, Set.of("channels","status_resistances","status_traits"));
        if (id.equals(EntityDefenseProfile.NONE.profileId())) throw new IllegalArgumentException("NONE is reserved identity");
        var channels = json.has("channels")?json.getAsJsonObject("channels"):new JsonObject();
        if (channels == null || channels.size() > WeaponDamageChannel.values().length) throw new IllegalArgumentException("Invalid channel count");
        var values = new EnumMap<WeaponDamageChannel, ChannelDefense>(WeaponDamageChannel.class);
        for (var entry : channels.entrySet()) {
            var channel = WeaponDamageChannel.parse(entry.getKey());
            var value = entry.getValue().getAsJsonObject(); fields(value, Set.of("defense", "absorption"));
            if (values.put(channel, new ChannelDefense(number(value, "defense", 0), number(value, "absorption", 0))) != null)
                throw new IllegalArgumentException("Duplicate channel");
        }
        var resistances=new EnumMap<dev.maplesadventure.progression.status.StatusEffectType,dev.maplesadventure.progression.status.StatusResistance>(dev.maplesadventure.progression.status.StatusEffectType.class);
        if(json.has("status_resistances")) for(var entry:json.getAsJsonObject("status_resistances").entrySet()) {
            var type=dev.maplesadventure.progression.status.StatusEffectType.parse(entry.getKey()); var v=entry.getValue().getAsJsonObject();
            fields(v,Set.of("threshold","immune","proc_damage_multiplier","correction","response"));
            if(v.has("immune")&&(!v.get("immune").isJsonPrimitive()||!v.getAsJsonPrimitive("immune").isBoolean())) throw new IllegalArgumentException("Expected immune boolean");
            if(resistances.put(type,new dev.maplesadventure.progression.status.StatusResistance(number(v,"threshold",160),v.has("immune")&&v.get("immune").getAsBoolean(),number(v,"proc_damage_multiplier",1),
                    v.has("correction")?id(v.get("correction")):dev.maplesadventure.progression.status.StatusResistanceCorrections.NONE,
                    v.has("response")?dev.maplesadventure.progression.status.SleepResponse.valueOf(v.get("response").getAsString().toUpperCase(Locale.ROOT)):dev.maplesadventure.progression.status.SleepResponse.STAGGER_ONLY))!=null)
                throw new IllegalArgumentException("Duplicate status resistance");
        }
        var traits=dev.maplesadventure.progression.status.StatusTargetTraits.DEFAULT;
        if(json.has("status_traits")) {
            var t=json.getAsJsonObject("status_traits"); fields(t,Set.of("tarnished_like","madness_immune","death_blight_immune","allow_death_blight"));
            for(var e:t.entrySet()) if(!e.getValue().isJsonPrimitive()||!e.getValue().getAsJsonPrimitive().isBoolean()) throw new IllegalArgumentException("Status trait boolean required");
            traits=new dev.maplesadventure.progression.status.StatusTargetTraits(t.has("tarnished_like")&&t.get("tarnished_like").getAsBoolean(),
                    t.has("madness_immune")&&t.get("madness_immune").getAsBoolean(),t.has("death_blight_immune")&&t.get("death_blight_immune").getAsBoolean(),
                    t.has("allow_death_blight")&&t.get("allow_death_blight").getAsBoolean());
        }
        return new EntityDefenseProfile(id, values, "DATAPACK",resistances,traits);
    }
    public static Rule parseRule(ResourceLocation id, JsonObject json) {
        fields(json, Set.of("entity", "tag", "priority", "profile"));
        if (json.has("entity") == json.has("tag")) throw new IllegalArgumentException("Specify exactly one entity or tag");
        double priority = number(json, "priority", 0);
        if (priority != Math.rint(priority) || Math.abs(priority) > 1000000) throw new IllegalArgumentException("Priority bounds");
        return new Rule(id, json.has("entity") ? id(json.get("entity")) : null, json.has("tag") ? id(json.get("tag")) : null,
                (int)priority, id(json.get("profile")));
    }
    private static ResourceLocation id(JsonElement element) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()
                || element.getAsString().isEmpty() || element.getAsString().length() > 256) throw new IllegalArgumentException("Profile/selector ID bounds");
        return ResourceLocation.parse(element.getAsString());
    }
    private static double number(JsonObject json, String key, double fallback) {
        if (!json.has(key)) return fallback;
        var element = json.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number: " + key);
        double value = element.getAsDouble();
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite " + key);
        return value;
    }
    private static void fields(JsonObject object, Set<String> allowed) {
        for (String key : object.keySet()) if (!allowed.contains(key)) throw new IllegalArgumentException("Unknown field " + key);
    }
    public static final class Profiles extends SimpleJsonResourceReloadListener {
        public Profiles() { super(new Gson(), "maplesadventure/entity_defense_profiles"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
            var next = new LinkedHashMap<ResourceLocation, EntityDefenseProfile>();
            input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                try {
                    if (next.size() >= MAX_PROFILES) throw new IllegalArgumentException("Too many defense profiles");
                    next.put(entry.getKey(), parseProfile(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (RuntimeException error) { MaplesAdventure.LOGGER.error("Rejected entity defense profile {}: {}", entry.getKey(), error.getMessage()); }
            });
            profiles = Map.copyOf(next);
            MaplesAdventure.LOGGER.info("Loaded {} entity defense profiles", profiles.size());
        }
    }
    public static final class Rules extends SimpleJsonResourceReloadListener {
        public Rules() { super(new Gson(), "maplesadventure/entity_defense_rules"); }
        @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
            var next = new ArrayList<Rule>();
            input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                try {
                    if (next.size() >= MAX_RULES) throw new IllegalArgumentException("Too many defense rules");
                    next.add(parseRule(entry.getKey(), entry.getValue().getAsJsonObject()));
                } catch (RuntimeException error) { MaplesAdventure.LOGGER.error("Rejected entity defense rule {}: {}", entry.getKey(), error.getMessage()); }
            });
            next.sort(RULE_ORDER); rules = List.copyOf(next);
        }
    }
    private EntityDefenseRegistry() {}
}
