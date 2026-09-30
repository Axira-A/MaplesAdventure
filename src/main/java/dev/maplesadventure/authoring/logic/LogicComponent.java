package dev.maplesadventure.authoring.logic;
import com.mojang.serialization.*;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.component.ComponentData;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Versioned authoring definition. Runtime decoding is performed once per scene revision. */
public record LogicComponent(List<LogicBinding> bindings) {
    public static final ResourceLocation ID=ResourceLocation.parse("maplesadventure:logic");
    public static final Codec<LogicComponent> CODEC=CompoundTag.CODEC.comapFlatMap(tag->{
        try{return DataResult.success(read(tag));}catch(RuntimeException ex){return DataResult.error(()->"Invalid bounded logic component: "+ex.getMessage());}
    },LogicComponent::write);
    public LogicComponent {bindings=List.copyOf(bindings);if(bindings.size()>LogicLimits.BINDINGS
            ||bindings.stream().map(LogicBinding::id).distinct().count()!=bindings.size())throw new IllegalArgumentException("Binding limit/duplicate UUID");}
    public static ComponentDescriptor<LogicComponent> descriptor(){return new ComponentDescriptor<>(ID,1,"editor.maplesadventure.component.logic",()->new LogicComponent(List.of()),CODEC,List.of(),l->List.of());}
    public ComponentData component(){return new ComponentData(ID,1,write());}
    public CompoundTag write(){var tag=new CompoundTag();var list=new ListTag();
        for(var b:bindings){var t=new CompoundTag();t.putUUID("Id",b.id());t.putBoolean("Enabled",b.enabled());t.put("Event",definition(b.event()));t.put("Conditions",condition(b.conditions()));
            var actions=new ListTag();b.actions().forEach(a->actions.add(definition(a)));t.put("Actions",actions);list.add(t);}tag.put("Bindings",list);return tag;}
    public static LogicComponent read(CompoundTag tag){var bindings=new ArrayList<LogicBinding>();var list=list(tag,"Bindings",LogicLimits.BINDINGS);
        for(var e:list){var t=(CompoundTag)e;var actions=new ArrayList<LogicDefinition>();for(var a:list(t,"Actions",LogicLimits.ACTIONS))actions.add(definition((CompoundTag)a));
            if(!t.hasUUID("Id")||!t.contains("Enabled",Tag.TAG_BYTE))throw new IllegalArgumentException("Missing binding identity/state");
            bindings.add(new LogicBinding(t.getUUID("Id"),t.getBoolean("Enabled"),definition(t.getCompound("Event")),condition(t.getCompound("Conditions"),1,new int[]{0}),actions));}
        return new LogicComponent(bindings);}
    public static CompoundTag definition(LogicDefinition d){var t=new CompoundTag();t.putString("Type",d.type().toString());t.putInt("Version",d.version());var fields=new CompoundTag();
        d.fields().forEach((k,v)->{var f=new CompoundTag();f.putString("Kind",v.kind().name());f.putString("Value",v.text());fields.put(k,f);});t.put("Fields",fields);return t;}
    public static LogicDefinition definition(CompoundTag t){if(!t.contains("Fields",Tag.TAG_COMPOUND))throw new IllegalArgumentException("Missing fields");
        var fields=t.getCompound("Fields");if(fields.size()>LogicLimits.FIELDS)throw new IllegalArgumentException("Fields limit");var values=new TreeMap<String,EditorValue>();
        for(var k:fields.getAllKeys()){var f=fields.getCompound(k);values.put(k,new EditorValue(EditorValue.Kind.valueOf(f.getString("Kind")),f.getString("Value")));}
        return new LogicDefinition(ResourceLocation.parse(t.getString("Type")),t.getInt("Version"),values);}
    private static CompoundTag condition(ConditionExpression c){var t=new CompoundTag();t.putString("Kind",c.kind().name());if(c.leaf()!=null)t.put("Leaf",definition(c.leaf()));
        var children=new ListTag();c.children().forEach(x->children.add(condition(x)));t.put("Children",children);return t;}
    private static ConditionExpression condition(CompoundTag t,int depth,int[] count){if(depth>LogicLimits.DEPTH||++count[0]>LogicLimits.NODES)throw new IllegalArgumentException("Condition tree limit");
        var kind=ConditionExpression.Kind.valueOf(t.getString("Kind"));var children=new ArrayList<ConditionExpression>();for(var e:list(t,"Children",LogicLimits.NODES))children.add(condition((CompoundTag)e,depth+1,count));
        return new ConditionExpression(kind,t.contains("Leaf")?definition(t.getCompound("Leaf")):null,children);}
    private static ListTag list(CompoundTag t,String key,int max){if(!t.contains(key,Tag.TAG_LIST)||!dev.maplesadventure.authoring.persistence.SceneSerialization.validCompoundList(t,key))throw new IllegalArgumentException("Malformed list "+key);
        var l=t.getList(key,Tag.TAG_COMPOUND);if(l.size()>max)throw new IllegalArgumentException("List limit");return l;}
}
