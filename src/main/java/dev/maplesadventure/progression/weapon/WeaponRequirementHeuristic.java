package dev.maplesadventure.progression.weapon;

import dev.maplesadventure.progression.spell.SpellSchoolScalingProfile;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;

public final class WeaponRequirementHeuristic {
    public static WeaponRequirementProfile generate(WeaponFacts f, Map<ResourceLocation,SpellSchoolScalingProfile> schools, int cap) {
        if (f.archetype()==WeaponRequirementArchetype.NONE) return WeaponRequirementProfile.NONE;
        cap=Math.clamp(cap,5,40);
        // Bounded, non-linear load adjustment, relative to 5 equipment units, not entity body weight.
        int weight=f.weight()<=0?0:(int)Math.round(Math.clamp(3*(Math.sqrt(f.weight())-Math.sqrt(5)), -2, 8));
        // A bow's default melee 1 damage / 4 speed does not describe its projectile handling.
        boolean ranged=f.archetype()==WeaponRequirementArchetype.BOW || f.archetype()==WeaponRequirementArchetype.CROSSBOW;
        int damage=ranged?0:(int)Math.round(Math.clamp((f.damage()-6)*.7, -3, 7));
        int slow=ranged?0:(int)Math.round(Math.clamp((1.6-f.speed())*2,0,3));
        int fast=ranged?0:(int)Math.round(Math.clamp((f.speed()-1.6)*1.5,0,3));
        int str=f.archetype().strength+weight+damage+slow;
        int dex=f.archetype().dexterity+fast+(int)Math.round(Math.max(0,damage)*.3);
        // Low-power starter weapons/tools remain usable by a fresh 5/5 character without exact overrides.
        if ((f.archetype()==WeaponRequirementArchetype.SWORD || f.archetype()==WeaponRequirementArchetype.TOOL)
                && f.damage()<=5 && f.weight()<=5) { str=5; dex=5; }
        if(f.archetype()==WeaponRequirementArchetype.TOOL) { str=Math.min(str,10); dex=5; }
        double i=0,faith=0,a=0;
        for(var entry:f.affinity().entrySet()) {
            var p=schools.get(entry.getKey()); if(p==null || entry.getValue()<=0) continue;
            double requirement=10+Math.min(10,entry.getValue()*20);
            i=Math.max(i,p.intelligenceWeight()*requirement);
            faith=Math.max(faith,p.faithWeight()*requirement);
            a=Math.max(a,p.arcaneWeight()*requirement);
        }
        return new WeaponRequirementProfile(Math.clamp(str,5,cap),Math.clamp(dex,5,cap),magic(i,cap),magic(faith,cap),magic(a,cap),
                f.source(),f.category(),"category="+f.archetype().strength+"/"+f.archetype().dexterity
                +" weight="+weight+" damage="+damage+" slow="+slow+" fast="+fast+" affinity="+f.affinity().size());
    }
    private static int magic(double value,int cap) { return value<=0?0:Math.clamp((int)Math.ceil(value),5,cap); }
    private WeaponRequirementHeuristic() {}
}
