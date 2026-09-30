package dev.maplesadventure.authoring;

import com.mojang.serialization.Codec;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.logic.*;
import dev.maplesadventure.authoring.logic.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.network.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LogicAuthoringTest {
    private static ResourceLocation id(String path){return ResourceLocation.parse("logic_test:"+path);}
    private static LogicDefinition enter(){return BuiltinLogic.EVENTS.defaults(BuiltinLogic.ENTER);}
    private static LogicBinding binding(ConditionExpression c,List<LogicDefinition> actions){return new LogicBinding(UUID.randomUUID(),true,enter(),c,actions);}
    private static LogicExecutionBudget budget(){var b=new LogicExecutionBudget();b.begin(1);return b;}
    private static ComponentDescriptor<Boolean> schema(String name){return new ComponentDescriptor<>(id(name),1,"test",()->false,Codec.BOOL.fieldOf("value").codec(),List.of(new ComponentDescriptor.Field<>(
        new InspectorField("value","value",EditorValue.Kind.BOOLEAN,0,1,1,5,false,false,List.of(),null),v->new EditorValue(EditorValue.Kind.BOOLEAN,v.toString()),(v,n)->Boolean.parseBoolean(n.text()))),v->List.of());}
    @Test void registriesAreSeparateDuplicateIdsRejectedAndFreezeIsFinal(){
        var events=new LogicTypeRegistry();var conditions=new LogicTypeRegistry();var actions=new LogicTypeRegistry();var s=schema("same");
        events.register(new EventType<>(s,true,false));conditions.register(new ConditionType<>(s,false,(c,v)->v));actions.register(new ActionType<>(s,false,(c,v)->{}));
        assertEquals(1,events.entries().size());assertEquals(1,conditions.entries().size());assertEquals(1,actions.entries().size());
        assertThrows(IllegalArgumentException.class,()->events.register(new EventType<>(s,true,false)));events.freeze();assertThrows(IllegalStateException.class,()->events.register(new EventType<>(schema("later"),true,false)));
    }
    @Test void unknownTypesAndFieldsCannotCompile(){
        var unknown=new LogicDefinition(id("missing"),1,Map.of());assertThrows(IllegalArgumentException.class,()->BuiltinLogic.ACTIONS.resolve(unknown,null));
        var always=BuiltinLogic.CONDITIONS.defaults(BuiltinLogic.id("always"));var fields=new HashMap<>(always.fields());fields.put("class",new EditorValue(EditorValue.Kind.STRING,"java.lang.Runtime"));
        assertThrows(IllegalArgumentException.class,()->BuiltinLogic.CONDITIONS.resolve(new LogicDefinition(always.type(),1,fields),null));
    }
    @Test void schemaUsesCodecAndValidatesTypedValues(){
        var registry=new LogicTypeRegistry();registry.register(new ConditionType<>(schema("bool"),false,(c,v)->v));
        var d=registry.defaults(id("bool"));assertFalse(registry.resolve(d,null).condition().test(null));
        assertTrue(registry.resolve(new LogicDefinition(d.type(),1,Map.of("value",new EditorValue(EditorValue.Kind.BOOLEAN,"true"))),null).condition().test(null));
        assertThrows(IllegalArgumentException.class,()->registry.resolve(new LogicDefinition(d.type(),1,Map.of("value",EditorValue.decimal(1))),null));
        assertThrows(IllegalArgumentException.class,()->registry.resolve(new LogicDefinition(d.type(),2,d.fields()),null));
    }
    @Test void allAnyNotAndEmptyPolicy(){
        var yes=ConditionExpression.leaf(BuiltinLogic.CONDITIONS.defaults(BuiltinLogic.id("always")));
        assertTrue(CompiledBinding.compile(binding(ConditionExpression.all(),List.of()),true,null).condition().test(null,budget()));
        assertFalse(CompiledBinding.compile(binding(new ConditionExpression(ConditionExpression.Kind.ANY,null,List.of()),List.of()),true,null).condition().test(null,budget()));
        var not=new ConditionExpression(ConditionExpression.Kind.NOT,null,List.of(yes));assertFalse(CompiledBinding.compile(binding(not,List.of()),true,null).condition().test(null,budget()));
        var any=new ConditionExpression(ConditionExpression.Kind.ANY,null,List.of(not,yes));assertTrue(CompiledBinding.compile(binding(any,List.of()),true,null).condition().test(null,budget()));
    }
    @Test void depthNodesAndMalformedTreesRejected(){
        ConditionExpression tree=ConditionExpression.all();for(int i=1;i<8;i++)tree=new ConditionExpression(ConditionExpression.Kind.NOT,null,List.of(tree));
        final var max=tree;assertThrows(IllegalArgumentException.class,()->new ConditionExpression(ConditionExpression.Kind.NOT,null,List.of(max)));
        assertThrows(IllegalArgumentException.class,()->new ConditionExpression(ConditionExpression.Kind.NOT,null,List.of()));
        assertThrows(IllegalArgumentException.class,()->new ConditionExpression(ConditionExpression.Kind.LEAF,null,List.of()));
        assertThrows(IllegalArgumentException.class,()->new ConditionExpression(ConditionExpression.Kind.ALL,null,Collections.nCopies(64,ConditionExpression.all())));
    }
    @Test void completeBindingValidatesBeforeAnySideEffects(){
        var bad=new LogicDefinition(id("removed_action"),1,Map.of());
        var good=BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message"));
        assertThrows(IllegalArgumentException.class,()->CompiledBinding.compile(binding(ConditionExpression.all(),List.of(good,bad)),true,null));
        assertThrows(IllegalArgumentException.class,()->CompiledBinding.compile(binding(ConditionExpression.all(),List.of()),false,null));
        var withoutPlayer=id("no_player");BuiltinLogic.EVENTS.register(new EventType<>(new ComponentDescriptor<>(withoutPlayer,1,"test",()->false,Codec.BOOL.fieldOf("v").codec(),List.of(),v->List.of()),false,false));
        var b=new LogicBinding(UUID.randomUUID(),true,BuiltinLogic.EVENTS.defaults(withoutPlayer),ConditionExpression.all(),List.of(good));
        assertThrows(IllegalArgumentException.class,()->CompiledBinding.compile(b,true,null));
    }
    @Test void actionsKeepOrderAndBudgetStopsFurtherWork(){
        var order=new ArrayList<Integer>();var b=new CompiledBinding(binding(ConditionExpression.all(),List.of()),(c,budget)->true,List.of(c->order.add(1),c->order.add(2),c->order.add(3)));
        b.execute(null,budget());assertEquals(List.of(1,2,3),order);
        var budget=budget();for(int i=0;i<LogicLimits.OPERATIONS_PER_TICK;i++)budget.use();assertThrows(IllegalStateException.class,()->b.execute(null,budget));assertEquals(3,order.size());
        budget.begin(1);assertEquals(0,budget.remaining());budget.begin(2);assertEquals(4096,budget.remaining());
    }
    @Test void componentAndScenePersistenceKeepIdsOrderAndUnknownData(){
        var removed=new LogicDefinition(id("removed"),3,Map.of("custom",new EditorValue(EditorValue.Kind.STRING,"preserve")));
        var logic=new LogicComponent(List.of(binding(ConditionExpression.all(),List.of(removed,BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message"))))));
        assertEquals(logic,LogicComponent.read(logic.write()));
        var uuid=UUID.randomUUID();var o=new MaplesObject(uuid,"Trigger",EditorTransform.origin(),null,Map.of(LogicComponent.ID,logic.component()),7);
        var scene=new MaplesScene(id("scene"),id("dim"),"Scene",1,9,Map.of(uuid,o),Map.of(),List.of(),false);
        var restored=SceneSerialization.load(SceneSerialization.save(scene),BuiltinComponents.createRegistry());assertEquals(o,restored.objects().get(uuid));
        assertFalse(LogicValidation.object(o,null).isEmpty());assertEquals(logic.component(),restored.objects().get(uuid).components().get(LogicComponent.ID));
    }
    @Test void malformedStoredLogicCannotCreateAnUnboundedTree(){
        var t=new LogicComponent(List.of(binding(ConditionExpression.all(),List.of()))).write();
        var node=t.getList("Bindings",10).getCompound(0).getCompound("Conditions");
        for(int i=0;i<12;i++){node.putString("Kind","NOT");var list=new ListTag();var child=new CompoundTag();list.add(child);node.put("Children",list);node=child;}
        node.putString("Kind","ALL");node.put("Children",new ListTag());assertThrows(IllegalArgumentException.class,()->LogicComponent.read(t));
    }
    @Test void flagsPersistIndependentlyAndFutureDataIsReadOnly(){
        var a=new GameFlagState();var b=new GameFlagState();a.set(id("flag"),true);assertTrue(a.get(id("flag")));assertFalse(b.get(id("flag")));
        var restored=new GameFlagState();restored.deserializeNBT(null,a.serializeNBT(null));assertTrue(restored.get(id("flag")));restored.set(id("flag"),false);assertFalse(restored.get(id("flag")));
        var future=a.serializeNBT(null);future.putInt("Version",99);restored.deserializeNBT(null,future);assertEquals(future,restored.serializeNBT(null));assertThrows(IllegalStateException.class,()->restored.set(id("x"),true));
    }
    @Test void flagsAreBoundedAndInvalidIdsFail(){var flags=new GameFlagState();for(int i=0;i<4096;i++)flags.set(id("f"+i),true);
        assertThrows(IllegalArgumentException.class,()->flags.set(id("overflow"),true));assertThrows(RuntimeException.class,()->ResourceLocation.parse("not a valid id"));}
    @Test void edgeTrackingOnlyEmitsTransitionsAndKeepsPlayersSeparate(){
        var edges=new TriggerEdges<String>();var a=UUID.randomUUID();var b=UUID.randomUUID();
        assertTrue(edges.update(a,Set.of()).entered().isEmpty());assertEquals(Set.of("one"),edges.update(a,Set.of("one")).entered());assertTrue(edges.update(a,Set.of("one")).entered().isEmpty());
        assertEquals(Set.of("one"),edges.update(b,Set.of("one")).entered());assertEquals(Set.of("one"),edges.update(a,Set.of()).exited());assertTrue(edges.update(a,Set.of()).exited().isEmpty());
        edges.forget(b);assertEquals(Set.of("one"),edges.update(b,Set.of("one")).entered());edges.clear();
    }
    @Test void volumeRotationAndSpatialCoverageIncludeNegativeCoordinates(){
        var v=new TriggerVolume(new EditorTransform(new Vec3(-16,64,-16),90,0),new Vec3(1,2,4),-1,Math.sqrt(21));
        assertTrue(v.contains(new Vec3(-13,64,-16)));assertFalse(v.contains(new Vec3(-16,64,-13)));
        var index=new TriggerSpatialIndex<String>();index.add(id("dim"),v,"rotated");
        assertTrue(index.candidates(id("dim"),new Vec3(-13,64,-16)).contains("rotated"));assertTrue(index.candidates(id("other"),v.transform().position()).isEmpty());
        var giant=new TriggerVolume(EditorTransform.origin(),null,1024,1024);index.add(id("dim"),giant,"large");assertTrue(index.candidates(id("dim"),new Vec3(1000,0,0)).contains("large"));
        assertFalse(index.candidates(id("dim"),new Vec3(20000,0,0)).contains("large"));
    }
    @Test void typedWireRoundTripsAndRejectsCountsBeforeAllocation(){
        var c=new LogicComponent(List.of(binding(ConditionExpression.all(),List.of(BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message"))))));
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),RegistryAccess.EMPTY);try{LogicWire.write(buffer,c);assertEquals(c,LogicWire.read(buffer));buffer.clear();buffer.writeVarInt(9);assertThrows(IllegalArgumentException.class,()->LogicWire.read(buffer));}finally{buffer.release();}
        assertThrows(IllegalArgumentException.class,()->new LogicComponent(Collections.nCopies(2,c.bindings().getFirst())));
        assertThrows(IllegalArgumentException.class,()->new LogicDefinition(id("x"),1,Map.of("v",new EditorValue(EditorValue.Kind.STRING,"x".repeat(1025)))));
    }
    @Test void triggerCreationAndDraftTransactionAreAtomicAndRevisionChecked(){
        var service=new EditorOperationService(BuiltinComponents.createRegistry());var scene=MaplesScene.empty(id("scene2"),id("dim"),"S");
        scene=service.apply(scene,0,new EditorOperation.CreateTrigger("Trigger",EditorTransform.origin(),null)).scene();var o=scene.objects().values().iterator().next();
        assertEquals(Set.of(BuiltinComponents.BOX,LogicComponent.ID),o.components().keySet());assertTrue(LogicComponent.read(o.components().get(LogicComponent.ID).data()).bindings().isEmpty());
        var before=scene;var bad=new EditorOperation.CommitDraft(o.id(),o.revision(),"New",o.transform(),Map.of(BuiltinComponents.BOX,Map.of("sizeX",EditorValue.decimal(-2))),null);
        assertThrows(EditorOperationService.Rejected.class,()->service.apply(before,before.revision(),bad));assertEquals("Trigger",before.objects().get(o.id()).name());
        var logic=new LogicComponent(List.of(binding(ConditionExpression.all(),List.of())));
        var saved=service.apply(before,before.revision(),new EditorOperation.CommitDraft(o.id(),o.revision(),"New",o.transform(),Map.of(BuiltinComponents.BOX,Map.of("sizeX",EditorValue.decimal(8))),logic)).scene();
        assertEquals(2,saved.revision());assertEquals(1,saved.objects().get(o.id()).revision());assertEquals("New",saved.objects().get(o.id()).name());
        assertThrows(EditorOperationService.Rejected.class,()->service.apply(saved,before.revision(),new EditorOperation.SetLogic(o.id(),o.revision(),logic)));
    }
}
