package dev.maplesadventure.authoring.persistence;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.editor.EditorFoundation;
import java.util.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Global world authoring, independent of player/Phase state and chunk residency. */
public final class AuthoringSavedData extends SavedData {
    private static final Factory<AuthoringSavedData> FACTORY=new Factory<>(AuthoringSavedData::new,AuthoringSavedData::load);
    private final Map<ResourceLocation,MaplesScene> scenes=new LinkedHashMap<>();
    private final Map<ResourceLocation,CompoundTag> readOnlySources=new HashMap<>();
    private final List<CompoundTag> quarantined=new ArrayList<>();
    private CompoundTag futureRoot;
    public static AuthoringSavedData get(MinecraftServer server){return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"maplesadventure_authoring");}
    public Collection<MaplesScene> scenes(){return List.copyOf(scenes.values());}
    public MaplesScene scene(ResourceLocation id){return scenes.get(id);}
    public boolean readOnly(){return futureRoot!=null;}
    public void put(MaplesScene scene){
        if(readOnly()||readOnlySources.containsKey(scene.id()))throw new IllegalArgumentException("editor.maplesadventure.read_only");
        if(!scenes.containsKey(scene.id())&&scenes.size()+quarantined.size()>=EditorLimits.SCENES)throw new IllegalArgumentException("editor.maplesadventure.limit");
        scenes.put(scene.id(),scene);setDirty();
    }
    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        if(futureRoot!=null)return futureRoot.copy();
        tag.putInt("DataVersion",1);var entries=new ListTag();
        scenes.values().stream().sorted(Comparator.comparing(s->s.id().toString())).forEach(s->entries.add(readOnlySources.containsKey(s.id())?readOnlySources.get(s.id()).copy():SceneSerialization.save(s)));
        quarantined.forEach(t->entries.add(t.copy()));tag.put("Scenes",entries);return tag;
    }
    public static AuthoringSavedData load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new AuthoringSavedData();var list=tag.getList("Scenes",Tag.TAG_COMPOUND);
        if((tag.contains("DataVersion")&&tag.getInt("DataVersion")!=1)||list.size()>EditorLimits.SCENES){
            data.futureRoot=tag.copy();MaplesAdventure.LOGGER.error("Authoring data version/limits unsupported; preserved read-only");return data;
        }
        for(int i=0;i<list.size();i++){
            CompoundTag raw=list.getCompound(i);
            try{
                var scene=SceneSerialization.load(raw,EditorFoundation.COMPONENTS);
                if(data.scenes.putIfAbsent(scene.id(),scene)!=null)throw new IllegalArgumentException("Duplicate scene ID");
                if(scene.readOnly())data.readOnlySources.put(scene.id(),raw.copy());
                for(var issue:scene.issues())MaplesAdventure.LOGGER.warn("Authoring validation scene={} object={} component={} issue={}",scene.id(),issue.object(),issue.component(),issue.message());
            }catch(RuntimeException error){data.quarantined.add(raw.copy());MaplesAdventure.LOGGER.error("Scene load failed id={}; raw data retained",raw.getString("Id"),error);}
        }return data;
    }
}
