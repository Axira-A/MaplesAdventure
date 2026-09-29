package dev.maplesadventure.authoring;

import com.mojang.serialization.Codec;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.EditorRateLimit;
import dev.maplesadventure.editor.network.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorSafetyTest {
    private static final ResourceLocation ID=ResourceLocation.parse("test:scene");
    private final ComponentRegistry registry=BuiltinComponents.createRegistry();
    private MaplesScene scene(){return MaplesScene.empty(ID,ResourceLocation.parse("minecraft:overworld"),"Scene");}
    @Test void allOperationWireVariantsRoundtrip(){
        UUID id=UUID.randomUUID();var component=BuiltinComponents.RADIUS;
        List<EditorOperation> operations=List.of(new EditorOperation.RenameScene("New"),new EditorOperation.CreateObject("M",EditorTransform.origin(),null,true),new EditorOperation.DeleteObject(id,2),new EditorOperation.DuplicateObject(id,2),
                new EditorOperation.RenameObject(id,2,"N"),new EditorOperation.SetTransform(id,2,EditorTransform.origin()),new EditorOperation.AddComponent(id,2,component),new EditorOperation.RemoveComponent(id,2,component),
                new EditorOperation.PatchComponent(id,2,component,Map.of("radius",EditorValue.decimal(2.5))),new EditorOperation.CreateGroup("G",null),new EditorOperation.RenameGroup(id,2,"R"),new EditorOperation.DeleteGroup(id,2),new EditorOperation.MoveObject(id,2,null),new EditorOperation.MoveGroup(id,2,null));
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{for(var op:operations){var request=new EditorPayloads.Request(UUID.randomUUID(),UUID.randomUUID(),EditorPayloads.Intent.OPERATION,ID,"",4,op);EditorPayloads.Request.CODEC.encode(b,request);assertEquals(request,EditorPayloads.Request.CODEC.decode(b));}}
        finally{b.release();}
    }
    @Test void hostileWireLengthsAndTagsRejectBeforeAllocation(){
        var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);
        try{
            b.writeVarInt(999);assertThrows(RuntimeException.class,()->EditorWire.operation(b));b.clear();
            b.writeVarInt(-1);assertThrows(RuntimeException.class,()->EditorWire.count(b,32));b.clear();
            b.writeVarInt(EditorValue.Kind.DOUBLE.ordinal());b.writeUtf("NaN");assertThrows(RuntimeException.class,()->EditorWire.value(b));b.clear();
            b.writeVarInt(8);b.writeUUID(UUID.randomUUID());b.writeVarLong(0);EditorWire.id(b,BuiltinComponents.RADIUS);b.writeVarInt(65);
            assertThrows(RuntimeException.class,()->EditorWire.operation(b));
        }finally{b.release();}
        assertThrows(IllegalArgumentException.class,()->new EditorPayloads.Page(UUID.randomUUID(),UUID.randomUUID(),EditorPayloads.Kind.SNAPSHOT,0,1,new byte[EditorLimits.PAGE_BYTES+1]));
        assertThrows(IllegalArgumentException.class,()->EditorValue.string("x".repeat(1025)));
        assertThrows(IllegalArgumentException.class,()->new EditorValue(EditorValue.Kind.RESOURCE_LOCATION,"Bad ID"));
    }
    @Test void schemaTypesAndConstraints(){
        for(var kind:List.of(EditorValue.Kind.BOOLEAN,EditorValue.Kind.INTEGER,EditorValue.Kind.DOUBLE,EditorValue.Kind.STRING,EditorValue.Kind.ENUM,EditorValue.Kind.RESOURCE_LOCATION)){
            var field=new InspectorField("test","test.field",kind,0,10,.5,64,false,false,List.of("a","b"),null);
            var value=new EditorValue(kind,switch(kind){case BOOLEAN->"true";case INTEGER->"5";case DOUBLE->"2.5";case RESOURCE_LOCATION->"test:item";default->"a";});
            assertDoesNotThrow(()->field.validate(value));assertEquals(field,EditorViews.field(EditorViews.field(field)));
        }
        assertThrows(IllegalArgumentException.class,()->InspectorField.number("radius",0,10,1).validate(EditorValue.decimal(-1)));
        var readonly=new InspectorField("test","test",EditorValue.Kind.STRING,0,0,0,10,true,true,List.of(),null);
        assertThrows(IllegalArgumentException.class,()->readonly.validate(EditorValue.string("hello")));
    }
    @Test void registryRejectsDuplicateAndLateRegistrationAndIsolatesExceptions(){
        record C(int value){}
        var descriptor=new ComponentDescriptor<>(ResourceLocation.parse("test:c"),1,"test.c",()->new C(1),Codec.INT.fieldOf("value").xmap(C::new,C::value).codec(),List.<ComponentDescriptor.Field<C>>of(),c->List.of());
        registry.register(descriptor);assertThrows(IllegalArgumentException.class,()->registry.register(descriptor));registry.freeze();
        assertThrows(IllegalStateException.class,()->registry.register(descriptor));
        var data=registry.create(descriptor.id());var nbt=data.data();nbt.putInt("value",999);assertEquals(1,data.data().getInt("value"));
    }
    @Test void futureAndCorruptSceneRemainReadOnlyAndOriginalDataIsSaved(){
        var tag=SceneSerialization.save(scene());tag.putInt("DataVersion",5);tag.putString("UnknownFutureField","do not lose");
        var root=new CompoundTag();var entries=new ListTag();entries.add(tag);root.putInt("DataVersion",1);root.put("Scenes",entries);
        var saved=AuthoringSavedData.load(root,null);assertTrue(saved.scene(ID).readOnly());
        assertEquals(tag,saved.save(new CompoundTag(),null).getList("Scenes",10).getCompound(0));
        assertThrows(IllegalArgumentException.class,()->saved.put(scene()));
        root.putInt("DataVersion",77);var future=AuthoringSavedData.load(root,null);assertTrue(future.readOnly());assertEquals(root,future.save(new CompoundTag(),null));
    }
    @Test void groupCycleAndDepthRejectWithoutMutation(){
        var service=new EditorOperationService(registry);MaplesScene current=scene();UUID parent=null;
        for(int depth=0;depth<16;depth++){var result=service.apply(current,current.revision(),new EditorOperation.CreateGroup("g",parent));current=result.scene();parent=result.selected();}
        var full=current;var last=parent;
        assertThrows(EditorOperationService.Rejected.class,()->service.apply(full,full.revision(),new EditorOperation.CreateGroup("too deep",last)));
        assertThrows(EditorOperationService.Rejected.class,()->service.apply(full,full.revision(),new EditorOperation.MoveGroup(last,0,last)));
        assertEquals(16,full.groups().size());
    }
    @Test void dataLimitsAndInvalidValuesReject(){
        assertThrows(IllegalArgumentException.class,()->MaplesScene.empty(ID,ID,"x".repeat(129)));
        for(double v:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-Double.POSITIVE_INFINITY,30_000_001})assertThrows(IllegalArgumentException.class,()->new EditorTransform(new net.minecraft.world.phys.Vec3(v,0,0),0,0));
        var registry=BuiltinComponents.createRegistry();var tag=SceneSerialization.save(scene());
        var components=new HashMap<ResourceLocation,ComponentData>();for(int i=0;i<33;i++){var id=ResourceLocation.parse("test:c"+i);components.put(id,new ComponentData(id,1,new CompoundTag()));}
        assertThrows(IllegalArgumentException.class,()->new MaplesObject(UUID.randomUUID(),"x",EditorTransform.origin(),null,components,0));
        assertEquals(tag,SceneSerialization.read(SceneSerialization.bytes(tag),EditorLimits.SCENE_BYTES));
        assertThrows(IllegalArgumentException.class,()->SceneSerialization.read(new byte[10],1));
    }
    @Test void rateLimitAllowsTwentyPerSecondAfterBurst(){var rate=new EditorRateLimit();for(int i=0;i<40;i++)assertTrue(rate.take(0));assertFalse(rate.take(0));for(int i=0;i<20;i++)assertTrue(rate.take(20));assertFalse(rate.take(20));}
    @Test void commonApiDoesNotReferenceClientTypes()throws Exception{
        for(var method:MaplesEditorApi.class.getDeclaredMethods())for(var type:method.getParameterTypes())assertFalse(type.getName().startsWith("net.minecraft.client"));
        try(var paths=java.nio.file.Files.walk(java.nio.file.Path.of("src/main/java/dev/maplesadventure/authoring"))){for(var path:paths.filter(p->p.toString().endsWith(".java")).toList())assertFalse(java.nio.file.Files.readString(path).contains("net.minecraft.client"));}
    }
}
