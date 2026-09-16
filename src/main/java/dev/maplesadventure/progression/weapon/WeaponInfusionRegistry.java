package dev.maplesadventure.progression.weapon;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.Attribute;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

public final class WeaponInfusionRegistry extends SimpleJsonResourceReloadListener {
    public static final ResourceLocation NORMAL_ID=id("normal"), HEAVY_ID=id("heavy"), KEEN_ID=id("keen"), QUALITY_ID=id("quality"),
            MAGIC_ID=id("magic"), SACRED_ID=id("sacred"), BLOOD_ID=id("blood"), POISON_ID=id("poison");
    private static volatile Map<ResourceLocation,WeaponInfusionDefinition> definitions=builtins();

    public WeaponInfusionRegistry() { super(new Gson(), "maplesadventure/weapon_infusions"); }
    public static Map<ResourceLocation,WeaponInfusionDefinition> definitions() { return definitions; }
    public static Optional<WeaponInfusionDefinition> find(ResourceLocation id) { return Optional.ofNullable(definitions.get(id)); }
    public static void reset() { definitions=builtins(); }

    @Override protected void apply(Map<ResourceLocation, JsonElement> input, ResourceManager manager, ProfilerFiller profiler) {
        var next=new LinkedHashMap<>(builtins());
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try {
                if(!next.containsKey(entry.getKey()) && next.size()>=WeaponInfusionDefinition.MAX_DEFINITIONS)
                    throw new IllegalArgumentException("Max 16 infusion definitions");
                next.put(entry.getKey(),parse(entry.getKey(),entry.getValue().getAsJsonObject(),next.get(entry.getKey())));
            } catch(RuntimeException error) { MaplesAdventure.LOGGER.error("Rejected weapon infusion {}: {}",entry.getKey(),error.getMessage()); }
        });
        definitions=Map.copyOf(next);
        MaplesAdventure.LOGGER.info("Loaded {} weapon infusion definitions",definitions.size());
    }

    public static WeaponInfusionDefinition parse(ResourceLocation id,JsonObject json,WeaponInfusionDefinition fallback) {
        for(String key:json.keySet()) if(!Set.of("display","icon","base_multiplier","physical_scaling","element","future_buildup","statuses").contains(key))
            throw new IllegalArgumentException("Unknown infusion field "+key);
        String display=json.has("display")?boundedString(json.get("display"),128):fallback!=null?fallback.translationKey():"infusion."+id.getNamespace()+'.'+id.getPath();
        ResourceLocation icon=json.has("icon")?ResourceLocation.parse(boundedString(json.get("icon"),256)):fallback==null?null:fallback.icon();
        double base=json.has("base_multiplier")?number(json.get("base_multiplier"),0.000001,2):fallback==null?1:fallback.baseMultiplier();
        var transforms=new EnumMap<Attribute,WeaponInfusionDefinition.AttributeTransform>(Attribute.class);
        if(fallback!=null) transforms.putAll(fallback.physicalScaling());
        if(json.has("physical_scaling")) {
            var values=json.getAsJsonObject("physical_scaling");
            for(String key:values.keySet()) {
                var attribute=Attribute.parse(key).orElseThrow(()->new IllegalArgumentException("Unknown scaling attribute "+key));
                if(!WeaponRequirementProfile.ATTRIBUTES.contains(attribute)) throw new IllegalArgumentException("Invalid weapon scaling attribute "+key);
                var value=values.getAsJsonObject(key);
                for(String part:value.keySet()) if(!Set.of("multiply","minimum","maximum").contains(part)) throw new IllegalArgumentException("Unknown transform field "+part);
                var old=transforms.getOrDefault(attribute,WeaponInfusionDefinition.AttributeTransform.identity());
                transforms.put(attribute,new WeaponInfusionDefinition.AttributeTransform(
                        value.has("multiply")?number(value.get("multiply"),0,2):old.multiplier(),
                        value.has("minimum")?number(value.get("minimum"),0,1.5):old.minimum(),
                        value.has("maximum")?number(value.get("maximum"),0,1.5):old.maximum()));
            }
        }
        var split=fallback==null?null:fallback.elementSplit();
        if(json.has("element")) {
            if(json.get("element").isJsonNull()) split=null;
            else {
                var value=json.getAsJsonObject("element");
                for(String key:value.keySet()) if(!Set.of("channel","physical_ratio","element_ratio","scaling","max_bonus").contains(key))
                    throw new IllegalArgumentException("Unknown element field "+key);
                if(!value.has("channel")||!value.has("physical_ratio")||!value.has("element_ratio")||!value.has("scaling"))
                    throw new IllegalArgumentException("Element split requires channel, ratios and scaling");
                var scalingJson=new JsonObject(); scalingJson.addProperty("item","minecraft:air"); scalingJson.add("scaling",value.get("scaling"));
                if(value.has("max_bonus")) scalingJson.add("max_bonus",value.get("max_bonus"));
                split=new WeaponInfusionDefinition.ElementSplit(WeaponDamageChannel.parse(value.get("channel").getAsString()),
                        number(value.get("physical_ratio"),0,2),number(value.get("element_ratio"),0.000001,2),
                        WeaponScalingRules.parse(id,scalingJson).profile());
            }
        }
        var buildup=json.has("future_buildup")?WeaponInfusionBuildup.parse(json.get("future_buildup").getAsString()):fallback==null?WeaponInfusionBuildup.NONE:fallback.futureBuildup();
        var statuses=json.has("statuses")?dev.maplesadventure.progression.status.WeaponStatusProfile.parse(json.getAsJsonObject("statuses")):
                json.has("future_buildup")||fallback==null?dev.maplesadventure.progression.status.WeaponStatusProfile.legacy(buildup):fallback.statuses();
        return new WeaponInfusionDefinition(id,display,icon,base,transforms,split,buildup,statuses);
    }

    private static Map<ResourceLocation,WeaponInfusionDefinition> builtins() {
        var result=new LinkedHashMap<ResourceLocation,WeaponInfusionDefinition>();
        result.put(NORMAL_ID,definition(NORMAL_ID,1,rules(1,0,1.5,1,0,1.5,1,0,1.5,1,0,1.5,1,0,1.5),null,WeaponInfusionBuildup.NONE));
        result.put(HEAVY_ID,definition(HEAVY_ID,.95,rules(1,.8,1.5,.25,0,1.5,0,0,0,0,0,0,0,0,0),null,WeaponInfusionBuildup.NONE));
        result.put(KEEN_ID,definition(KEEN_ID,.95,rules(.25,0,1.5,1,.8,1.5,0,0,0,0,0,0,0,0,0),null,WeaponInfusionBuildup.NONE));
        result.put(QUALITY_ID,definition(QUALITY_ID,.94,rules(1,.55,.65,1,.55,.65,0,0,0,0,0,0,0,0,0),null,WeaponInfusionBuildup.NONE));
        var magicPhysical=rules(.4,0,1.5,.4,0,1.5,0,0,0,0,0,0,0,0,0);
        result.put(MAGIC_ID,definition(MAGIC_ID,1,magicPhysical,split(WeaponDamageChannel.MAGIC,.8,0,0),WeaponInfusionBuildup.NONE));
        result.put(SACRED_ID,definition(SACRED_ID,1,magicPhysical,split(WeaponDamageChannel.HOLY,0,.8,0),WeaponInfusionBuildup.NONE));
        result.put(BLOOD_ID,definition(BLOOD_ID,.90,rules(.6,0,1.5,.6,0,1.5,0,0,0,0,0,0,0,.55,.55),null,WeaponInfusionBuildup.BLEED));
        result.put(POISON_ID,definition(POISON_ID,.90,rules(.6,0,1.5,.6,0,1.5,0,0,0,0,0,0,0,.45,.45),null,WeaponInfusionBuildup.POISON));
        return Map.copyOf(result);
    }
    private static WeaponInfusionDefinition definition(ResourceLocation id,double base,Map<Attribute,WeaponInfusionDefinition.AttributeTransform> rules,
            WeaponInfusionDefinition.ElementSplit split,WeaponInfusionBuildup buildup) {
        return new WeaponInfusionDefinition(id,"infusion.maplesadventure."+id.getPath(),
                ResourceLocation.fromNamespaceAndPath("maplesadventure","textures/gui/infusion/"+id.getPath()+".png"),base,rules,split,buildup);
    }
    private static WeaponInfusionDefinition.ElementSplit split(WeaponDamageChannel channel,double intelligence,double faith,double arcane) {
        return new WeaponInfusionDefinition.ElementSplit(channel,.65,.35,new WeaponScalingProfile(0,0,intelligence,faith,arcane,
                WeaponScalingProfile.DEFAULT_MAX_BONUS,"INFUSION","ELEMENT",channel.id()));
    }
    private static Map<Attribute,WeaponInfusionDefinition.AttributeTransform> rules(double... values) {
        var result=new EnumMap<Attribute,WeaponInfusionDefinition.AttributeTransform>(Attribute.class);
        for(int i=0;i<WeaponRequirementProfile.ATTRIBUTES.size();i++) result.put(WeaponRequirementProfile.ATTRIBUTES.get(i),
                new WeaponInfusionDefinition.AttributeTransform(values[i*3],values[i*3+1],values[i*3+2]));
        return result;
    }
    private static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath("maplesadventure",path); }
    private static double number(JsonElement value,double min,double max) {
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Expected number");
        double number=value.getAsDouble(); if(!Double.isFinite(number)||number<min||number>max) throw new IllegalArgumentException("Number bounds"); return number;
    }
    private static String boundedString(JsonElement value,int max) {
        if(!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Expected string");
        String text=value.getAsString(); if(text.isEmpty()||text.length()>max) throw new IllegalArgumentException("String bounds"); return text;
    }
}
