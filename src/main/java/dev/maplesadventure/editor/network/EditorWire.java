package dev.maplesadventure.editor.network;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.operation.EditorOperation;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** Explicit wire tags; collection lengths are checked before allocation. */
public final class EditorWire {
    public static int count(RegistryFriendlyByteBuf b,int max){int n=b.readVarInt();if(n<0||n>max)throw new IllegalArgumentException("Editor wire limit");return n;}
    public static ResourceLocation id(RegistryFriendlyByteBuf b){return ResourceLocation.parse(b.readUtf(EditorLimits.ID));}
    public static void id(RegistryFriendlyByteBuf b,ResourceLocation id){b.writeUtf(id.toString(),EditorLimits.ID);}
    public static UUID optionalUuid(RegistryFriendlyByteBuf b){return b.readBoolean()?b.readUUID():null;}
    public static void optionalUuid(RegistryFriendlyByteBuf b,UUID id){b.writeBoolean(id!=null);if(id!=null)b.writeUUID(id);}
    public static EditorTransform transform(RegistryFriendlyByteBuf b){return new EditorTransform(new Vec3(b.readDouble(),b.readDouble(),b.readDouble()),b.readFloat(),b.readFloat());}
    public static void transform(RegistryFriendlyByteBuf b,EditorTransform t){b.writeDouble(t.position().x);b.writeDouble(t.position().y);b.writeDouble(t.position().z);b.writeFloat(t.yaw());b.writeFloat(t.pitch());}
    public static void value(RegistryFriendlyByteBuf b,EditorValue value){b.writeVarInt(value.kind().ordinal());b.writeUtf(value.text(),EditorLimits.STRING);}
    public static EditorValue value(RegistryFriendlyByteBuf b){return new EditorValue(EditorValue.Kind.values()[count(b,EditorValue.Kind.values().length-1)],b.readUtf(EditorLimits.STRING));}
    public static EditorOperation operation(RegistryFriendlyByteBuf b){
        return switch(count(b,13)){
            case 0->new EditorOperation.RenameScene(b.readUtf(EditorLimits.NAME));
            case 1->new EditorOperation.CreateObject(b.readUtf(EditorLimits.NAME),transform(b),optionalUuid(b),b.readBoolean());
            case 2->new EditorOperation.DeleteObject(b.readUUID(),b.readVarLong());
            case 3->new EditorOperation.DuplicateObject(b.readUUID(),b.readVarLong());
            case 4->new EditorOperation.RenameObject(b.readUUID(),b.readVarLong(),b.readUtf(EditorLimits.NAME));
            case 5->new EditorOperation.SetTransform(b.readUUID(),b.readVarLong(),transform(b));
            case 6->new EditorOperation.AddComponent(b.readUUID(),b.readVarLong(),id(b));
            case 7->new EditorOperation.RemoveComponent(b.readUUID(),b.readVarLong(),id(b));
            case 8->{UUID uuid=b.readUUID();long rev=b.readVarLong();var type=id(b);int count=count(b,EditorLimits.FIELDS);Map<String,EditorValue> fields=new LinkedHashMap<>();
                for(int i=0;i<count;i++)if(fields.putIfAbsent(b.readUtf(64),value(b))!=null)throw new IllegalArgumentException("Duplicate field");yield new EditorOperation.PatchComponent(uuid,rev,type,fields);}
            case 9->new EditorOperation.CreateGroup(b.readUtf(EditorLimits.NAME),optionalUuid(b));
            case 10->new EditorOperation.RenameGroup(b.readUUID(),b.readVarLong(),b.readUtf(EditorLimits.NAME));
            case 11->new EditorOperation.DeleteGroup(b.readUUID(),b.readVarLong());
            case 12->new EditorOperation.MoveObject(b.readUUID(),b.readVarLong(),optionalUuid(b));
            default->new EditorOperation.MoveGroup(b.readUUID(),b.readVarLong(),optionalUuid(b));
        };
    }
    public static void operation(RegistryFriendlyByteBuf b,EditorOperation operation){switch(operation){
        case EditorOperation.RenameScene o->{b.writeVarInt(0);b.writeUtf(o.name(),EditorLimits.NAME);}
        case EditorOperation.CreateObject o->{b.writeVarInt(1);b.writeUtf(o.name(),EditorLimits.NAME);transform(b,o.transform());optionalUuid(b,o.group());b.writeBoolean(o.marker());}
        case EditorOperation.DeleteObject o->{head(b,2,o.id(),o.revision());}
        case EditorOperation.DuplicateObject o->{head(b,3,o.id(),o.revision());}
        case EditorOperation.RenameObject o->{head(b,4,o.id(),o.revision());b.writeUtf(o.name(),EditorLimits.NAME);}
        case EditorOperation.SetTransform o->{head(b,5,o.id(),o.revision());transform(b,o.transform());}
        case EditorOperation.AddComponent o->{head(b,6,o.id(),o.revision());id(b,o.type());}
        case EditorOperation.RemoveComponent o->{head(b,7,o.id(),o.revision());id(b,o.type());}
        case EditorOperation.PatchComponent o->{head(b,8,o.id(),o.revision());id(b,o.type());if(o.fields().size()>EditorLimits.FIELDS)throw new IllegalArgumentException("Field limit");b.writeVarInt(o.fields().size());o.fields().forEach((k,v)->{b.writeUtf(k,64);value(b,v);});}
        case EditorOperation.CreateGroup o->{b.writeVarInt(9);b.writeUtf(o.name(),EditorLimits.NAME);optionalUuid(b,o.parent());}
        case EditorOperation.RenameGroup o->{head(b,10,o.id(),o.revision());b.writeUtf(o.name(),EditorLimits.NAME);}
        case EditorOperation.DeleteGroup o->{head(b,11,o.id(),o.revision());}
        case EditorOperation.MoveObject o->{head(b,12,o.id(),o.revision());optionalUuid(b,o.group());}
        case EditorOperation.MoveGroup o->{head(b,13,o.id(),o.revision());optionalUuid(b,o.parent());}
    }}
    private static void head(RegistryFriendlyByteBuf b,int type,UUID id,long revision){b.writeVarInt(type);b.writeUUID(id);b.writeVarLong(revision);}
    private EditorWire(){}
}
