package dev.maplesadventure.editor;

import dev.maplesadventure.authoring.EditorTransform;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.INBTSerializable;

/** Player-owned recovery marker only; never a copy of inventory, XP, Phase or RPG progression. */
public final class EditorRecoveryState implements INBTSerializable<CompoundTag> {
    public record ReturnPoint(GameType gameMode,ResourceLocation dimension,EditorTransform transform){}
    private ReturnPoint point;
    private boolean malformed;
    public boolean active(){return point!=null||malformed;}
    public boolean malformed(){return malformed;}
    public Optional<ReturnPoint> point(){return Optional.ofNullable(point);}
    public void begin(ReturnPoint point){if(active())throw new IllegalStateException("Unresolved editor recovery");this.point=java.util.Objects.requireNonNull(point);}
    public void clear(){point=null;malformed=false;}
    @Override public CompoundTag serializeNBT(HolderLookup.Provider registries){
        var tag=new CompoundTag();tag.putInt("Version",1);tag.putBoolean("Active",active());
        if(point!=null){tag.putString("Mode",point.gameMode().getName());tag.putString("Dimension",point.dimension().toString());
            tag.put("Transform",dev.maplesadventure.authoring.persistence.SceneSerialization.transform(point.transform()));}
        return tag;
    }
    @Override public void deserializeNBT(HolderLookup.Provider registries,CompoundTag tag){
        clear();if(!tag.getBoolean("Active"))return;
        try{
            if(tag.getInt("Version")!=1||!tag.contains("Transform",10))throw new IllegalArgumentException("Recovery version/transform");
            GameType mode=java.util.Arrays.stream(GameType.values()).filter(t->t.getName().equals(tag.getString("Mode"))).findFirst().orElseThrow();
            point=new ReturnPoint(mode,ResourceLocation.parse(tag.getString("Dimension")),dev.maplesadventure.authoring.persistence.SceneSerialization.transform(tag.getCompound("Transform")));
        }catch(RuntimeException invalid){malformed=true;}
    }
}
