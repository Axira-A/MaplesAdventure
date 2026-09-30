package dev.maplesadventure.editor.network;
import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.authoring.logic.*;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;

/** No NBT on C2S. Bound every count before allocation/recursion. */
public final class LogicWire {
    public static LogicComponent read(RegistryFriendlyByteBuf b){int n=EditorWire.count(b,LogicLimits.BINDINGS);var bindings=new ArrayList<LogicBinding>();
        for(int i=0;i<n;i++){var id=b.readUUID();boolean enabled=b.readBoolean();var event=readDefinition(b);var condition=readCondition(b,1,new int[]{0});
            int count=EditorWire.count(b,LogicLimits.ACTIONS);var actions=new ArrayList<LogicDefinition>();for(int a=0;a<count;a++)actions.add(readDefinition(b));bindings.add(new LogicBinding(id,enabled,event,condition,actions));}
        return new LogicComponent(bindings);}
    public static void write(RegistryFriendlyByteBuf b,LogicComponent logic){b.writeVarInt(logic.bindings().size());
        for(var binding:logic.bindings()){b.writeUUID(binding.id());b.writeBoolean(binding.enabled());writeDefinition(b,binding.event());writeCondition(b,binding.conditions());
            b.writeVarInt(binding.actions().size());binding.actions().forEach(d->writeDefinition(b,d));}}
    private static LogicDefinition readDefinition(RegistryFriendlyByteBuf b){var id=EditorWire.id(b);int version=b.readVarInt();int n=EditorWire.count(b,LogicLimits.FIELDS);var fields=new LinkedHashMap<String,EditorValue>();
        for(int i=0;i<n;i++)if(fields.putIfAbsent(b.readUtf(64),EditorWire.value(b))!=null)throw new IllegalArgumentException("Duplicate logic field");return new LogicDefinition(id,version,fields);}
    private static void writeDefinition(RegistryFriendlyByteBuf b,LogicDefinition d){EditorWire.id(b,d.type());b.writeVarInt(d.version());b.writeVarInt(d.fields().size());d.fields().forEach((k,v)->{b.writeUtf(k,64);EditorWire.value(b,v);});}
    private static ConditionExpression readCondition(RegistryFriendlyByteBuf b,int depth,int[] nodes){if(depth>LogicLimits.DEPTH||++nodes[0]>LogicLimits.NODES)throw new IllegalArgumentException("Condition tree limit");
        var kind=ConditionExpression.Kind.values()[EditorWire.count(b,3)];var leaf=b.readBoolean()?readDefinition(b):null;int count=EditorWire.count(b,LogicLimits.NODES);var children=new ArrayList<ConditionExpression>();
        for(int i=0;i<count;i++)children.add(readCondition(b,depth+1,nodes));return new ConditionExpression(kind,leaf,children);}
    private static void writeCondition(RegistryFriendlyByteBuf b,ConditionExpression c){b.writeVarInt(c.kind().ordinal());b.writeBoolean(c.leaf()!=null);if(c.leaf()!=null)writeDefinition(b,c.leaf());b.writeVarInt(c.children().size());c.children().forEach(n->writeCondition(b,n));}
    private LogicWire(){}
}
