package dev.maplesadventure.authoring.logic;

import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Bounded booleans only. Future/corrupt data is retained read-only rather than overwritten. */
public final class GameFlagState implements INBTSerializable<CompoundTag> {
    private final Map<ResourceLocation,Boolean> values=new TreeMap<>();
    private CompoundTag preserved;
    public boolean get(ResourceLocation id){return values.getOrDefault(id,false);}
    public void set(ResourceLocation id,boolean value){
        if(preserved!=null)throw new IllegalStateException("Unsupported game flag data is read-only");
        if(id.toString().length()>256||(!values.containsKey(id)&&values.size()>=LogicLimits.FLAGS))throw new IllegalArgumentException("Game flag limit");
        values.put(id,value);
    }
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries){
        if(preserved!=null)return preserved.copy();var tag=new CompoundTag();tag.putInt("Version",1);var entries=new CompoundTag();
        values.forEach((k,v)->entries.putBoolean(k.toString(),v));tag.put("Flags",entries);return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries,CompoundTag tag){
        values.clear();preserved=null;
        try{if(tag.getInt("Version")!=1||!tag.contains("Flags",Tag.TAG_COMPOUND)||tag.getCompound("Flags").size()>LogicLimits.FLAGS)throw new IllegalArgumentException("Version/size");
            var flags=tag.getCompound("Flags");for(var key:flags.getAllKeys()){
                if(!flags.contains(key,Tag.TAG_BYTE))throw new IllegalArgumentException("Not a boolean flag");set(ResourceLocation.parse(key),flags.getBoolean(key));
            }
        }catch(RuntimeException bad){values.clear();preserved=tag.copy();dev.maplesadventure.MaplesAdventure.LOGGER.warn("Invalid/unsupported game flags retained read-only: {}",bad.getMessage());}
    }
}
