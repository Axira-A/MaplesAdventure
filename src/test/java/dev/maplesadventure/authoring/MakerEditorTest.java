package dev.maplesadventure.authoring;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.client.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.logic.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.client.editor.*;
import dev.maplesadventure.editor.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MakerEditorTest {
    private final ComponentRegistry registry=BuiltinComponents.createRegistry();
    private final EditorOperationService operations=new EditorOperationService(registry);
    private static final UUID AUTHOR=UUID.randomUUID();
    private MaplesScene empty(){return MaplesScene.empty(ResourceLocation.parse("maker:test"),ResourceLocation.parse("minecraft:overworld"),"场景");}
    private MaplesScene apply(MaplesScene s,EditorOperation op){return operations.apply(s,s.revision(),op).scene();}
    private MaplesScene edit(EditorHistoryService h,MaplesScene s,EditorOperation op){var next=apply(s,op);h.record(s,next,AUTHOR,op);return next;}
    private MaplesScene history(EditorHistoryService h,MaplesScene s,boolean redo){var p=h.prepare(s,s.revision(),redo,registry);h.committed(p);return p.result();}
    @Test void beginnerNamesHideIdsAndAdvancedExposesThem(){var id=ResourceLocation.parse("maplesadventure:show_message");assertEquals("显示消息",MakerPresentation.name("显示消息",id,false));assertFalse(MakerPresentation.name(null,id,false).contains(":"));assertTrue(MakerPresentation.name("Show message",id,true).contains(id.toString()));}
    @Test void groupMappingsPreserveNestedExpressions(){var leaf=ConditionExpression.leaf(BuiltinLogic.CONDITIONS.defaults(BuiltinLogic.id("always")));var tree=new ConditionExpression(ConditionExpression.Kind.ALL,null,List.of(leaf,MakerPresentation.invert(leaf)));
        var any=MakerPresentation.group(tree,ConditionExpression.Kind.ANY);assertEquals(tree.children(),any.children());assertEquals(tree,MakerPresentation.invert(MakerPresentation.invert(tree)));assertEquals(List.of(MakerPresentation.invert(tree)),MakerPresentation.group(MakerPresentation.invert(tree),ConditionExpression.Kind.ALL).children());
        assertTrue(MakerPresentation.conditionKey(ConditionExpression.Kind.ALL).endsWith(".all"));}
    @Test void onlyNewFlagReferencesDefaultToPlayer(){var original=BuiltinLogic.CONDITIONS.defaults(BuiltinLogic.id("flag_equals"));assertEquals("WORLD",original.fields().get("scope").text());assertEquals("PLAYER",MakerPresentation.newDefinition(original).fields().get("scope").text());assertEquals("WORLD",original.fields().get("scope").text());}
    @Test void chineseCatalogIdentityMigrationAndNormalization(){var scene=apply(empty(),new EditorOperation.CreateFlag("  已见过教程  "));var f=scene.flags().values().iterator().next();assertEquals("已见过教程",f.name());assertTrue(f.id().toString().startsWith("maplesadventure:flags/"));
        scene=apply(scene,new EditorOperation.RenameFlag(f.id(),"教程完成"));assertEquals(Set.of(f.id()),scene.flags().keySet());assertEquals(scene,SceneSerialization.load(SceneSerialization.save(scene),registry));
        var legacy=SceneSerialization.save(empty());legacy.putInt("DataVersion",1);legacy.remove("Flags");var migrated=SceneSerialization.load(legacy,registry);assertEquals(2,migrated.dataVersion());assertFalse(migrated.readOnly());assertTrue(migrated.flags().isEmpty());
        assertEquals("é",FlagDefinition.normalizeName("e\u0301"));}
    @Test void invalidCatalogIsPreservedReadOnly(){var scene=SceneSerialization.save(empty());scene.put("Flags",StringTag.valueOf("broken"));var root=new CompoundTag();root.putInt("DataVersion",1);var list=new ListTag();list.add(scene);root.put("Scenes",list);
        var saved=AuthoringSavedData.load(root,null);assertTrue(saved.scene(empty().id()).readOnly());assertEquals(root,saved.save(new CompoundTag(),null));}
    @Test void flagPickerUsesNamesAndCollectsLegacyReferences(){var scene=apply(empty(),new EditorOperation.CreateFlag("教程"));var f=scene.flags().values().iterator().next();scene=apply(scene,new EditorOperation.CreateTrigger("区域",EditorTransform.origin(),null));var o=scene.objects().values().iterator().next();
        var condition=BuiltinLogic.CONDITIONS.defaults(BuiltinLogic.id("flag_equals"));var binding=new LogicBinding(UUID.randomUUID(),true,BuiltinLogic.EVENTS.defaults(BuiltinLogic.ENTER),ConditionExpression.leaf(condition),List.of());
        scene=apply(scene,new EditorOperation.SetLogic(o.id(),o.revision(),new LogicComponent(List.of(binding))));var choices=MakerPresentation.flags(scene);assertEquals("教程",choices.get(f.id()));assertEquals("example",choices.get(ResourceLocation.parse("maplesadventure:example")));}
    @Test void createDeleteUndoKeepsUuidAndMonotonicRevisions(){var h=new EditorHistoryService();var scene=edit(h,empty(),new EditorOperation.CreateObject("A",EditorTransform.origin(),null,true));var id=scene.objects().keySet().iterator().next();long previous=scene.revision();
        scene=history(h,scene,false);assertTrue(scene.objects().isEmpty());assertTrue(scene.revision()>previous);scene=history(h,scene,true);assertTrue(scene.objects().containsKey(id));long objectRevision=scene.objects().get(id).revision();
        scene=edit(h,scene,new EditorOperation.DeleteObject(id,objectRevision));scene=history(h,scene,false);assertTrue(scene.objects().get(id).revision()>objectRevision);assertEquals(id,scene.objects().get(id).id());}
    @Test void renameTransformComponentsAndLogicUndoAsAtomicSteps(){var h=new EditorHistoryService();var scene=edit(h,empty(),new EditorOperation.CreateTrigger("A",EditorTransform.origin(),null));var id=scene.objects().keySet().iterator().next();
        var logic=new LogicComponent(List.of(new LogicBinding(UUID.randomUUID(),true,BuiltinLogic.EVENTS.defaults(BuiltinLogic.ENTER),ConditionExpression.all(),List.of(BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message"))))));
        var transform=new EditorTransform(new Vec3(5,64,8),30,-10);var before=scene.objects().get(id);
        scene=edit(h,scene,new EditorOperation.CommitDraft(id,before.revision(),"B",transform,Map.of(BuiltinComponents.BOX,Map.of("sizeX",EditorValue.decimal(8))),logic));
        scene=history(h,scene,false);var restored=scene.objects().get(id);assertEquals(before.name(),restored.name());assertEquals(before.transform(),restored.transform());assertEquals(before.components(),restored.components());
        scene=history(h,scene,true);assertEquals("B",scene.objects().get(id).name());assertEquals(transform,scene.objects().get(id).transform());assertEquals(logic,LogicComponent.read(scene.objects().get(id).components().get(LogicComponent.ID).data()));}
    @Test void groupDeleteUndoRestoresOrganizationWithoutMovingObjects(){var h=new EditorHistoryService();var scene=edit(h,empty(),new EditorOperation.CreateGroup("G",null));var group=scene.groups().keySet().iterator().next();scene=edit(h,scene,new EditorOperation.CreateObject("O",EditorTransform.origin(),group,true));var id=scene.objects().keySet().iterator().next();
        scene=edit(h,scene,new EditorOperation.DeleteGroup(group,scene.groups().get(group).revision()));scene=history(h,scene,false);assertEquals(group,scene.objects().get(id).group());assertEquals(EditorTransform.origin(),scene.objects().get(id).transform());}
    @Test void newEditInvalidatesRedoAndFailedPrepareDoesNotConsumeHistory(){var h=new EditorHistoryService();var scene=edit(h,empty(),new EditorOperation.RenameScene("A"));var current=scene;
        assertThrows(EditorOperationService.Rejected.class,()->h.prepare(current,current.revision()-1,false,registry));assertEquals(1,h.state(current.id()).undo());
        scene=history(h,scene,false);assertEquals(1,h.state(scene.id()).redo());scene=edit(h,scene,new EditorOperation.RenameScene("B"));assertEquals(0,h.state(scene.id()).redo());}
    @Test void historyBoundsAndOutOfBandChangesCannotInjectRestores(){var h=new EditorHistoryService();var scene=empty();for(int i=0;i<70;i++)scene=edit(h,scene,new EditorOperation.RenameScene("Scene "+i));assertEquals(50,h.state(scene.id()).undo());
        scene=apply(scene,new EditorOperation.RenameScene("External"));var current=scene;assertThrows(EditorOperationService.Rejected.class,()->h.prepare(current,current.revision(),false,registry));assertEquals(0,h.state(scene.id()).undo());}
    @Test void historyByteBudgetEvictsOldEntries(){var h=new EditorHistoryService(50,1024,2048);var scene=empty();for(int i=0;i<10;i++)scene=edit(h,scene,new EditorOperation.RenameScene("X".repeat(128)));assertTrue(h.state(scene.id()).undo()<10);assertTrue(h.state(scene.id()).undo()>0);}
    @Test void readonlyHistoryCannotMutateAndUnknownComponentsSurviveUndo(){var h=new EditorHistoryService();var scene=apply(empty(),new EditorOperation.CreateObject("O",EditorTransform.origin(),null,true));var tag=SceneSerialization.save(scene);var c=tag.getList("Objects",10).getCompound(0).getList("Components",10).getCompound(0);c.putString("Type","missing:opaque");c.getCompound("Data").putString("Private","keep");scene=SceneSerialization.load(tag,registry);var id=scene.objects().keySet().iterator().next();var original=scene.objects().get(id).components();
        scene=edit(h,scene,new EditorOperation.RenameObject(id,scene.objects().get(id).revision(),"New"));scene=history(h,scene,false);assertEquals(original,scene.objects().get(id).components());
        tag=SceneSerialization.save(scene);tag.putInt("DataVersion",99);var readonly=SceneSerialization.load(tag,registry);assertThrows(EditorOperationService.Rejected.class,()->h.prepare(readonly,readonly.revision(),false,registry));}
    @Test void localDraftUndoCoalescesTypingAndPreservesStructuralLogic(){var h=new EditorDraftController();var a=new EditorDraftController.Snapshot(Map.of("text","A"),new LogicComponent(List.of()));var b=new EditorDraftController.Snapshot(Map.of("text","AB"),new LogicComponent(List.of()));var c=new EditorDraftController.Snapshot(Map.of("text","ABC"),new LogicComponent(List.of()));
        h.record(a,b,"text",10);h.record(b,c,"text",20);assertEquals(a,h.undo().orElseThrow());assertFalse(h.dirty());assertEquals(c,h.redo().orElseThrow());assertTrue(h.dirty());h.record(c,a,null,30);assertFalse(h.canRedo());}
    @Test void returnContextRestoresOnlyValidSelection(){var scene=apply(empty(),new EditorOperation.CreateObject("O",EditorTransform.origin(),null,true));var id=scene.objects().keySet().iterator().next();var context=new EditorReturnContext(scene.dimension(),scene.id(),id,false,true,3,40);assertEquals(id,context.validSelection(scene));assertNull(context.validSelection(empty()));
        assertNull(new EditorReturnContext(scene.dimension(),scene.id(),null,true,false,0,0).validSelection(scene));}
    @Test void acceptedOperationsMarkSavedDataDirtyWithoutExtraSaveCommand(){var store=new AuthoringSavedData();store.put(empty());long epoch=store.changeEpoch();var next=apply(empty(),new EditorOperation.RenameScene("Saved"));store.put(next);assertTrue(store.isDirty());assertEquals(epoch+1,store.changeEpoch());assertEquals("Saved",store.scene(next.id()).name());}
    @Test void optionalPresentationApiIsAdditive(){var id=ResourceLocation.parse("maker:sample_"+UUID.randomUUID());var info=new EditorPresentation("sample.category","sample.description",null,false,"sample.sentence");MaplesEditorClientApi.registerPresentation(EditorPresentation.Target.ACTION,id,info);assertEquals(info,MaplesEditorClientApi.presentation(EditorPresentation.Target.ACTION,id).orElseThrow());assertTrue(MaplesEditorClientApi.presentation(EditorPresentation.Target.COMPONENT,id).isEmpty());}
    @Test void globalBudgetEvictsChronologicallyEvenWhenOlderSceneIsViewed(){
        var h=new EditorHistoryService(50,1024,1024);var a=empty();var b=MaplesScene.empty(ResourceLocation.parse("maker:second"),a.dimension(),"场景");
        a=edit(h,a,new EditorOperation.RenameScene("A".repeat(128)));h.state(a.id());
        b=edit(h,b,new EditorOperation.RenameScene("B".repeat(128)));assertEquals(0,h.state(a.id()).undo());assertEquals(1,h.state(b.id()).undo());
    }
    @Test void historyKeepsActorNameAndBothDirectionsAndPrepareAloneCannotCommit(){
        var h=new EditorHistoryService();var a=empty();var op=new EditorOperation.RenameScene("New");var b=apply(a,op);h.record(a,b,AUTHOR,"Author A",op);
        var p=h.prepare(b,b.revision(),false,registry);assertEquals(1,h.state(a.id()).undo());assertEquals("Author A",h.state(a.id()).authorName());
        h.committed(p);assertEquals(0,h.state(a.id()).undo());assertEquals("Author A",h.state(a.id()).redoAuthorName());assertFalse(h.state(a.id()).redoDescription().isBlank());
        assertThrows(IllegalStateException.class,()->h.committed(p));
    }
    @Test void catalogRejectsDuplicateNamesAndUnknownIdsWithoutMutatingScene(){
        var s=apply(empty(),new EditorOperation.CreateFlag("教程"));
        assertThrows(RuntimeException.class,()->apply(s,new EditorOperation.CreateFlag(" 教程 ")));
        assertThrows(RuntimeException.class,()->apply(s,new EditorOperation.RenameFlag(ResourceLocation.parse("maker:missing"),"其他")));
        assertThrows(RuntimeException.class,()->apply(s,new EditorOperation.CreateFlag(" ")));
        assertEquals(1,s.flags().size());
        var flags=new HashMap<ResourceLocation,FlagDefinition>();for(int i=0;i<1025;i++){var id=ResourceLocation.parse("maker:f"+i);flags.put(id,new FlagDefinition(id,"F"+i));}
        assertThrows(RuntimeException.class,()->new MaplesScene(s.id(),s.dimension(),s.name(),2,s.revision(),s.objects(),s.groups(),List.of(),false,flags));
    }
    @Test void draftHistoryDoesNotCoalesceStructuralEditsOrEraseDroppedChanges(){
        var h=new EditorDraftController();var current=new EditorDraftController.Snapshot(Map.of("text","0"),null);
        for(int i=1;i<=60;i++){var next=new EditorDraftController.Snapshot(Map.of("text",""+i),null);h.record(current,next,null,i);current=next;}
        int undone=0;while(h.undo().isPresent())undone++;assertEquals(50,undone);assertTrue(h.dirty());h.clear();assertFalse(h.dirty());assertFalse(h.canRedo());
    }
    @Test void historyBudgetIncludesActorNamesAndDoesNotDriftAfterEviction(){
        var h=new EditorHistoryService(50,400,800);var scene=empty();
        for(int i=0;i<100;i++){var op=new EditorOperation.RenameScene("S"+i);var next=apply(scene,op);h.record(scene,next,AUTHOR,"A".repeat(100),op);scene=next;}
        // Every entry exceeds the scene budget once its author label is included.
        assertEquals(0,h.state(scene.id()).undo());
        var op=new EditorOperation.RenameScene("End");var next=apply(scene,op);h.record(scene,next,AUTHOR,"A",op);
        assertEquals(1,h.state(next.id()).undo());
    }
}
