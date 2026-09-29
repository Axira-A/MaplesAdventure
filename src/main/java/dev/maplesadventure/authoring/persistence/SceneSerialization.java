package dev.maplesadventure.authoring.persistence;

import dev.maplesadventure.api.editor.ValidationIssue;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.*;
import java.io.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Versioned NBT value encoding. Unknown component payloads remain opaque and round-trip intact. */
public final class SceneSerialization {
    public static byte[] bytes(CompoundTag tag) {
        try { var out = new ByteArrayOutputStream(); NbtIo.write(tag, new DataOutputStream(out)); return out.toByteArray(); }
        catch (IOException e) { throw new IllegalStateException("NBT serialization", e); }
    }
    public static CompoundTag read(byte[] data, int maximum) {
        if (data.length > maximum) throw new IllegalArgumentException("editor.maplesadventure.limit");
        try { return NbtIo.read(new DataInputStream(new ByteArrayInputStream(data)), new NbtAccounter(maximum, 32)); }
        catch (IOException e) { throw new IllegalArgumentException("Invalid authoring NBT", e); }
    }
    public static CompoundTag transform(EditorTransform transform) {
        CompoundTag tag = new CompoundTag(); Vec3 p = transform.position();
        tag.putDouble("X", p.x); tag.putDouble("Y", p.y); tag.putDouble("Z", p.z);
        tag.putFloat("Yaw", transform.yaw()); tag.putFloat("Pitch", transform.pitch()); return tag;
    }
    public static EditorTransform transform(CompoundTag tag) {
        return new EditorTransform(new Vec3(tag.getDouble("X"),tag.getDouble("Y"),tag.getDouble("Z")),tag.getFloat("Yaw"),tag.getFloat("Pitch"));
    }
    public static CompoundTag object(MaplesObject object) {
        var tag = new CompoundTag(); tag.putUUID("Id", object.id()); tag.putString("Name", object.name());
        tag.put("Transform", transform(object.transform())); if (object.group() != null) tag.putUUID("Group", object.group());
        tag.putLong("Revision", object.revision()); ListTag components = new ListTag();
        object.components().values().stream().sorted(Comparator.comparing(c -> c.type().toString())).forEach(c -> {
            var entry = new CompoundTag(); entry.putString("Type",c.type().toString()); entry.putInt("Version",c.version()); entry.put("Data",c.data()); components.add(entry);
        }); tag.put("Components", components); return tag;
    }
    public static MaplesObject object(CompoundTag tag) {
        if (bytes(tag).length > EditorLimits.OBJECT_BYTES) throw new IllegalArgumentException("editor.maplesadventure.limit");
        if(!validCompoundList(tag,"Components")
                ||tag.contains("Transform")&&!tag.contains("Transform",Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("editor.maplesadventure.corrupt_object");
        var components = new LinkedHashMap<ResourceLocation,ComponentData>(); var list = tag.getList("Components",Tag.TAG_COMPOUND);
        if (list.size() > EditorLimits.COMPONENTS) throw new IllegalArgumentException("editor.maplesadventure.limit");
        for (int i=0;i<list.size();i++) {
            var entry=list.getCompound(i); var id=ResourceLocation.parse(entry.getString("Type"));
            // Do not silently replace a malformed payload with getCompound's empty default.
            // The containing scene is quarantined read-only and SavedData keeps its exact source.
            if(!entry.contains("Data",Tag.TAG_COMPOUND))throw new IllegalArgumentException("editor.maplesadventure.invalid_component");
            var data=entry.getCompound("Data");
            if (bytes(data).length>EditorLimits.COMPONENT_BYTES || components.putIfAbsent(id,new ComponentData(id,entry.contains("Version")?entry.getInt("Version"):1,data))!=null)
                throw new IllegalArgumentException("editor.maplesadventure.invalid_component");
        }
        return new MaplesObject(tag.getUUID("Id"),tag.contains("Name")?tag.getString("Name"):"Object",transform(tag.getCompound("Transform")),
                tag.hasUUID("Group")?tag.getUUID("Group"):null,components,Math.max(0,tag.getLong("Revision")));
    }
    public static CompoundTag group(EditorGroup group) {
        var tag=new CompoundTag();tag.putUUID("Id",group.id());tag.putString("Name",group.name());tag.putLong("Revision",group.revision());
        if(group.parent()!=null)tag.putUUID("Parent",group.parent());return tag;
    }
    public static EditorGroup group(CompoundTag tag) {return new EditorGroup(tag.getUUID("Id"),tag.getString("Name"),tag.hasUUID("Parent")?tag.getUUID("Parent"):null,Math.max(0,tag.getLong("Revision")));}
    public static CompoundTag save(MaplesScene scene) {
        var tag=new CompoundTag();tag.putInt("DataVersion",scene.dataVersion());tag.putString("Id",scene.id().toString());
        tag.putString("Dimension",scene.dimension().toString());tag.putString("Name",scene.name());tag.putLong("Revision",scene.revision());
        ListTag objects=new ListTag(), groups=new ListTag();
        scene.objects().values().stream().sorted(Comparator.comparing(MaplesObject::id)).forEach(o->objects.add(object(o)));
        scene.groups().values().stream().sorted(Comparator.comparing(EditorGroup::id)).forEach(g->groups.add(group(g)));
        tag.put("Objects",objects);tag.put("Groups",groups);return tag;
    }
    public static MaplesScene load(CompoundTag tag,ComponentRegistry registry) {
        if(bytes(tag).length>EditorLimits.SCENE_BYTES)throw new IllegalArgumentException("editor.maplesadventure.limit");
        var id=ResourceLocation.parse(tag.getString("Id")); var dimension=ResourceLocation.parse(tag.getString("Dimension"));
        Map<UUID,MaplesObject> objects=new LinkedHashMap<>();Map<UUID,EditorGroup> groups=new LinkedHashMap<>();List<ValidationIssue> issues=new ArrayList<>();
        var ol=tag.getList("Objects",Tag.TAG_COMPOUND);var gl=tag.getList("Groups",Tag.TAG_COMPOUND);
        if(ol.size()>EditorLimits.OBJECTS||gl.size()>EditorLimits.GROUPS)throw new IllegalArgumentException("editor.maplesadventure.limit");
        boolean damaged=!validCompoundList(tag,"Objects")||!validCompoundList(tag,"Groups");
        if(damaged)issues.add(ValidationIssue.error("editor.maplesadventure.corrupt_object"));
        for(int i=0;i<ol.size();i++)try {
            var o=object(ol.getCompound(i));if(objects.putIfAbsent(o.id(),o)!=null)throw new IllegalArgumentException("Duplicate UUID");
            o.components().forEach((type,data)->registry.validate(data).forEach(issue->issues.add(new ValidationIssue(issue.severity(),o.id(),type,issue.field(),issue.message()))));
            if(!ol.getCompound(i).contains("Transform")||!ol.getCompound(i).contains("Name"))issues.add(new ValidationIssue(ValidationIssue.Severity.WARNING,o.id(),null,"","editor.maplesadventure.repaired"));
        }catch(RuntimeException error){damaged=true;issues.add(ValidationIssue.error("editor.maplesadventure.corrupt_object"));}
        for(int i=0;i<gl.size();i++)try{var g=group(gl.getCompound(i));if(groups.putIfAbsent(g.id(),g)!=null)throw new IllegalArgumentException("Duplicate UUID");}
        catch(RuntimeException error){damaged=true;issues.add(ValidationIssue.error("editor.maplesadventure.corrupt_group"));}
        for(var o:objects.values())if(o.group()!=null&&!groups.containsKey(o.group())){damaged=true;issues.add(new ValidationIssue(ValidationIssue.Severity.ERROR,o.id(),null,"group","editor.maplesadventure.missing_reference"));}
        for(var g:groups.values()) {Set<UUID> chain=new HashSet<>();UUID current=g.id();
            while(current!=null){var next=groups.get(current);if(next==null||!chain.add(current)||chain.size()>EditorLimits.DEPTH){damaged=true;issues.add(ValidationIssue.error("editor.maplesadventure.invalid_group"));break;}current=next.parent();}}
        int version=tag.contains("DataVersion")?tag.getInt("DataVersion"):1;
        if(version!=MaplesScene.VERSION)issues.add(ValidationIssue.error("editor.maplesadventure.unknown_version"));
        return new MaplesScene(id,dimension,tag.contains("Name")?tag.getString("Name"):id.getPath(),version,Math.max(0,tag.getLong("Revision")),objects,groups,issues,damaged||version!=MaplesScene.VERSION);
    }
    public static boolean validCompoundList(CompoundTag tag,String key){
        return !tag.contains(key)||(tag.get(key) instanceof ListTag list
                &&(list.isEmpty()||list.getElementType()==Tag.TAG_COMPOUND));
    }
    private SceneSerialization() {}
}
