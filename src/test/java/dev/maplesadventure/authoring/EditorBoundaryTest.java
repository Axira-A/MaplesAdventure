package dev.maplesadventure.authoring;

import com.mojang.serialization.Codec;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.network.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorBoundaryTest {
    private static ResourceLocation id(String value){return ResourceLocation.parse("test:"+value);}
    private static MaplesScene scene(){return MaplesScene.empty(id("scene"),id("dimension"),"S");}
    @Test void exactCountLimitsAndOverflow(){
        var objects=new LinkedHashMap<UUID,MaplesObject>();
        for(int i=0;i<EditorLimits.OBJECTS;i++){UUID uuid=UUID.randomUUID();objects.put(uuid,new MaplesObject(uuid,"O",EditorTransform.origin(),null,Map.of(),0));}
        var full=new MaplesScene(id("scene"),id("dimension"),"S",1,0,objects,Map.of(),List.of(),false);
        assertEquals(4096,full.objects().size());
        var service=new EditorOperationService(BuiltinComponents.createRegistry());
        assertThrows(EditorOperationService.Rejected.class,()->service.apply(full,0,new EditorOperation.CreateObject("overflow",EditorTransform.origin(),null,false)));
        var groups=new LinkedHashMap<UUID,EditorGroup>();
        for(int i=0;i<1025;i++){var uuid=UUID.randomUUID();groups.put(uuid,new EditorGroup(uuid,"G",null,0));}
        assertThrows(IllegalArgumentException.class,()->new MaplesScene(id("s"),id("d"),"S",1,0,Map.of(),groups,List.of(),false));
        var store=new AuthoringSavedData();for(int i=0;i<256;i++)store.put(MaplesScene.empty(id("s"+i),id("d"),"S"));
        assertThrows(IllegalArgumentException.class,()->store.put(scene()));
    }
    @Test void componentAndObjectBytesCannotExceedBudget(){
        var payload=new CompoundTag();payload.putByteArray("bytes",new byte[EditorLimits.COMPONENT_BYTES+1]);
        var object=new MaplesObject(UUID.randomUUID(),"O",EditorTransform.origin(),null,Map.of(id("opaque"),new ComponentData(id("opaque"),1,payload)),0);
        assertThrows(IllegalArgumentException.class,()->SceneSerialization.object(SceneSerialization.object(object)));
        var components=new LinkedHashMap<ResourceLocation,ComponentData>();
        payload.putByteArray("bytes",new byte[16000]);for(int i=0;i<5;i++)components.put(id("c"+i),new ComponentData(id("c"+i),1,payload));
        var large=new MaplesObject(UUID.randomUUID(),"O",EditorTransform.origin(),null,components,0);
        assertThrows(IllegalArgumentException.class,()->SceneSerialization.object(SceneSerialization.object(large)));
    }
    @Test void invalidKnownComponentRetainsOriginalData(){
        var registry=BuiltinComponents.createRegistry();var service=new EditorOperationService(registry);
        var initial=service.apply(scene(),0,new EditorOperation.CreateObject("O",EditorTransform.origin(),null,true)).scene();
        var tag=SceneSerialization.save(initial);var component=tag.getList("Objects",10).getCompound(0).getList("Components",10).getCompound(0);
        component.getCompound("Data").putString("visible","not a boolean");component.getCompound("Data").putInt("opaque",99);
        var restored=SceneSerialization.load(tag,registry);assertFalse(restored.issues().isEmpty());
        assertEquals(component,SceneSerialization.save(restored).getList("Objects",10).getCompound(0).getList("Components",10).getCompound(0));
    }
    @Test void addonAccessorFailureCannotCommitPartialPatch(){
        var registry=BuiltinComponents.createRegistry();
        var field=new ComponentDescriptor.Field<Integer>(new InspectorField("value","v",EditorValue.Kind.INTEGER,0,20,1,10,false,false,List.of(),null),
                v->new EditorValue(EditorValue.Kind.INTEGER,v.toString()),(v,n)->{throw new IllegalStateException("third party setter failed");});
        registry.register(new ComponentDescriptor<>(id("throwing"),1,"test",()->1,Codec.INT.fieldOf("value").codec(),List.of(field),v->List.of()));
        var original=registry.create(id("throwing"));
        assertThrows(IllegalStateException.class,()->registry.patch(original,Map.of("value",new EditorValue(EditorValue.Kind.INTEGER,"2"))));
        assertEquals(1,original.data().getInt("value"));
    }
    @Test void deltaCarriesOnlyChangesAndRevisionContinuity(){
        var service=new EditorOperationService(BuiltinComponents.createRegistry());
        var a=service.apply(scene(),0,new EditorOperation.CreateObject("A",EditorTransform.origin(),null,true)).scene();
        var b=service.apply(a,1,new EditorOperation.CreateGroup("G",null)).scene();
        var delta=EditorViews.delta(a,b);assertEquals(1,delta.getLong("Before"));assertEquals(2,delta.getLong("Revision"));
        assertTrue(delta.getList("Objects",10).isEmpty());assertEquals(1,delta.getList("Groups",10).size());
        var removed=service.apply(b,2,new EditorOperation.DeleteObject(a.objects().keySet().iterator().next(),0)).scene();
        assertEquals(1,EditorViews.delta(b,removed).getList("RemovedObjects",8).size());
    }
    @Test void snapshotPagesAreImmutableAndBounded(){
        byte[] bytes={1,2,3};var p=new EditorPayloads.Page(UUID.randomUUID(),UUID.randomUUID(),EditorPayloads.Kind.SNAPSHOT,0,1,bytes);
        bytes[0]=9;assertEquals(1,p.bytes()[0]);p.bytes()[0]=8;assertEquals(1,p.bytes()[0]);
        assertThrows(IllegalArgumentException.class,()->new EditorPayloads.Page(p.session(),p.transfer(),p.kind(),128,128,bytes));
        assertThrows(IllegalArgumentException.class,()->new EditorPayloads.Page(p.session(),p.transfer(),p.kind(),0,129,bytes));
    }
    @Test void malformedPayloadIsNeverReplacedWithAnEmptyCompound(){
        var service=new EditorOperationService(BuiltinComponents.createRegistry());
        var initial=service.apply(scene(),0,new EditorOperation.CreateObject("O",EditorTransform.origin(),null,true)).scene();
        var tag=SceneSerialization.save(initial);
        tag.getList("Objects",10).getCompound(0).getList("Components",10).getCompound(0).putString("Data","preserve this corrupt value");
        var root=new CompoundTag();var list=new ListTag();list.add(tag);root.put("Scenes",list);
        var store=AuthoringSavedData.load(root,null);
        assertTrue(store.scene(initial.id()).readOnly());
        assertEquals(tag,store.save(new CompoundTag(),null).getList("Scenes",10).getCompound(0));
        root.putString("Scenes","not a list");store=AuthoringSavedData.load(root,null);
        assertTrue(store.readOnly());assertEquals(root,store.save(new CompoundTag(),null));
        var strings=new ListTag();strings.add(StringTag.valueOf("not a compound"));root.put("Scenes",strings);
        store=AuthoringSavedData.load(root,null);assertTrue(store.readOnly());assertEquals(root,store.save(new CompoundTag(),null));
    }
    @Test void brokenReaderIsDiagnosedAndReadOnlyValuesStillValidate(){
        var registry=BuiltinComponents.createRegistry();
        var schema=new InspectorField("value","v",EditorValue.Kind.INTEGER,0,20,1,10,false,true,List.of(),null);
        var field=new ComponentDescriptor.Field<Integer>(schema,v->{if(v==2)throw new IllegalStateException("reader failed");return new EditorValue(EditorValue.Kind.INTEGER,v.toString());},(v,n)->v);
        registry.register(new ComponentDescriptor<>(id("reader"),1,"test",()->1,Codec.INT.fieldOf("value").codec(),List.of(field),v->List.of()));
        assertTrue(registry.validate(registry.create(id("reader"))).isEmpty());
        var bad=new CompoundTag();bad.putInt("value",2);
        assertEquals(ValidationIssue.Severity.ERROR,registry.validate(new ComponentData(id("reader"),1,bad)).getFirst().severity());
        assertThrows(IllegalArgumentException.class,()->schema.validate(new EditorValue(EditorValue.Kind.INTEGER,"1")));
    }
    @Test void pagedSnapshotSwitchesOnlyWhenCompleteAndRejectsMixedSessions(){
        var assembler=new EditorPageAssembler();UUID session=UUID.randomUUID(),transfer=UUID.randomUUID();
        var first=new EditorPayloads.Page(session,transfer,EditorPayloads.Kind.SNAPSHOT,0,2,new byte[]{1,2});
        assertNull(assembler.accept(first));assertEquals(1,assembler.pendingTransfers());
        assertArrayEquals(new byte[]{1,2,3},assembler.accept(new EditorPayloads.Page(session,transfer,first.kind(),1,2,new byte[]{3})));
        assertEquals(0,assembler.pendingTransfers());
        assembler.accept(first);
        assertThrows(IllegalArgumentException.class,()->assembler.accept(new EditorPayloads.Page(UUID.randomUUID(),transfer,first.kind(),1,2,new byte[]{3})));
        assertEquals(0,assembler.pendingTransfers());
        assembler.accept(first);assertThrows(IllegalArgumentException.class,()->assembler.accept(first));
        for(int i=0;i<4;i++)assembler.accept(new EditorPayloads.Page(session,UUID.randomUUID(),first.kind(),0,2,new byte[]{1}));
        assertThrows(IllegalArgumentException.class,()->assembler.accept(first));
        assembler.accept(first);assembler.clear();assertEquals(0,assembler.pendingTransfers());
    }
}
