package dev.maplesadventure.progression.status;

import com.google.gson.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;

public final class StatusMotionRules extends SimpleJsonResourceReloadListener {
    private static final Set<String> SELECTORS=Set.of("attack","animation","skill","archetype","projectile");
    private static volatile Map<String,Map<String,Double>> compiled=empty();
    public record Rule(String kind,String key,double value,int priority) {}
    public StatusMotionRules() { super(new Gson(),"maplesadventure/status_motion_values"); }
    private static Map<String,Map<String,Double>> empty() {
        var out=new HashMap<String,Map<String,Double>>(); SELECTORS.forEach(s->out.put(s,Map.of())); return Map.copyOf(out);
    }
    public static Map<String,Map<String,Double>> tables() { return compiled; }
    public static void reset() { compiled=empty(); }
    public static Rule parse(JsonObject json) {
        var allowed=new HashSet<>(SELECTORS); allowed.addAll(Set.of("motion","priority")); StatusDefinitions.fields(json,allowed);
        var keys=SELECTORS.stream().filter(json::has).toList(); if(keys.size()!=1) throw new IllegalArgumentException("One motion selector required");
        String kind=keys.getFirst();
        if(!json.get(kind).isJsonPrimitive()||!json.getAsJsonPrimitive(kind).isString()) throw new IllegalArgumentException("Motion selector must be a string");
        String key=json.get(kind).getAsString(); if(key.isEmpty()||key.length()>256) throw new IllegalArgumentException("Motion selector bounds");
        double value=StatusResistance.bounded(StatusDefinitions.number(json,"motion",1),0,4);
        double priority=StatusDefinitions.number(json,"priority",0); if(priority!=Math.rint(priority)||Math.abs(priority)>1000000) throw new IllegalArgumentException("Priority bounds");
        return new Rule(kind,key,value,(int)priority);
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        var rules=new ArrayList<Map.Entry<ResourceLocation,Rule>>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).limit(4096).forEach(e->{try {rules.add(Map.entry(e.getKey(),parse(e.getValue().getAsJsonObject())));}
            catch(RuntimeException error){dev.maplesadventure.MaplesAdventure.LOGGER.error("Rejected motion rule {}: {}",e.getKey(),error.getMessage());}});
        rules.sort(Comparator.<Map.Entry<ResourceLocation,Rule>>comparingInt(e->e.getValue().priority()).reversed().thenComparing(Map.Entry::getKey));
        var next=new HashMap<String,Map<String,Double>>(); SELECTORS.forEach(k->next.put(k,new HashMap<>()));
        rules.forEach(e->{var r=e.getValue();next.get(r.kind()).putIfAbsent(r.key(),r.value());});
        next.replaceAll((k,v)->Map.copyOf(v)); compiled=Map.copyOf(next);
    }
}
