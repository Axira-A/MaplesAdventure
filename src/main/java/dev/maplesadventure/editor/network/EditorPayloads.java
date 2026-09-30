package dev.maplesadventure.editor.network;

import dev.maplesadventure.authoring.EditorLimits;
import dev.maplesadventure.authoring.operation.EditorOperation;
import java.util.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public final class EditorPayloads {
    public enum Intent { OPEN, CLOSE, SELECT_SCENE, CREATE_SCENE, OPERATION, RESYNC, VALIDATE, SAVE }
    public enum Kind { SESSION, CATALOG, SCHEMA, SNAPSHOT, DELTA, RESULT, CLOSED }
    public record Request(UUID requestId,UUID session,Intent intent,ResourceLocation scene,String name,long revision,EditorOperation operation) implements CustomPacketPayload {
        public static final Type<Request> TYPE=new Type<>(ResourceLocation.parse("maplesadventure:editor_request"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Request> CODEC=StreamCodec.of((b,p)->{
            b.writeUUID(p.requestId);EditorWire.optionalUuid(b,p.session);b.writeVarInt(p.intent.ordinal());
            if(p.intent==Intent.SELECT_SCENE||p.intent==Intent.CREATE_SCENE||p.intent==Intent.OPERATION){EditorWire.id(b,p.scene);}
            if(p.intent==Intent.CREATE_SCENE)b.writeUtf(p.name,EditorLimits.NAME);
            if(p.intent==Intent.OPERATION){b.writeVarLong(p.revision);EditorWire.operation(b,p.operation);}
        },b->{UUID request=b.readUUID(),session=EditorWire.optionalUuid(b);Intent intent=Intent.values()[EditorWire.count(b,Intent.values().length-1)];
            ResourceLocation scene=(intent==Intent.SELECT_SCENE||intent==Intent.CREATE_SCENE||intent==Intent.OPERATION)?EditorWire.id(b):null;
            String name=intent==Intent.CREATE_SCENE?b.readUtf(EditorLimits.NAME):"";long revision=intent==Intent.OPERATION?b.readVarLong():0;
            return new Request(request,session,intent,scene,name,revision,intent==Intent.OPERATION?EditorWire.operation(b):null);});
        @Override public Type<Request> type(){return TYPE;}
    }
    /** Server snapshots may contain NBT; C2S has no raw serialized authoring data. */
    public record Page(UUID session,UUID transfer,Kind kind,int index,int count,byte[] bytes) implements CustomPacketPayload {
        public Page{bytes=bytes.clone();if(bytes.length>EditorLimits.PAGE_BYTES||count<1||count>128||index<0||index>=count)throw new IllegalArgumentException("Editor page limit");}
        @Override public byte[] bytes(){return bytes.clone();}
        public static final Type<Page> TYPE=new Type<>(ResourceLocation.parse("maplesadventure:editor_page"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Page> CODEC=StreamCodec.of((b,p)->{b.writeUUID(p.session);b.writeUUID(p.transfer);b.writeVarInt(p.kind.ordinal());b.writeVarInt(p.index);b.writeVarInt(p.count);b.writeByteArray(p.bytes);},
                b->new Page(b.readUUID(),b.readUUID(),Kind.values()[EditorWire.count(b,Kind.values().length-1)],EditorWire.count(b,127),EditorWire.count(b,128),b.readByteArray(EditorLimits.PAGE_BYTES)));
        @Override public Type<Page> type(){return TYPE;}
    }
    private EditorPayloads(){}
}
