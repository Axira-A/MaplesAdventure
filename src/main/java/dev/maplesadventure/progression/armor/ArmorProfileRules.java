package dev.maplesadventure.progression.armor;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import dev.maplesadventure.progression.status.StatusResistanceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

/** Public armor_profiles datapack grammar; each bad file is rejected independently. */
public final class ArmorProfileRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file, ResourceLocation item, ResourceLocation tag, int priority,
                       boolean disabled, ArmorProfile profile) {}
    private static volatile List<Rule> rules = List.of();
    public ArmorProfileRules() { super(new Gson(), "maplesadventure/armor_profiles"); }
    public static List<Rule> rules() { return rules; }

    public static Rule parse(ResourceLocation file, JsonObject json) {
        for (String key : json.keySet()) if (!Set.of("item", "tag", "priority", "disabled", "channels", "resistances").contains(key))
            throw new IllegalArgumentException("Unknown armor field " + key);
        if (json.has("item") == json.has("tag")) throw new IllegalArgumentException("Specify exactly one item or tag");
        ResourceLocation item = json.has("item") ? selector(json.get("item")) : null;
        ResourceLocation tag = json.has("tag") ? selector(json.get("tag")) : null;
        int priority = json.has("priority") ? integer(json.get("priority")) : 0;
        boolean disabled = false;
        if (json.has("disabled")) {
            if (!json.get("disabled").isJsonPrimitive() || !json.getAsJsonPrimitive("disabled").isBoolean())
                throw new IllegalArgumentException("disabled must be boolean");
            disabled = json.get("disabled").getAsBoolean();
        }
        var channels = new EnumMap<WeaponDamageChannel, Double>(WeaponDamageChannel.class);
        if (json.has("channels")) json.getAsJsonObject("channels").entrySet().forEach(entry -> {
            var channel = WeaponDamageChannel.parse(entry.getKey());
            channels.put(channel, number(entry.getValue()));
        });
        var resistances = new EnumMap<StatusResistanceType, Double>(StatusResistanceType.class);
        if (json.has("resistances")) json.getAsJsonObject("resistances").entrySet().forEach(entry -> {
            var type = StatusResistanceType.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            resistances.put(type, number(entry.getValue()));
        });
        if (!disabled && channels.isEmpty() && resistances.isEmpty())
            throw new IllegalArgumentException("Armor profile requires channels or resistances");
        return new Rule(file, item, tag, priority, disabled, new ArmorProfile(channels, resistances));
    }
    private static ResourceLocation selector(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString() || value.getAsString().length() > 256)
            throw new IllegalArgumentException("Invalid armor selector");
        return ResourceLocation.parse(value.getAsString());
    }
    private static int integer(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("priority must be integer");
        int result = value.getAsBigDecimal().intValueExact();
        if (result < -10000 || result > 10000) throw new IllegalArgumentException("priority outside -10000..10000");
        return result;
    }
    private static double number(JsonElement value) {
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Armor contribution must be number");
        double result = value.getAsDouble();
        if (!Double.isFinite(result) || result < 0 || result > 1000) throw new IllegalArgumentException("Armor contribution outside 0..1000");
        return result;
    }
    @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        var next = new ArrayList<Rule>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                if (next.size() >= 4096) throw new IllegalArgumentException("Max 4096 armor rules");
                next.add(parse(entry.getKey(), entry.getValue().getAsJsonObject()));
            } catch (RuntimeException error) {
                MaplesAdventure.LOGGER.error("Rejected armor profile {}: {}", entry.getKey(), error.getMessage());
            }
        });
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file));
        rules = List.copyOf(next);
    }
}
