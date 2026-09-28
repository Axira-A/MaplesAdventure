package dev.maplesadventure.authoring.operation;

import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import java.util.*;

/** Pure transaction: copy, validate, return a replacement. Caller alone commits SavedData. */
public final class EditorOperationService {
    public static final class Rejected extends RuntimeException { public Rejected(String reason){super(reason);} }
    public record Result(MaplesScene scene,UUID selected) {}
    private final ComponentRegistry registry;
    public EditorOperationService(ComponentRegistry registry){this.registry=registry;}
    public Result apply(MaplesScene old,long expected,EditorOperation operation) {
        if(old.readOnly())throw new Rejected("editor.maplesadventure.read_only");
        if(old.revision()!=expected)throw new Rejected("editor.maplesadventure.stale");
        if(old.revision()==Long.MAX_VALUE)throw new Rejected("editor.maplesadventure.limit");
        var objects=new LinkedHashMap<>(old.objects());var groups=new LinkedHashMap<>(old.groups());String name=old.name();UUID selected=null;
        try {
            switch(operation) {
                case EditorOperation.RenameScene op -> name=EditorLimits.name(op.name());
                case EditorOperation.CreateObject op -> {
                    groupExists(groups,op.group());selected=UUID.randomUUID();
                    var components=op.marker()?Map.of(BuiltinComponents.MARKER,registry.create(BuiltinComponents.MARKER)):Map.<net.minecraft.resources.ResourceLocation,ComponentData>of();
                    objects.put(selected,new MaplesObject(selected,op.name(),op.transform(),op.group(),components,0));
                }
                case EditorOperation.DeleteObject op -> {object(old,op.id(),op.revision());objects.remove(op.id());}
                case EditorOperation.DuplicateObject op -> {var o=object(old,op.id(),op.revision());selected=UUID.randomUUID();objects.put(selected,new MaplesObject(selected,o.name(),o.transform(),o.group(),o.components(),0));}
                case EditorOperation.RenameObject op -> {var o=object(old,op.id(),op.revision());objects.put(o.id(),copy(o,op.name(),o.transform(),o.group(),o.components()));}
                case EditorOperation.SetTransform op -> {var o=object(old,op.id(),op.revision());objects.put(o.id(),copy(o,o.name(),op.transform(),o.group(),o.components()));}
                case EditorOperation.MoveObject op -> {var o=object(old,op.id(),op.revision());groupExists(groups,op.group());objects.put(o.id(),copy(o,o.name(),o.transform(),op.group(),o.components()));}
                case EditorOperation.AddComponent op -> {var o=object(old,op.id(),op.revision());var c=new LinkedHashMap<>(o.components());if(c.putIfAbsent(op.type(),registry.create(op.type()))!=null)throw new Rejected("editor.maplesadventure.duplicate_component");objects.put(o.id(),copy(o,o.name(),o.transform(),o.group(),c));}
                case EditorOperation.RemoveComponent op -> {var o=object(old,op.id(),op.revision());var c=new LinkedHashMap<>(o.components());if(c.remove(op.type())==null)throw new Rejected("editor.maplesadventure.missing_reference");objects.put(o.id(),copy(o,o.name(),o.transform(),o.group(),c));}
                case EditorOperation.PatchComponent op -> {var o=object(old,op.id(),op.revision());var c=new LinkedHashMap<>(o.components());if(!c.containsKey(op.type()))throw new Rejected("editor.maplesadventure.missing_reference");c.put(op.type(),registry.patch(c.get(op.type()),op.fields()));objects.put(o.id(),copy(o,o.name(),o.transform(),o.group(),c));}
                case EditorOperation.CreateGroup op -> {groupExists(groups,op.parent());selected=UUID.randomUUID();groups.put(selected,new EditorGroup(selected,op.name(),op.parent(),0));}
                case EditorOperation.RenameGroup op -> {var g=group(old,op.id(),op.revision());groups.put(g.id(),new EditorGroup(g.id(),op.name(),g.parent(),next(g.revision())));}
                case EditorOperation.MoveGroup op -> {var g=group(old,op.id(),op.revision());groupExists(groups,op.parent());groups.put(g.id(),new EditorGroup(g.id(),g.name(),op.parent(),next(g.revision())));}
                case EditorOperation.DeleteGroup op -> {var g=group(old,op.id(),op.revision());groups.remove(g.id());
                    for(var o:old.objects().values())if(g.id().equals(o.group()))objects.put(o.id(),copy(o,o.name(),o.transform(),g.parent(),o.components()));
                    for(var child:old.groups().values())if(g.id().equals(child.parent()))groups.put(child.id(),new EditorGroup(child.id(),child.name(),g.parent(),next(child.revision())));
                }
            }
            var result=new MaplesScene(old.id(),old.dimension(),name,MaplesScene.VERSION,next(old.revision()),objects,groups,List.of(),false);
            var tag=SceneSerialization.save(result);
            if(SceneSerialization.bytes(tag).length>EditorLimits.SCENE_BYTES)throw new Rejected("editor.maplesadventure.limit");
            for(var object:objects.values())if(SceneSerialization.bytes(SceneSerialization.object(object)).length>EditorLimits.OBJECT_BYTES)throw new Rejected("editor.maplesadventure.limit");
            // Also checks group cycles, dangling parents and depth, without touching a world/chunk.
            result=SceneSerialization.load(tag,registry);
            if(result.readOnly())throw new Rejected("editor.maplesadventure.invalid_group");
            return new Result(result,selected);
        }catch(Rejected e){throw e;}catch(RuntimeException | LinkageError e){throw new Rejected(e instanceof IllegalArgumentException&&e.getMessage()!=null&&e.getMessage().startsWith("editor.")?e.getMessage():"editor.maplesadventure.invalid_operation");}
    }
    private static long next(long revision){if(revision==Long.MAX_VALUE)throw new Rejected("editor.maplesadventure.limit");return revision+1;}
    private static MaplesObject object(MaplesScene scene,UUID id,long revision){var o=scene.objects().get(id);if(o==null)throw new Rejected("editor.maplesadventure.missing_reference");if(o.revision()!=revision)throw new Rejected("editor.maplesadventure.stale");return o;}
    private static EditorGroup group(MaplesScene scene,UUID id,long revision){var g=scene.groups().get(id);if(g==null)throw new Rejected("editor.maplesadventure.missing_reference");if(g.revision()!=revision)throw new Rejected("editor.maplesadventure.stale");return g;}
    private static void groupExists(Map<UUID,EditorGroup> groups,UUID id){if(id!=null&&!groups.containsKey(id))throw new Rejected("editor.maplesadventure.missing_reference");}
    private static MaplesObject copy(MaplesObject o,String name,EditorTransform transform,UUID group,Map<net.minecraft.resources.ResourceLocation,ComponentData> components){return new MaplesObject(o.id(),name,transform,group,components,next(o.revision()));}
}
