package dev.maplesadventure.progression.spell;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

public final class SpellSchoolScalingRegistry extends SimpleJsonResourceReloadListener {
    public static final int MAX_SCHOOLS = 256;
    private static volatile Map<ResourceLocation, SpellSchoolScalingProfile> profiles = Map.of();
    public SpellSchoolScalingRegistry() { super(new Gson(), "spell_school_scaling"); }
    public static Map<ResourceLocation, SpellSchoolScalingProfile> profiles() { return profiles; }
    @Override protected void apply(Map<ResourceLocation, JsonElement> resources, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, SpellSchoolScalingProfile> next = new LinkedHashMap<>();
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            try {
                var value = SpellSchoolScalingProfile.parse(entry.getValue().getAsJsonObject());
                if (next.containsKey(value.schoolId())) throw new IllegalArgumentException("Duplicate school: " + value.schoolId());
                if (next.size() >= MAX_SCHOOLS) throw new IllegalArgumentException("Too many school profiles (max 256)");
                next.put(value.schoolId(), value);
            } catch (RuntimeException invalid) {
                MaplesAdventure.LOGGER.error("Rejected spell school scaling profile {}: {}", entry.getKey(), invalid.getMessage());
            }
        });
        profiles = Collections.unmodifiableMap(next);
        MaplesAdventure.LOGGER.info("Loaded {} spell school scaling profiles", profiles.size());
    }
}
