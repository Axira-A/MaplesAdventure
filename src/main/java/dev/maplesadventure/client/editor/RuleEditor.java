package dev.maplesadventure.client.editor;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.logic.*;
import java.util.*;
import java.util.function.*;
import net.minecraft.network.chat.Component;

/** Schema-driven local draft. Structural edits and field edits never send packets on their own. */
public final class RuleEditor {
    public interface Host {
        void line(String text);void action(String text,Runnable action);
        void field(String key,InspectorField spec,EditorValue value);
        String text(String key,String fallback);
        void picker(String title,List<Choice> choices);
        void changed();void clearLogicFields();void invalid();
        default void beginChange(){}default boolean advanced(){return false;}
    }
    public record Choice(String label,Runnable action){}
    private LogicComponent draft;
    public RuleEditor(LogicComponent value){draft=value;}
    public LogicComponent snapshot(){return draft;}
    public LogicComponent value(Host host){var bindings=new ArrayList<LogicBinding>();
        for(var b:draft.bindings()){String p="logic/"+b.id();var actions=new ArrayList<LogicDefinition>();for(int i=0;i<b.actions().size();i++)actions.add(read(host,p+"/action/"+i,LogicTypeRegistry.Kind.ACTION,b.actions().get(i)));
            bindings.add(new LogicBinding(b.id(),b.enabled(),read(host,p+"/event",LogicTypeRegistry.Kind.EVENT,b.event()),readCondition(host,p+"/condition",b.conditions()),actions));}
        return new LogicComponent(bindings);}
    private LogicDefinition read(Host h,String path,LogicTypeRegistry.Kind kind,LogicDefinition def){var entry=EditorClient.logicSchemas.getOrDefault(kind,Map.of()).get(def.type());if(entry==null)return def;
        var fields=new LinkedHashMap<>(def.fields());for(var f:entry.schema().fields())if(!f.readOnly()){
            String text=h.text(path+"/"+f.id(),fields.get(f.id()).text());var v=new EditorValue(text.isEmpty()&&f.nullable()?EditorValue.Kind.NULL:f.kind(),text);f.validate(v);fields.put(f.id(),v);}
        return new LogicDefinition(def.type(),def.version(),fields);}
    private ConditionExpression readCondition(Host h,String path,ConditionExpression c){var children=new ArrayList<ConditionExpression>();for(int i=0;i<c.children().size();i++)children.add(readCondition(h,path+"/"+i,c.children().get(i)));
        return new ConditionExpression(c.kind(),c.leaf()==null?null:read(h,path,LogicTypeRegistry.Kind.CONDITION,c.leaf()),children);}
    private void mutate(Host h,UnaryOperator<LogicComponent> change){try{var next=change.apply(value(h));h.beginChange();draft=next;h.clearLogicFields();h.changed();}catch(RuntimeException e){h.invalid();}}
    private void binding(Host h,UUID id,UnaryOperator<LogicBinding> change){mutate(h,c->new LogicComponent(c.bindings().stream().map(b->b.id().equals(id)?change.apply(b):b).toList()));}
    public void render(Host h){
        int number=0;
        for(var b:draft.bindings()){
            String p="logic/"+b.id();h.line(tr("binding")+" "+(++number)+(h.advanced()?" · "+b.id():""));
            h.action((b.enabled()?"[x] ":"[ ] ")+tr("enabled"),()->binding(h,b.id(),v->new LogicBinding(v.id(),!v.enabled(),v.event(),v.conditions(),v.actions())));
            h.action(tr("event")+": "+label(LogicTypeRegistry.Kind.EVENT,b.event()),()->choose(h,LogicTypeRegistry.Kind.EVENT,d->binding(h,b.id(),v->new LogicBinding(v.id(),v.enabled(),d,v.conditions(),v.actions()))));
            fields(h,p+"/event",LogicTypeRegistry.Kind.EVENT,b.event());
            condition(h,b.id(),p+"/condition",b.conditions(),List.of(),0);
            h.line(tr("actions"));
            for(int i=0;i<b.actions().size();i++){final int index=i;var a=b.actions().get(i);String path=p+"/action/"+i;
                h.action((i+1)+". "+label(LogicTypeRegistry.Kind.ACTION,a),()->choose(h,LogicTypeRegistry.Kind.ACTION,d->binding(h,b.id(),v->{var list=new ArrayList<>(v.actions());list.set(index,d);return withActions(v,list);})));
                fields(h,path,LogicTypeRegistry.Kind.ACTION,a);
                if(i>0)h.action("↑ "+tr("move_up"),()->binding(h,b.id(),v->{var list=new ArrayList<>(v.actions());Collections.swap(list,index,index-1);return withActions(v,list);}));
                if(i<b.actions().size()-1)h.action("↓ "+tr("move_down"),()->binding(h,b.id(),v->{var list=new ArrayList<>(v.actions());Collections.swap(list,index,index+1);return withActions(v,list);}));
                h.action("− "+tr("remove_action"),()->binding(h,b.id(),v->{var list=new ArrayList<>(v.actions());list.remove(index);return withActions(v,list);}));
            }
            if(b.actions().size()<LogicLimits.ACTIONS)h.action("+ "+tr("action"),()->choose(h,LogicTypeRegistry.Kind.ACTION,d->binding(h,b.id(),v->{var list=new ArrayList<>(v.actions());list.add(d);return withActions(v,list);})));
            h.action("− "+tr("remove_binding"),()->mutate(h,c->new LogicComponent(c.bindings().stream().filter(v->!v.id().equals(b.id())).toList())));
        }
        if(draft.bindings().size()<LogicLimits.BINDINGS)h.action("+ "+tr("binding"),()->choose(h,LogicTypeRegistry.Kind.EVENT,d->mutate(h,c->{var list=new ArrayList<>(c.bindings());list.add(new LogicBinding(UUID.randomUUID(),true,d,ConditionExpression.all(),List.of()));return new LogicComponent(list);})));
    }
    private static LogicBinding withActions(LogicBinding b,List<LogicDefinition> a){return new LogicBinding(b.id(),b.enabled(),b.event(),b.conditions(),a);}
    private void condition(Host h,UUID binding,String prefix,ConditionExpression c,List<Integer> path,int depth){
        String pad="  ".repeat(depth);h.action(pad+tr("conditions")+": "+(c.leaf()==null?Component.translatable(MakerPresentation.conditionKey(c.kind())).getString():label(LogicTypeRegistry.Kind.CONDITION,c.leaf())),()->{
            var choices=new ArrayList<Choice>();for(var kind:List.of(ConditionExpression.Kind.ALL,ConditionExpression.Kind.ANY))choices.add(new Choice(Component.translatable(MakerPresentation.conditionKey(kind)).getString(),()->replace(h,binding,path,n->MakerPresentation.group(n,kind))));
            choices.add(new Choice(tr("invert_group"),()->replace(h,binding,path,MakerPresentation::invert)));
            if(c.kind()==ConditionExpression.Kind.LEAF)choices.add(new Choice(tr("change_condition"),()->choose(h,LogicTypeRegistry.Kind.CONDITION,d->replace(h,binding,path,n->ConditionExpression.leaf(d)))));h.picker(tr("conditions"),choices);
        });
        if(c.leaf()!=null)fields(h,prefix,LogicTypeRegistry.Kind.CONDITION,c.leaf());
        for(int i=0;i<c.children().size();i++){var childPath=new ArrayList<>(path);childPath.add(i);condition(h,binding,prefix+"/"+i,c.children().get(i),List.copyOf(childPath),depth+1);}
        if((c.kind()==ConditionExpression.Kind.ALL||c.kind()==ConditionExpression.Kind.ANY)&&depth<LogicLimits.DEPTH-1)
            h.action(pad+"+ "+tr("condition"),()->choose(h,LogicTypeRegistry.Kind.CONDITION,d->replace(h,binding,path,n->{var list=new ArrayList<>(n.children());list.add(ConditionExpression.leaf(d));return new ConditionExpression(n.kind(),null,list);})));
        if(!path.isEmpty())h.action(pad+"− "+tr("remove_condition"),()->{
            var parent=path.subList(0,path.size()-1);int index=path.getLast();replace(h,binding,parent,n->{if(n.kind()==ConditionExpression.Kind.NOT)return ConditionExpression.all();var list=new ArrayList<>(n.children());list.remove(index);return new ConditionExpression(n.kind(),null,list);});});
    }
    private void replace(Host h,UUID id,List<Integer> path,UnaryOperator<ConditionExpression> change){binding(h,id,b->new LogicBinding(b.id(),b.enabled(),b.event(),replace(b.conditions(),path,0,change),b.actions()));}
    private ConditionExpression replace(ConditionExpression c,List<Integer> path,int index,UnaryOperator<ConditionExpression> change){if(index==path.size())return change.apply(c);var children=new ArrayList<>(c.children());int child=path.get(index);children.set(child,replace(children.get(child),path,index+1,change));return new ConditionExpression(c.kind(),c.leaf(),children);}
    private void fields(Host h,String path,LogicTypeRegistry.Kind kind,LogicDefinition definition){var schema=EditorClient.logicSchemas.getOrDefault(kind,Map.of()).get(definition.type());
        if(schema==null){h.line(tr("unknown_type")+(h.advanced()?": "+definition.type():""));return;}
        var metadata=dev.maplesadventure.api.editor.client.MaplesEditorClientApi.presentation(dev.maplesadventure.api.editor.client.EditorPresentation.Target.valueOf(kind.name()),definition.type());
        if(metadata.isPresent()){
            var info=metadata.get();if(!info.descriptionKey().isBlank())h.line(Component.translatable(info.descriptionKey()).getString());
            if(!info.sentenceKey().isBlank()){String sentence=Component.translatable(info.sentenceKey()).getString();for(var entry:definition.fields().entrySet())sentence=sentence.replace("{"+entry.getKey()+"}",h.text(path+"/"+entry.getKey(),entry.getValue().text()));h.line(sentence);}
        }
        for(var f:schema.schema().fields()){var value=definition.fields().get(f.id());if(value!=null)h.field(path+"/"+f.id(),f,value);}
    }
    private void choose(Host h,LogicTypeRegistry.Kind kind,Consumer<LogicDefinition> callback){var choices=new ArrayList<Choice>();
        EditorClient.logicSchemas.getOrDefault(kind,Map.of()).values().stream().sorted(Comparator.comparing(s->s.schema().id().toString())).forEach(s->{
            var info=dev.maplesadventure.api.editor.client.MaplesEditorClientApi.presentation(dev.maplesadventure.api.editor.client.EditorPresentation.Target.valueOf(kind.name()),s.schema().id());
            if(!h.advanced()&&info.map(dev.maplesadventure.api.editor.client.EditorPresentation::advancedOnly).orElse(false))return;
            String label=MakerPresentation.name(Component.translatable(s.schema().label()).getString(),s.schema().id(),h.advanced());
            if(info.isPresent()&&!info.get().categoryKey().isBlank())label=Component.translatable(info.get().categoryKey()).getString()+" · "+label;
            choices.add(new Choice(label,()->callback.accept(MakerPresentation.newDefinition(s.defaults()))));});
        h.picker(tr(kind.name().toLowerCase(Locale.ROOT)),choices);}
    private static String label(LogicTypeRegistry.Kind k,LogicDefinition d){var s=EditorClient.logicSchemas.getOrDefault(k,Map.of()).get(d.type());return MakerPresentation.name(s==null?null:Component.translatable(s.schema().label()).getString(),d.type(),dev.maplesadventure.config.EditorClientConfig.ADVANCED.get());}
    private static String tr(String key){return Component.translatable("editor.maplesadventure.logic."+key).getString();}
}
