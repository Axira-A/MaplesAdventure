package dev.maplesadventure.progression.status;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

public final class StatusResistanceCorrections extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation NONE=ResourceLocation.fromNamespaceAndPath("maplesadventure","none");
    private static volatile Map<ResourceLocation,StatusResistanceCorrectionProfile> profiles=defaults();
    public StatusResistanceCorrections() { super(new Gson(),"maplesadventure/status_resistance_corrections"); }
    private static Map<ResourceLocation,StatusResistanceCorrectionProfile> defaults() {
        return Map.of(NONE,StatusResistanceCorrectionProfile.NONE,
                ResourceLocation.fromNamespaceAndPath("maplesadventure","standard"),StatusResistanceCorrectionProfile.STANDARD,
                ResourceLocation.fromNamespaceAndPath("maplesadventure","resistant"),StatusResistanceCorrectionProfile.RESISTANT,
                ResourceLocation.fromNamespaceAndPath("maplesadventure","boss"),StatusResistanceCorrectionProfile.RESISTANT);
    }
    public static StatusResistanceCorrectionProfile get(ResourceLocation id) { return profiles.getOrDefault(id,StatusResistanceCorrectionProfile.NONE); }
    public static void reset() { profiles=defaults(); }
    public static StatusResistanceCorrectionProfile parse(JsonObject json) {
        StatusDefinitions.fields(json,Set.of("stages"));
        var array=json.getAsJsonArray("stages");
        if(array.size()>5) throw new IllegalArgumentException("Correction stage count");
        var values=new ArrayList<Double>();
        for(var e:array) { if(!e.isJsonPrimitive()||!e.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Correction number"); values.add(e.getAsDouble()); }
        return new StatusResistanceCorrectionProfile(values);
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager resources,ProfilerFiller profiler) {
        var next=new HashMap<>(defaults());
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(4096).forEach(e->{try {
            if(e.getKey().equals(NONE)) throw new IllegalArgumentException("NONE is reserved");
            next.put(e.getKey(),parse(e.getValue().getAsJsonObject()));
        } catch(RuntimeException error) { dev.maplesadventure.MaplesAdventure.LOGGER.error("Rejected correction {}: {}",e.getKey(),error.getMessage()); }});
        profiles=Map.copyOf(next);
    }
}
