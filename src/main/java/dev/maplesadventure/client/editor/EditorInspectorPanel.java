package dev.maplesadventure.client.editor;

import dev.maplesadventure.api.editor.client.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.logic.LogicComponent;
import dev.maplesadventure.authoring.operation.EditorOperation;
import java.util.*;
import net.minecraft.network.chat.Component;

/** Maker-oriented inspector. All edits remain a draft until the shared atomic apply action. */
public final class EditorInspectorPanel {
    public interface Host extends RuleEditor.Host {
        boolean creatingScene();void simpleField(String key,String label,String value,int length);
        void sceneCreate();void applyDraft();void discardDraft();boolean section(String id,String label);
        void transform(EditorTransform value);void organization(MaplesObject object,EditorGroup group);
        void renderRules(ComponentData component);void issues(UUID id);void mutate(EditorOperation operation);
    }
    public static void render(Host h){
        var scene=EditorClient.scene();
        if(h.creatingScene()||scene==null){h.line(tr("new_scene"));h.simpleField("scene_name",tr("name"),tr("scene_default_name"),128);
            if(h.advanced())h.simpleField("scene_id",tr("scene_id"),"",256);h.action(tr("create"),h::sceneCreate);return;}
        var id=EditorClient.selected();var object=id==null?null:scene.objects().get(id);var group=id==null?null:scene.groups().get(id);
        if(object==null&&group==null){h.line(tr("scene"));if(h.advanced())h.line(scene.id()+" · "+scene.revision());h.simpleField("scene_name",tr("name"),scene.name(),128);draft(h);h.issues(null);return;}
        h.line(tr(object!=null&&object.components().containsKey(LogicComponent.ID)?"trigger":"inspector"));
        if(h.advanced())h.line(id+" · "+(object==null?group.revision():object.revision()));
        h.simpleField("name",tr("name"),object==null?group.name():object.name(),128);draft(h);
        if(object==null){h.organization(null,group);h.issues(null);return;}
        // Region size and rules are the primary workflow; position/organization remain collapsible.
        for(var c:object.components().values().stream().sorted(Comparator.comparingInt((ComponentData c)->c.type().equals(BuiltinComponents.BOX)?0:c.type().equals(LogicComponent.ID)?2:1).thenComparing(c->c.type().toString())).toList()){
            var schema=EditorClient.schemas.get(c.type());var info=MaplesEditorClientApi.presentation(EditorPresentation.Target.COMPONENT,c.type());
            if(!h.advanced()&&info.map(EditorPresentation::advancedOnly).orElse(false)){h.line(tr("advanced_function"));continue;}
            String title=c.type().equals(BuiltinComponents.BOX)?tr("region_size"):c.type().equals(LogicComponent.ID)?tr("rules"):
                    MakerPresentation.name(schema==null?null:Component.translatable(schema.label()).getString(),c.type(),h.advanced());
            if(!h.section(c.type().toString(),title))continue;
            if(c.type().equals(LogicComponent.ID))h.renderRules(c);
            else if(schema!=null){
                info.filter(v->!v.descriptionKey().isBlank()).ifPresent(v->h.line(Component.translatable(v.descriptionKey()).getString()));
                var values=EditorClient.fields(object.id(),c.type());for(var f:schema.fields()){var value=values.get(f.id());if(value!=null)h.field(c.type()+"/"+f.id(),f,value);}
            }else h.line(tr("unknown_component"));
            if(h.advanced())h.action(tr("remove_component"),()->h.mutate(new EditorOperation.RemoveComponent(object.id(),object.revision(),c.type())));
        }
        if(h.section("transform",tr("transform")))h.transform(object.transform());
        if(h.section("organization",tr("organization")))h.organization(object,null);
        h.issues(object.id());
    }
    private static void draft(Host h){h.action(tr("apply"),h::applyDraft);h.action(tr("discard"),h::discardDraft);}
    private static String tr(String key){return Component.translatable("editor.maplesadventure."+key).getString();}
    private EditorInspectorPanel(){}
}
