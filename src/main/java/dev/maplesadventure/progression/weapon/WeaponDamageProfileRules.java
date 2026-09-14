package dev.maplesadventure.progression.weapon;

import com.google.gson.*;
import dev.maplesadventure.MaplesAdventure;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.*;

/** Same per-file rejection, exact/tag and stable priority contract as Round 7. */
public final class WeaponDamageProfileRules extends SimpleJsonResourceReloadListener {
    public record Rule(ResourceLocation file,ResourceLocation item,ResourceLocation tag,int priority,boolean disabled,WeaponDamageProfile profile) {}
    public static List<Rule> rules=List.of();
    public WeaponDamageProfileRules() { super(new Gson(),"maplesadventure/weapon_damage_profiles"); }
    public static Rule parse(ResourceLocation id,JsonObject json) {
        for(var key:json.keySet()) if(!Set.of("item","tag","priority","disabled","components").contains(key)) throw new IllegalArgumentException("Unknown field "+key);
        // Delegate common selector, priority, boolean and scaling validation to the existing parser.
        JsonObject selector=json.deepCopy(); selector.remove("components"); selector.add("scaling",new JsonObject());
        var common=WeaponScalingRules.parse(id,selector);
        boolean disabled=common.profile().source().equals("DISABLED");
        if(disabled) return new Rule(id,common.item(),common.tag(),common.priority(),true,WeaponDamageProfile.STANDARD);
        JsonArray array=json.getAsJsonArray("components");
        if(array==null||array.isEmpty()||array.size()>8) throw new IllegalArgumentException("components must be 1..8");
        List<WeaponDamageComponent> components=new ArrayList<>();
        for(var element:array) {
            var c=element.getAsJsonObject();
            for(var key:c.keySet()) if(!Set.of("channel","base_ratio","scaling","max_bonus").contains(key)) throw new IllegalArgumentException("Unknown component field "+key);
            var channel=WeaponDamageChannel.parse(c.get("channel").getAsString());
            var ratio=c.get("base_ratio");
            if(ratio==null||!ratio.isJsonPrimitive()||!ratio.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("base_ratio must be number");
            WeaponScalingProfile override=null;
            if(c.has("scaling")) {
                JsonObject profile=new JsonObject(); profile.addProperty("item","minecraft:air"); profile.add("scaling",c.get("scaling"));
                if(c.has("max_bonus")) profile.add("max_bonus",c.get("max_bonus"));
                override=WeaponScalingRules.parse(id,profile).profile();
            } else if(c.has("max_bonus")) throw new IllegalArgumentException("max_bonus requires explicit scaling");
            components.add(new WeaponDamageComponent(channel,ratio.getAsDouble(),override==null?WeaponDamageComponent.ScalingMode.INHERIT_WEAPON:WeaponDamageComponent.ScalingMode.OVERRIDE,override));
        }
        return new Rule(id,common.item(),common.tag(),common.priority(),false,
                new WeaponDamageProfile(components,common.item()!=null?"EXACT_JSON":"TAG_JSON","OVERRIDE",id.toString()));
    }
    @Override protected void apply(Map<ResourceLocation,JsonElement> input,ResourceManager manager,ProfilerFiller profiler) {
        List<Rule> next=new ArrayList<>();
        input.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
            try { if(next.size()>=4096) throw new IllegalArgumentException("Max 4096 rules"); next.add(parse(entry.getKey(),entry.getValue().getAsJsonObject())); }
            catch(RuntimeException e) { MaplesAdventure.LOGGER.error("Rejected weapon damage profile {}: {}",entry.getKey(),e.getMessage()); }
        });
        next.sort(Comparator.comparingInt(Rule::priority).reversed().thenComparing(Rule::file)); rules=List.copyOf(next);
    }
}
