package dev.maplesadventure.editor.network;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import dev.maplesadventure.editor.EditorFoundation;
import java.util.*;
import net.minecraft.nbt.*;

/** Authoritative presentation metadata is generated on the server, never accepted as a mutation. */
public final class EditorViews {
    public static CompoundTag schema(){
        var root=new CompoundTag();var types=new ListTag();
        for(var descriptor:EditorFoundation.COMPONENTS.descriptors()){
            var type=new CompoundTag();type.putString("Id",descriptor.id().toString());type.putString("Label",descriptor.translationKey());
            var fields=new ListTag();for(var field:descriptor.fields())fields.add(field(field.schema()));type.put("Fields",fields);types.add(type);
        }root.put("Types",types);
        for(var kind:dev.maplesadventure.authoring.logic.LogicTypeRegistry.Kind.values()){
            var list=new ListTag();var registry=dev.maplesadventure.authoring.logic.BuiltinLogic.registry(kind);
            for(var e:registry.entries()){var t=new CompoundTag();var d=e.schema();t.putString("Id",d.id().toString());t.putString("Label",d.translationKey());
                var fields=new ListTag();d.fields().forEach(f->fields.add(field(f.schema())));t.put("Fields",fields);
                t.put("Defaults",dev.maplesadventure.authoring.logic.LogicComponent.definition(registry.defaults(d.id())));list.add(t);}
            root.put("Logic"+kind.name(),list);
        }return root;
    }
    public static CompoundTag field(InspectorField f){
        var t=new CompoundTag();t.putString("Id",f.id());t.putString("Label",f.label());t.putString("Kind",f.kind().name());
        t.putDouble("Min",f.min());t.putDouble("Max",f.max());t.putDouble("Step",f.step());t.putInt("Length",f.maxLength());
        t.putBoolean("Nullable",f.nullable());t.putBoolean("ReadOnly",f.readOnly());
        var choices=new ListTag();f.choices().forEach(s->choices.add(StringTag.valueOf(s)));t.put("Choices",choices);
        if(f.registry()!=null)t.putString("Registry",f.registry().toString());return t;
    }
    public static InspectorField field(CompoundTag t){
        var choices=new ArrayList<String>();t.getList("Choices",Tag.TAG_STRING).forEach(v->choices.add(v.getAsString()));
        return new InspectorField(t.getString("Id"),t.getString("Label"),EditorValue.Kind.valueOf(t.getString("Kind")),t.getDouble("Min"),t.getDouble("Max"),t.getDouble("Step"),t.getInt("Length"),t.getBoolean("Nullable"),t.getBoolean("ReadOnly"),choices,
                t.contains("Registry")?net.minecraft.resources.ResourceLocation.parse(t.getString("Registry")):null);
    }
    public static CompoundTag value(EditorValue v){var t=new CompoundTag();t.putString("Kind",v.kind().name());t.putString("Value",v.text());return t;}
    public static EditorValue value(CompoundTag t){return new EditorValue(EditorValue.Kind.valueOf(t.getString("Kind")),t.getString("Value"));}
    public static CompoundTag object(MaplesObject object){
        var t=SceneSerialization.object(object);var views=new CompoundTag();
        object.components().forEach((id,c)->{
            try{if(EditorFoundation.COMPONENTS.validate(c).stream().anyMatch(i->i.severity()==ValidationIssue.Severity.ERROR))return;
                var values=new CompoundTag();EditorFoundation.COMPONENTS.fields(c).forEach((k,v)->values.put(k,value(v)));views.put(id.toString(),values);}
            catch(RuntimeException|LinkageError ignored){/* Unknown/corrupt data remains present, but not editable. */}
        });t.put("EditorFields",views);return t;
    }
    public static ListTag issues(MaplesScene scene){
        var issues=new ListTag();for(var issue:scene.issues()){
            var t=new CompoundTag();t.putString("Severity",issue.severity().name());t.putString("Message",issue.message());t.putString("Field",issue.field());
            if(issue.object()!=null)t.putUUID("Object",issue.object());if(issue.component()!=null)t.putString("Component",issue.component().toString());issues.add(t);
        }return issues;
    }
    public static CompoundTag scene(MaplesScene scene){
        var t=SceneSerialization.save(scene);var objects=new ListTag();scene.objects().values().stream().sorted(Comparator.comparing(MaplesObject::id)).forEach(o->objects.add(object(o)));
        t.put("Objects",objects);t.putBoolean("ReadOnly",scene.readOnly());t.put("Issues",issues(scene));return t;
    }
    public static CompoundTag delta(MaplesScene old,MaplesScene next){
        var t=new CompoundTag();t.putString("Id",next.id().toString());t.putString("Name",next.name());t.putLong("Before",old.revision());t.putLong("Revision",next.revision());
        var objects=new ListTag();var groups=new ListTag();var removedObjects=new ListTag();var removedGroups=new ListTag();
        next.objects().forEach((id,o)->{if(!o.equals(old.objects().get(id)))objects.add(object(o));});
        next.groups().forEach((id,g)->{if(!g.equals(old.groups().get(id)))groups.add(SceneSerialization.group(g));});
        old.objects().keySet().forEach(id->{if(!next.objects().containsKey(id))removedObjects.add(StringTag.valueOf(id.toString()));});
        old.groups().keySet().forEach(id->{if(!next.groups().containsKey(id))removedGroups.add(StringTag.valueOf(id.toString()));});
        t.put("Objects",objects);t.put("Groups",groups);t.put("RemovedObjects",removedObjects);t.put("RemovedGroups",removedGroups);t.put("Issues",issues(next));t.put("Flags",SceneSerialization.flags(next));t.putInt("DataVersion",next.dataVersion());return t;
    }
    private EditorViews(){}
}
