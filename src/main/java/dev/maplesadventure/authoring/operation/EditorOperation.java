package dev.maplesadventure.authoring.operation;

import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.authoring.EditorTransform;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Typed intentions, no arbitrary snapshot replacement or raw NBT mutation. */
public sealed interface EditorOperation {
    record RenameScene(String name) implements EditorOperation {}
    record CreateObject(String name,EditorTransform transform,UUID group,boolean marker) implements EditorOperation {}
    record DeleteObject(UUID id,long revision) implements EditorOperation {}
    record DuplicateObject(UUID id,long revision) implements EditorOperation {}
    record RenameObject(UUID id,long revision,String name) implements EditorOperation {}
    record SetTransform(UUID id,long revision,EditorTransform transform) implements EditorOperation {}
    record AddComponent(UUID id,long revision,ResourceLocation type) implements EditorOperation {}
    record RemoveComponent(UUID id,long revision,ResourceLocation type) implements EditorOperation {}
    record PatchComponent(UUID id,long revision,ResourceLocation type,Map<String,EditorValue> fields) implements EditorOperation {
        public PatchComponent { fields=Map.copyOf(fields); }
    }
    record CreateGroup(String name,UUID parent) implements EditorOperation {}
    record RenameGroup(UUID id,long revision,String name) implements EditorOperation {}
    record DeleteGroup(UUID id,long revision) implements EditorOperation {}
    record MoveObject(UUID id,long revision,UUID group) implements EditorOperation {}
    record MoveGroup(UUID id,long revision,UUID parent) implements EditorOperation {}
    record CreateTrigger(String name,EditorTransform transform,UUID group) implements EditorOperation {}
    record SetLogic(UUID id,long revision,dev.maplesadventure.authoring.logic.LogicComponent logic) implements EditorOperation {}
    /** One atomic inspector save; each component patch still uses registered typed accessors. */
    record CommitDraft(UUID id,long revision,String name,EditorTransform transform,
                       Map<ResourceLocation,Map<String,EditorValue>> fields,dev.maplesadventure.authoring.logic.LogicComponent logic) implements EditorOperation {
        public CommitDraft {var copy=new LinkedHashMap<ResourceLocation,Map<String,EditorValue>>();fields.forEach((k,v)->copy.put(k,Map.copyOf(v)));fields=Map.copyOf(copy);
            if(fields.size()>32||fields.values().stream().anyMatch(v->v.size()>64))throw new IllegalArgumentException("Draft limit");}
    }
}
