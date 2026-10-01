package dev.maplesadventure.editor;

import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.ComponentRegistry;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Ephemeral server-owned scene history. Prepare/validate first; advance only after SavedData commit. */
public final class EditorHistoryService {
    public static final int STEPS=50;
    public static final long SCENE_BUDGET=64L*1024*1024,SERVER_BUDGET=128L*1024*1024;
    private record Change<T>(T before,T after) {}
    private record Entry(UUID author,String authorName,String description,String beforeName,String afterName,long order,
                         Map<UUID,Change<MaplesObject>> objects,Map<UUID,Change<EditorGroup>> groups,
                         Map<ResourceLocation,Change<FlagDefinition>> flags,long bytes) {}
    private static final class Timeline {
        final ArrayList<Entry> entries=new ArrayList<>();int cursor;long revision,bytes;
    }
    public record State(int undo,int redo,UUID author,String description,String authorName,String redoDescription,String redoAuthorName) {}
    public record Prepared(ResourceLocation scene,long beforeRevision,MaplesScene result,UUID selected,boolean redo) {}
    private final LinkedHashMap<ResourceLocation,Timeline> timelines=new LinkedHashMap<>(16,.75f,true);
    private final int steps;private final long sceneBudget,serverBudget;
    private long bytes,sequence;
    public EditorHistoryService(){this(STEPS,SCENE_BUDGET,SERVER_BUDGET);}
    public EditorHistoryService(int steps,long sceneBudget,long serverBudget){
        if(steps<1||steps>STEPS||sceneBudget<256||serverBudget<sceneBudget)throw new IllegalArgumentException("History bounds");
        this.steps=steps;this.sceneBudget=sceneBudget;this.serverBudget=serverBudget;
    }
    public State state(ResourceLocation scene){var h=timelines.get(scene);if(h==null)return new State(0,0,null,"","","","");
        var e=h.cursor==0?null:h.entries.get(h.cursor-1);var r=h.cursor==h.entries.size()?null:h.entries.get(h.cursor);
        return new State(h.cursor,h.entries.size()-h.cursor,e==null?null:e.author,e==null?"":e.description,e==null?"":e.authorName,r==null?"":r.description,r==null?"":r.authorName);}
    public void record(MaplesScene before,MaplesScene after,UUID author,EditorOperation op){
        record(before,after,author,"",op);
    }
    public void record(MaplesScene before,MaplesScene after,UUID author,String authorName,EditorOperation op){
        var h=timelines.computeIfAbsent(before.id(),id->new Timeline());
        if(!h.entries.isEmpty()&&h.revision!=before.revision())discard(before.id());
        h=timelines.computeIfAbsent(before.id(),id->new Timeline());
        while(h.entries.size()>h.cursor){var removed=h.entries.removeLast();h.bytes-=removed.bytes;bytes-=removed.bytes;}
        var objects=diff(before.objects(),after.objects());var groups=diff(before.groups(),after.groups());var flags=diff(before.flags(),after.flags());
        long size=before.name().length()*2L+after.name().length()*2L+256;
        for(var c:objects.values())size+=(c.before==null?0:SceneSerialization.bytes(SceneSerialization.object(c.before)).length)+(c.after==null?0:SceneSerialization.bytes(SceneSerialization.object(c.after)).length);
        for(var c:groups.values())size+=(c.before==null?0:SceneSerialization.bytes(SceneSerialization.group(c.before)).length)+(c.after==null?0:SceneSerialization.bytes(SceneSerialization.group(c.after)).length);
        for(var c:flags.values())size+=128+(c.before==null?0:c.before.name().length()*2L+c.before.id().toString().length()*2L)+(c.after==null?0:c.after.name().length()*2L+c.after.id().toString().length()*2L);
        size+=authorName.length()*2L;
        var e=new Entry(author,authorName,description(op),before.name(),after.name(),++sequence,objects,groups,flags,size);
        h.entries.add(e);h.cursor++;h.bytes+=size;bytes+=size;h.revision=after.revision();
        while(h.entries.size()>steps||h.bytes>sceneBudget){var removed=h.entries.removeFirst();h.cursor=Math.max(0,h.cursor-1);h.bytes-=removed.bytes;bytes-=removed.bytes;}
        while(bytes>serverBudget){var oldest=timelines.values().stream().filter(t->!t.entries.isEmpty()).min(Comparator.comparingLong(t->t.entries.getFirst().order)).orElseThrow();
            var removed=oldest.entries.removeFirst();oldest.cursor=Math.max(0,oldest.cursor-1);oldest.bytes-=removed.bytes;bytes-=removed.bytes;}
    }
    public Prepared prepare(MaplesScene current,long expected,boolean redo,ComponentRegistry registry){
        if(current.readOnly())throw new EditorOperationService.Rejected("editor.maplesadventure.read_only");
        if(current.revision()!=expected)throw new EditorOperationService.Rejected("editor.maplesadventure.stale");
        var h=timelines.get(current.id());if(h==null||redo&&h.cursor==h.entries.size()||!redo&&h.cursor==0)
            throw new EditorOperationService.Rejected("editor.maplesadventure.history_empty");
        if(h.revision!=expected){discard(current.id());throw new EditorOperationService.Rejected("editor.maplesadventure.stale");}
        var e=h.entries.get(redo?h.cursor:h.cursor-1);long high=current.revision();
        for(var c:e.objects.values()){if(c.before!=null)high=Math.max(high,c.before.revision());if(c.after!=null)high=Math.max(high,c.after.revision());}
        for(var c:e.groups.values()){if(c.before!=null)high=Math.max(high,c.before.revision());if(c.after!=null)high=Math.max(high,c.after.revision());}
        if(high==Long.MAX_VALUE)throw new EditorOperationService.Rejected("editor.maplesadventure.limit");long revision=high+1;
        var objects=new LinkedHashMap<>(current.objects());var groups=new LinkedHashMap<>(current.groups());var flags=new LinkedHashMap<>(current.flags());
        e.objects.forEach((id,c)->{var v=redo?c.after:c.before;if(v==null)objects.remove(id);else objects.put(id,new MaplesObject(id,v.name(),v.transform(),v.group(),v.components(),revision));});
        e.groups.forEach((id,c)->{var v=redo?c.after:c.before;if(v==null)groups.remove(id);else groups.put(id,new EditorGroup(id,v.name(),v.parent(),revision));});
        e.flags.forEach((id,c)->{var v=redo?c.after:c.before;if(v==null)flags.remove(id);else flags.put(id,v);});
        var next=new MaplesScene(current.id(),current.dimension(),redo?e.afterName:e.beforeName,MaplesScene.VERSION,revision,objects,groups,List.of(),false,flags);
        next=SceneSerialization.load(SceneSerialization.save(next),registry);
        if(next.readOnly())throw new EditorOperationService.Rejected("editor.maplesadventure.invalid_operation");
        UUID selected=e.objects.keySet().stream().filter(objects::containsKey).sorted().findFirst().orElseGet(()->e.groups.keySet().stream().filter(groups::containsKey).sorted().findFirst().orElse(null));
        return new Prepared(current.id(),expected,next,selected,redo);
    }
    public void committed(Prepared p){var h=timelines.get(p.scene);if(h==null||h.revision!=p.beforeRevision)throw new IllegalStateException("History commit order");h.cursor+=p.redo?1:-1;h.revision=p.result.revision();}
    public void clear(){timelines.clear();bytes=sequence=0;}
    private void discard(ResourceLocation id){var old=timelines.remove(id);if(old!=null)bytes-=old.bytes;}
    private static <K,T> Map<K,Change<T>> diff(Map<K,T> a,Map<K,T> b){var result=new LinkedHashMap<K,Change<T>>();var keys=new LinkedHashSet<>(a.keySet());keys.addAll(b.keySet());for(var k:keys)if(!Objects.equals(a.get(k),b.get(k)))result.put(k,new Change<>(a.get(k),b.get(k)));return Collections.unmodifiableMap(result);}
    private static String description(EditorOperation op){return "editor.maplesadventure.history."+switch(op){
        case EditorOperation.RenameScene ignored->"scene";case EditorOperation.CreateObject ignored->"create";case EditorOperation.CreateTrigger ignored->"create";
        case EditorOperation.DeleteObject ignored->"delete";case EditorOperation.DuplicateObject ignored->"duplicate";case EditorOperation.RenameObject ignored->"rename";
        case EditorOperation.SetTransform ignored->"transform";case EditorOperation.AddComponent ignored->"component";case EditorOperation.RemoveComponent ignored->"component";case EditorOperation.PatchComponent ignored->"component";
        case EditorOperation.CreateGroup ignored->"group";case EditorOperation.RenameGroup ignored->"group";case EditorOperation.DeleteGroup ignored->"group";case EditorOperation.MoveGroup ignored->"group";case EditorOperation.MoveObject ignored->"group";
        case EditorOperation.SetLogic ignored->"rule";case EditorOperation.CommitDraft ignored->"draft";case EditorOperation.CreateFlag ignored->"flag";case EditorOperation.RenameFlag ignored->"flag";};}
}
