package dev.maplesadventure.client.editor;

import dev.maplesadventure.authoring.logic.LogicComponent;
import java.util.*;

/** Bounded local editing history, never sent as an authoritative scene snapshot. */
public final class EditorDraftController {
    public record Snapshot(Map<String,String> fields,LogicComponent logic){public Snapshot{fields=Map.copyOf(fields);}}
    private record Change(Snapshot before,Snapshot after,String field,long time) {}
    private final ArrayList<Change> changes=new ArrayList<>();private int cursor;private boolean dropped;
    public void record(Snapshot before,Snapshot after,String field,long now){
        if(before.equals(after))return;
        while(changes.size()>cursor)changes.removeLast();
        if(cursor>0&&field!=null&&field.equals(changes.get(cursor-1).field)&&now-changes.get(cursor-1).time<500_000_000L){
            var last=changes.get(cursor-1);changes.set(cursor-1,new Change(last.before,after,field,now));
        }else{changes.add(new Change(before,after,field,now));cursor++;}
        if(changes.size()>50){changes.removeFirst();cursor--;dropped=true;}
    }
    public Optional<Snapshot> undo(){return cursor==0?Optional.empty():Optional.of(changes.get(--cursor).before);}
    public Optional<Snapshot> redo(){return cursor==changes.size()?Optional.empty():Optional.of(changes.get(cursor++).after);}
    public boolean canUndo(){return cursor>0;}public boolean canRedo(){return cursor<changes.size();}public boolean dirty(){return dropped||cursor>0;}
    public void clear(){changes.clear();cursor=0;dropped=false;}
}
