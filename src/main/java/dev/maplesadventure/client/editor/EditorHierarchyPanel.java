package dev.maplesadventure.client.editor;

import dev.maplesadventure.authoring.*;
import java.util.*;

/** Scene organization, independent of widget rebuilding and inspector drafts. */
public final class EditorHierarchyPanel {
    public static List<Object> entries(MaplesScene scene,Set<UUID> collapsed,String search){var result=new ArrayList<Object>();collect(scene,collapsed,search.toLowerCase(Locale.ROOT),null,result,0);return result;}
    private static void collect(MaplesScene scene,Set<UUID> collapsed,String search,UUID parent,List<Object> result,int depth){
        if(depth>EditorLimits.DEPTH)return;
        scene.groups().values().stream().filter(g->Objects.equals(parent,g.parent())).sorted(Comparator.comparing(EditorGroup::name).thenComparing(EditorGroup::id)).forEach(g->{
            if(search.isEmpty()||g.name().toLowerCase(Locale.ROOT).contains(search))result.add(g);
            if(!collapsed.contains(g.id())||!search.isEmpty())collect(scene,collapsed,search,g.id(),result,depth+1);
        });
        scene.objects().values().stream().filter(o->Objects.equals(parent,o.group())&&(search.isEmpty()||o.name().toLowerCase(Locale.ROOT).contains(search)))
                .sorted(Comparator.comparing(MaplesObject::name).thenComparing(MaplesObject::id)).forEach(result::add);
    }
    private EditorHierarchyPanel(){}
}
