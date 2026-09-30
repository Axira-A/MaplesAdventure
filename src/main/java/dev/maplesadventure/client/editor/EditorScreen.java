package dev.maplesadventure.client.editor;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.logic.*;
import dev.maplesadventure.config.EditorClientConfig;
import dev.maplesadventure.authoring.operation.EditorOperation;
import dev.maplesadventure.client.input.AdventureKeyMappings;
import dev.maplesadventure.editor.network.EditorPayloads;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import org.lwjgl.glfw.GLFW;

/** Spectator authoring workspace; no world blur, detached camera or direct server-state writes. */
public final class EditorScreen extends Screen implements LogicInspector.Host {
    private static final int TOP=52, BOTTOM=32;
    private EditorDockLayout layout;
    private double leftRatio=EditorClientConfig.LEFT.get(), rightRatio=EditorClientConfig.RIGHT.get();
    private int splitter,pickerScroll;
    private boolean selectTool=true;
    private String search="",pickerSearch="",pickerTitle="",footer="",footerError="";
    private List<LogicInspector.Choice> pickerChoices;
    private final Set<String> collapsedSections=new HashSet<>();
    private LogicInspector logic;
    private Object baselineObject;
    private UUID baselineSelection;
    private ResourceLocation baselineScene;
    private String baselineSceneName;
    private Button sceneButton;
    private final Set<EditBox> editableFields=Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<AbstractWidget> hierarchyWidgets=new ArrayList<>();
    private record Label(int x,int y,String text,int color){}
    private final List<Label> labels=new ArrayList<>();
    private final Map<String,String> draft=new HashMap<>();
    private final List<EditBox> formFields=new ArrayList<>();
    private final Map<EditBox,Runnable> enterActions=new IdentityHashMap<>();
    private final Set<Integer> movementKeys=new HashSet<>();
    private final Set<UUID> collapsed=new HashSet<>();
    private int left,right,hierarchyScroll,inspectorScroll,row;
    private boolean createScene,looking,rotate,deleteConfirm,rebuilding,dirtyDraft;
    private UUID deleting,dragId;
    private long dragRevision;
    private int dragAxis=-1;
    private double dragStart;
    private EditorTransform dragOriginal,preview;
    public EditorScreen(){super(Component.translatable("editor.maplesadventure.title"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float tick){}
    public EditorTransform preview(){return preview;}
    public boolean hasDraft(){return dirtyDraft||dragAxis>=0;}
    public void authoritativeUpdate(){
        var s=EditorClient.scene();var id=EditorClient.selected();
        Object value=s==null||id==null?null:s.objects().containsKey(id)?s.objects().get(id):s.groups().get(id);
        if(!Objects.equals(baselineObject,value)||!Objects.equals(baselineSelection,id)||!Objects.equals(baselineScene,s==null?null:s.id())
                ||id==null&&!Objects.equals(baselineSceneName,s==null?null:s.name())){
            if(dirtyDraft&&Objects.equals(id,baselineSelection)&&!EditorClient.pending())EditorClient.error="editor.maplesadventure.stale";
            baselineObject=value;baselineSelection=id;baselineScene=s==null?null:s.id();
            cancelDrag();draft.clear();logic=null;dirtyDraft=false;deleteConfirm=false;pickerChoices=null;
            if(minecraft!=null)init();
        }else if(minecraft!=null){buildHierarchy();for(var c:children())if(c instanceof AbstractWidget w)w.active=!EditorClient.pending();
            for(var box:editableFields)box.setEditable(!EditorClient.pending()&&!EditorClient.storeReadOnly&&(s==null||!s.readOnly()));}
        baselineSceneName=s==null?null:s.name();
        if(sceneButton!=null)sceneButton.setMessage(Component.literal(s==null?tr("select_scene"):s.name()));
        updateStatus();
    }
    @Override protected void init(){
        rebuilding=true;clearWidgets();labels.clear();hierarchyWidgets.clear();formFields.clear();editableFields.clear();enterActions.clear();
        layout=EditorDockLayout.resolve(width,leftRatio,rightRatio);left=layout.left();right=layout.right();
        int brand=Math.min(130,width/4);labels.add(new Label(7,9,"MaplesAdventure",0xFFE6BF77));
        sceneButton=button(brand,4,Math.max(40,width-brand-64),20,Component.literal(EditorClient.scene()==null?tr("select_scene"):EditorClient.scene().name()),this::scenePicker);
        button(width-58,4,53,20,key("exit"),this::onClose);
        int tw=Math.max(24,(width-10)/7);
        tool(5,tw,"select",()->{selectTool=true;rotate=false;cancelDrag();init();},()->selectTool);
        tool(5+tw,tw,"move",()->{selectTool=false;rotate=false;cancelDrag();init();},()->!selectTool&&!rotate);
        tool(5+tw*2,tw,"rotate",()->{selectTool=false;rotate=true;cancelDrag();init();},()->!selectTool&&rotate);
        button(5+tw*3,28,tw-3,20,key("add_object"),this::objectPicker);
        button(5+tw*4,28,tw-3,20,key("add_component"),this::componentPicker);
        button(5+tw*5,28,tw-3,20,key("save"),this::saveDrafts);
        button(5+tw*6,28,tw-3,20,key("validate"),()->EditorClient.request(EditorPayloads.Intent.VALIDATE,null,"",null));
        buildHierarchy();row=TOP+8;buildInspector();rebuilding=false;updateStatus();if(pickerChoices!=null)buildPicker();
    }
    private void tool(int x,int w,String text,Runnable action,java.util.function.BooleanSupplier active){
        var b=new EditorButton(x,28,w-3,20,key(text),action,active);b.active=!EditorClient.pending();addRenderableWidget(b);
    }
    private void buildHierarchy(){
        if(minecraft==null||pickerChoices!=null)return;
        hierarchyWidgets.forEach(this::removeWidget);hierarchyWidgets.clear();labels.removeIf(l->l.x()<left&&l.y()>=TOP);
        labels.add(new Label(7,TOP+8,tr("hierarchy"),0xFFE0E7F0));
        var searchBox=new EditBox(font,6,TOP+24,Math.max(30,left-12),18,key("search"));searchBox.setMaxLength(128);searchBox.setValue(search);searchBox.setHint(key("search"));
        searchBox.setResponder(v->{search=v;hierarchyScroll=0;rebuildTree();});hierarchyWidgets.add(addRenderableWidget(searchBox));
        var group=button(6,TOP+45,left-12,18,key("new_group"),()->{if(EditorClient.scene()!=null)EditorClient.operation(new EditorOperation.CreateGroup(tr("group"),selectedGroup()));});hierarchyWidgets.add(group);rebuildTree();
    }
    private void rebuildTree(){
        for(var w:List.copyOf(hierarchyWidgets))if(w.getY()>=TOP+67){removeWidget(w);hierarchyWidgets.remove(w);}
        if(EditorClient.scene()==null)return;
        List<Object> entries=new ArrayList<>();hierarchy(null,entries,0);int visible=Math.max(1,(height-TOP-BOTTOM-70)/20);hierarchyScroll=Math.clamp(hierarchyScroll,0,Math.max(0,entries.size()-visible));
        for(int i=hierarchyScroll;i<Math.min(entries.size(),hierarchyScroll+visible);i++){
            var entry=entries.get(i);UUID id,parent;String name;
            if(entry instanceof EditorGroup g){id=g.id();name=g.name();parent=g.parent();}
            else{var o=(MaplesObject)entry;id=o.id();name=(o.components().containsKey(LogicComponent.ID)?"◇ ":"◆ ")+o.name();parent=o.group();}
            int x=6+Math.min(left/3,depth(parent)*8),y=TOP+67+(i-hierarchyScroll)*20;
            if(entry instanceof EditorGroup){hierarchyWidgets.add(button(x,y,16,18,Component.literal(collapsed.contains(id)?">":"v"),()->{if(!collapsed.add(id))collapsed.remove(id);rebuildTree();}));x+=18;}
            var b=new EditorButton(x,y,Math.max(15,left-x-6),18,Component.literal(name),()->{createScene=false;inspectorScroll=0;EditorClient.select(id);},()->id.equals(EditorClient.selected()));
            b.setTooltip(Tooltip.create(Component.literal(name+"\n"+id)));b.active=!EditorClient.pending();hierarchyWidgets.add(addRenderableWidget(b));
        }
    }
    private void scenePicker(){
        var options=new ArrayList<LogicInspector.Choice>();EditorClient.scenes.forEach((id,name)->options.add(new LogicInspector.Choice(name+" · "+id,()->{createScene=false;inspectorScroll=0;EditorClient.request(EditorPayloads.Intent.SELECT_SCENE,id,"",null);})));
        options.add(new LogicInspector.Choice("+ "+tr("new_scene"),()->{createScene=true;inspectorScroll=0;draft.clear();init();}));picker(tr("select_scene"),options);
    }
    private void objectPicker(){if(EditorClient.scene()==null)return;picker(tr("add_object"),List.of(new LogicInspector.Choice(tr("empty"),()->create(false)),new LogicInspector.Choice(tr("marker"),()->create(true)),new LogicInspector.Choice(tr("trigger"),this::createTrigger)));}
    private void createTrigger(){var p=minecraft.player;if(p==null)return;var hit=p.pick(64,1,false);var pos=hit.getType()!=HitResult.Type.MISS?hit.getLocation():p.getEyePosition().add(p.getLookAngle().scale(4));
        EditorClient.operation(new EditorOperation.CreateTrigger(tr("trigger"),new EditorTransform(pos,EditorGeometry.yaw(p.getYRot()),p.getXRot()),selectedGroup()));
    }
    private void componentPicker(){var o=selectedObject();if(o==null)return;var choices=new ArrayList<LogicInspector.Choice>();
        for(var s:EditorClient.schemas.values())if(!o.components().containsKey(s.id()))choices.add(new LogicInspector.Choice(Component.translatable(s.label()).getString()+" · "+s.id(),()->EditorClient.operation(new EditorOperation.AddComponent(o.id(),o.revision(),s.id()))));
        picker(tr("add_component"),choices);
    }
    private void hierarchy(UUID parent,List<Object> result,int depth){if(depth>EditorLimits.DEPTH)return;var scene=EditorClient.scene();
        scene.groups().values().stream().filter(g->Objects.equals(parent,g.parent())).sorted(Comparator.comparing(EditorGroup::name).thenComparing(EditorGroup::id)).forEach(g->{if(search.isEmpty()||g.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))result.add(g);if(!collapsed.contains(g.id())||!search.isEmpty())hierarchy(g.id(),result,depth+1);});
        scene.objects().values().stream().filter(o->Objects.equals(parent,o.group())&&(search.isEmpty()||o.name().toLowerCase(Locale.ROOT).contains(search.toLowerCase(Locale.ROOT)))).sorted(Comparator.comparing(MaplesObject::name).thenComparing(MaplesObject::id)).forEach(result::add);
    }
    private int depth(UUID parent){int count=0;while(parent!=null&&EditorClient.scene()!=null&&count<16){var g=EditorClient.scene().groups().get(parent);if(g==null)break;count++;parent=g.parent();}return count;}
    private UUID selectedGroup(){var scene=EditorClient.scene();var selected=EditorClient.selected();return scene!=null&&selected!=null&&scene.groups().containsKey(selected)?selected:null;}
    private void create(boolean marker){var p=minecraft.player;if(p==null)return;var hit=p.pick(64,1,false);Vec3 pos=hit.getType()!=HitResult.Type.MISS?hit.getLocation():p.getEyePosition().add(p.getLookAngle().scale(4));
        EditorClient.operation(new EditorOperation.CreateObject(tr(marker?"marker":"object"),new EditorTransform(pos,EditorGeometry.yaw(p.getYRot()),p.getXRot()),selectedGroup(),marker));}
    private void buildInspector(){
        var scene=EditorClient.scene();
        if(createScene||scene==null){
            label(tr("new_scene"));field("scene_id",tr("scene_id"),"maplesadventure:scene",128);field("scene_name",tr("name"),"Scene",128);
            action(tr("create"),()->{try{EditorClient.request(EditorPayloads.Intent.CREATE_SCENE,ResourceLocation.parse(draft.get("scene_id")),draft.get("scene_name"),null);createScene=false;}catch(RuntimeException e){invalid();}});return;
        }
        UUID selected=EditorClient.selected();var o=selected==null?null:scene.objects().get(selected);var group=selected==null?null:scene.groups().get(selected);
        if(o==null&&group==null){label(scene.id().toString());field("scene_name",tr("name"),scene.name(),128);action(tr("apply"),()->EditorClient.operation(new EditorOperation.RenameScene(draft.get("scene_name"))));issues(null);return;}
        label(tr("inspector"));label(selected.toString());
        field("name",tr("name"),o==null?group.name():o.name(),128);
        action(tr("rename"),()->EditorClient.operation(o==null?new EditorOperation.RenameGroup(group.id(),group.revision(),draft.get("name")):new EditorOperation.RenameObject(o.id(),o.revision(),draft.get("name"))));
        if(o==null){
            action(tr("collapse"),()->{if(!collapsed.add(group.id()))collapsed.remove(group.id());init();});
            parent(group.parent(),id->EditorClient.operation(new EditorOperation.MoveGroup(group.id(),group.revision(),id)),group.id());
            deletion(group.id(),()->EditorClient.operation(new EditorOperation.DeleteGroup(group.id(),group.revision())));issues(null);return;
        }
        var t=o.transform();if(section("transform",tr("transform"))){
        field("x","X",Double.toString(t.position().x),64);field("y","Y",Double.toString(t.position().y),64);field("z","Z",Double.toString(t.position().z),64);
        field("yaw","Yaw",Float.toString(t.yaw()),64);field("pitch","Pitch",Float.toString(t.pitch()),64);
        action(tr("apply_transform"),()->{try{EditorClient.operation(new EditorOperation.SetTransform(o.id(),o.revision(),new EditorTransform(new Vec3(number("x"),number("y"),number("z")),(float)number("yaw"),(float)number("pitch"))));}catch(RuntimeException failure){invalid();}});
        }
        parent(o.group(),id->EditorClient.operation(new EditorOperation.MoveObject(o.id(),o.revision(),id)),null);
        action(tr("duplicate"),()->EditorClient.operation(new EditorOperation.DuplicateObject(o.id(),o.revision())));
        deletion(o.id(),()->EditorClient.operation(new EditorOperation.DeleteObject(o.id(),o.revision())));
        for(var component:o.components().values().stream().sorted(Comparator.comparing(c->c.type().toString())).toList()){
            var schema=EditorClient.schemas.get(component.type());var values=EditorClient.fields(o.id(),component.type());
            if(!section(component.type().toString(),schema==null?component.type().toString():Component.translatable(schema.label()).getString()))continue;
            if(component.type().equals(LogicComponent.ID)){
                try{if(component.version()!=1)throw new IllegalArgumentException("Unsupported logic version");if(logic==null)logic=new LogicInspector(LogicComponent.read(component.data()));logic.render(this);
                    action(tr("apply_component"),()->{try{EditorClient.operation(new EditorOperation.SetLogic(o.id(),o.revision(),logic.value(this)));}catch(RuntimeException failure){invalid();}});
                }catch(RuntimeException bad){label(tr("unknown_component"));}
            }else if(schema!=null){
                for(var spec:schema.fields()){
                    var value=values.get(spec.id());if(value==null)continue;String key=component.type()+"/"+spec.id();
                    field(key,spec,value);
                }
                action(tr("apply_component"),()->{try{
                    Map<String,EditorValue> changes=new LinkedHashMap<>();for(var spec:schema.fields())if(!spec.readOnly()&&values.containsKey(spec.id())){
                        String text=draft.get(component.type()+"/"+spec.id());var value=new EditorValue(text.isEmpty()&&spec.nullable()?EditorValue.Kind.NULL:spec.kind(),text);spec.validate(value);
                        if(!value.equals(values.get(spec.id())))changes.put(spec.id(),value);
                    }if(!changes.isEmpty())EditorClient.operation(new EditorOperation.PatchComponent(o.id(),o.revision(),component.type(),changes));
                }catch(RuntimeException failure){invalid();}});
            }else label(tr("unknown_component"));
            action(tr("remove_component"),()->EditorClient.operation(new EditorOperation.RemoveComponent(o.id(),o.revision(),component.type())));
        }
        action("+ "+tr("add_component"),this::componentPicker);
        issues(o.id());
    }
    private void issues(UUID object){var scene=EditorClient.scene();if(scene.readOnly())label(tr("read_only"));
        for(var issue:scene.issues())if(object==null||issue.object()==null||Objects.equals(issue.object(),object)){
            String text=issue.severity()+": "+Component.translatable(issue.message()).getString();
            if(issue.object()!=null)action(text,()->{inspectorScroll=0;EditorClient.select(issue.object());});else label(text);
        }}
    private void parent(UUID current,java.util.function.Consumer<UUID> change,UUID excluded){
        var scene=EditorClient.scene();String name=current==null?tr("root"):scene.groups().containsKey(current)?scene.groups().get(current).name():"?";
        action(tr("group")+": "+name,()->{List<UUID> ids=new ArrayList<>();ids.add(null);scene.groups().values().stream().sorted(Comparator.comparing(EditorGroup::name)).filter(g->!g.id().equals(excluded)).forEach(g->ids.add(g.id()));change.accept(ids.get((ids.indexOf(current)+1)%ids.size()));});
    }
    private void deletion(UUID id,Runnable delete){action(tr(deleteConfirm&&Objects.equals(deleting,id)?"confirm_delete":"delete"),()->{if(deleteConfirm&&Objects.equals(deleting,id))delete.run();else{deleting=id;deleteConfirm=true;init();}});}
    private void label(String text){int y=row-inspectorScroll;row+=16;if(y>=TOP+4&&y<height-BOTTOM-4)labels.add(new Label(right+6,y,text,0xFFE0E5EC));}
    private void field(String id,String name,String fallback,int max){
        field(id,name,fallback,max,false);
    }
    private EditBox field(String id,String name,String fallback,int max,boolean readOnly){
        draft.putIfAbsent(id,fallback);int y=row-inspectorScroll;row+=24;if(y<TOP+4||y+20>height-BOTTOM-4)return null;
        int available=width-right-12,offset=Math.max(24,(int)(available*.4));
        labels.add(new Label(right+6,y+5,font.plainSubstrByWidth(name,offset-4),0xFFAEBACA));var box=new EditBox(font,right+6+offset,y,Math.max(20,available-offset),18,Component.literal(name));box.setMaxLength(max);box.setValue(draft.get(id));
        box.setResponder(value->{if(!rebuilding){draft.put(id,value);dirtyDraft=true;updateStatus();}});box.setEditable(!readOnly&&!EditorClient.pending()&&!EditorClient.storeReadOnly&&(EditorClient.scene()==null||!EditorClient.scene().readOnly()));
        addRenderableWidget(box);
        if(!readOnly){formFields.add(box);editableFields.add(box);}
        return box;
    }
    private Component fieldHint(InspectorField spec){
        var hint=Component.literal(spec.kind().name());
        if(spec.kind()==EditorValue.Kind.INTEGER||spec.kind()==EditorValue.Kind.DOUBLE)
            hint.append("\n").append(Component.translatable("editor.maplesadventure.field_bounds",spec.min(),spec.max(),spec.step()));
        else hint.append("\n").append(Component.translatable("editor.maplesadventure.field_length",spec.maxLength()));
        if(spec.kind()==EditorValue.Kind.BOOLEAN)hint.append("\ntrue / false");
        if(!spec.choices().isEmpty())hint.append("\n"+String.join(" / ",spec.choices()));
        if(spec.registry()!=null)hint.append("\n"+spec.registry());
        if(spec.nullable())hint.append("\n").append(key("field_nullable"));
        if(spec.readOnly())hint.append("\n").append(key("read_only"));
        return hint;
    }
    @Override public void action(String label,Runnable action){for(var box:formFields)enterActions.put(box,action);formFields.clear();int y=row-inspectorScroll;row+=23;if(y>=TOP+4&&y+20<height-BOTTOM-4)button(right+6,y,width-right-12,20,Component.literal(label),action);}
    private Button button(int x,int y,int w,int h,Component label,Runnable action){var b=new EditorButton(x,y,Math.max(12,w),h,label,action,()->false);b.active=!EditorClient.pending();b.setTooltip(Tooltip.create(label));return addRenderableWidget(b);}
    private double number(String id){double value=Double.parseDouble(draft.get(id));if(!Double.isFinite(value))throw new IllegalArgumentException();return value;}
    @Override public void invalid(){EditorClient.error="editor.maplesadventure.invalid_field";updateStatus();}
    private static Component key(String key){return Component.translatable("editor.maplesadventure."+key);}
    private static String tr(String key){return key(key).getString();}
    private boolean section(String id,String name){action((collapsedSections.contains(id)?"> ":"v ")+name,()->{if(!collapsedSections.add(id))collapsedSections.remove(id);init();});return !collapsedSections.contains(id);}
    @Override public void line(String text){label(text);}
    @Override public String text(String key,String fallback){return draft.getOrDefault(key,fallback);}
    @Override public void changed(){dirtyDraft=true;init();}
    @Override public void clearLogicFields(){draft.keySet().removeIf(k->k.startsWith("logic/"));}
    @Override public void field(String id,InspectorField spec,EditorValue value){
        String name=Component.translatable(spec.label()).getString();draft.putIfAbsent(id,value.text());
        if(spec.kind()==EditorValue.Kind.BOOLEAN||spec.kind()==EditorValue.Kind.ENUM){
            String current=draft.get(id);action(name+": "+(spec.kind()==EditorValue.Kind.BOOLEAN?("true".equals(current)?"[x]":"[ ]"):current),()->{
                if(spec.readOnly())return;
                if(spec.kind()==EditorValue.Kind.BOOLEAN){draft.put(id,Boolean.toString(!Boolean.parseBoolean(current)));changed();}
                else{var choices=new ArrayList<LogicInspector.Choice>();for(var choice:spec.choices())choices.add(new LogicInspector.Choice(choice,()->{draft.put(id,choice);changed();}));picker(name,choices);}
            });
        }else{var box=field(id,name,value.text(),spec.maxLength(),spec.readOnly());if(box!=null)box.setTooltip(Tooltip.create(fieldHint(spec)));}
    }
    private double value(String id,double fallback){return Double.parseDouble(draft.getOrDefault(id,Double.toString(fallback)));}
    private void saveDrafts(){
        if(EditorClient.pending())return;var o=selectedObject();
        try{if(o!=null&&dirtyDraft){
            var fields=new LinkedHashMap<ResourceLocation,Map<String,EditorValue>>();
            for(var type:o.components().keySet()){
                if(type.equals(LogicComponent.ID))continue;var schema=EditorClient.schemas.get(type);if(schema==null)continue;var values=EditorClient.fields(o.id(),type);var changes=new LinkedHashMap<String,EditorValue>();
                for(var spec:schema.fields())if(!spec.readOnly()&&values.containsKey(spec.id())){var before=values.get(spec.id());String text=draft.getOrDefault(type+"/"+spec.id(),before.text());var v=new EditorValue(text.isEmpty()&&spec.nullable()?EditorValue.Kind.NULL:spec.kind(),text);spec.validate(v);if(!v.equals(before))changes.put(spec.id(),v);}
                if(!changes.isEmpty())fields.put(type,changes);
            }
            var t=o.transform();var transform=new EditorTransform(new Vec3(value("x",t.position().x),value("y",t.position().y),value("z",t.position().z)),(float)value("yaw",t.yaw()),(float)value("pitch",t.pitch()));
            EditorClient.operation(new EditorOperation.CommitDraft(o.id(),o.revision(),draft.getOrDefault("name",o.name()),transform,fields,logic==null?null:logic.value(this)));
        }else if(EditorClient.scene()!=null){
            var scene=EditorClient.scene();var group=EditorClient.selected()==null?null:scene.groups().get(EditorClient.selected());
            if(dirtyDraft&&group!=null)EditorClient.operation(new EditorOperation.RenameGroup(group.id(),group.revision(),draft.getOrDefault("name",group.name())));
            else if(dirtyDraft&&EditorClient.selected()==null)EditorClient.operation(new EditorOperation.RenameScene(draft.getOrDefault("scene_name",scene.name())));
            else EditorClient.request(EditorPayloads.Intent.SAVE,null,"",null);
        }
        }catch(RuntimeException failure){invalid();}
    }
    @Override public void picker(String title,List<LogicInspector.Choice> choices){movementKeys.clear();looking=false;pickerTitle=title;pickerChoices=List.copyOf(choices);pickerSearch="";pickerScroll=0;buildPicker();}
    private void buildPicker(){clearWidgets();int w=Math.min(360,width-24),x=(width-w)/2,y=TOP+12;
        var box=new EditBox(font,x+10,y+24,w-20,18,key("search"));box.setMaxLength(128);box.setValue(pickerSearch);box.setHint(key("search"));
        box.setResponder(v->{pickerSearch=v;pickerScroll=0;pickerRows(x,y,w);});addRenderableWidget(box);setFocused(box);
        button(x+w-28,y,18,18,Component.literal("x"),()->{pickerChoices=null;init();});pickerRows(x,y,w);
    }
    private void pickerRows(int x,int y,int w){
        for(var c:List.copyOf(children()))if(c instanceof AbstractWidget a&&a.getY()>=y+48)removeWidget(a);
        var list=pickerChoices.stream().filter(c->c.label().toLowerCase(Locale.ROOT).contains(pickerSearch.toLowerCase(Locale.ROOT))).toList();
        int visible=Math.max(1,(height-BOTTOM-y-52)/23);pickerScroll=Math.clamp(pickerScroll,0,Math.max(0,list.size()-visible));
        for(int i=pickerScroll;i<Math.min(list.size(),pickerScroll+visible);i++){var entry=list.get(i);button(x+10,y+48+(i-pickerScroll)*23,w-20,20,Component.literal(entry.label()),()->{pickerChoices=null;entry.action().run();if(pickerChoices==null)init();});}
    }
    private void saveLayout(){EditorClientConfig.LEFT.set(leftRatio);EditorClientConfig.RIGHT.set(rightRatio);EditorClientConfig.SPEC.save();}
    private void updateStatus(){var s=EditorClient.scene();var o=selectedObject();
        footer=tr("editor_mode")+" | "+(s==null?"—":s.name())+" | "+(o==null?tr("none"):o.name())+" | "+tr("objects")+": "+(s==null?0:s.objects().size())+" | "+tr(dirtyDraft?"unsaved_draft":"saved")+" | "+tr("validation")+": "+(s==null?0:s.issues().size());
        footerError=EditorClient.pending()?tr("pending"):!EditorClient.error.isEmpty()?Component.translatable(EditorClient.error).getString():tr("controls");
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float tick){
        if(central(mouseX,mouseY)&&!looking&&dragAxis<0&&splitter==0&&pickerChoices==null)EditorWorldRenderer.hover(mouseX,mouseY);else EditorWorldRenderer.clearHover();
        g.fill(0,0,width,TOP,0xF0101821);g.fill(0,TOP,left,height-BOTTOM,0xEA111D29);g.fill(right,TOP,width,height-BOTTOM,0xEA111D29);g.fill(0,height-BOTTOM,width,height,0xF0101821);
        for(int x:List.of(left,right))g.fill(x-2,TOP,x+2,height-BOTTOM,splitter!=0||Math.abs(mouseX-x)<4?0xFF659BC5:0xFF3E4C5E);
        for(var label:labels)g.drawString(font,font.plainSubstrByWidth(label.text(),label.x()>=right?width-right-12:label.y()>=TOP?left-12:120),label.x(),label.y(),label.color(),false);
        gizmo(g,mouseX,mouseY);
        if(pickerChoices!=null){int w=Math.min(360,width-24),x=(width-w)/2,y=TOP+12;g.fill(x,y,x+w,height-BOTTOM,0xFC101A26);g.renderOutline(x,y,w,height-BOTTOM-y,0xFF5988AF);g.drawString(font,font.plainSubstrByWidth(pickerTitle,w-50),x+10,y+6,0xFFE1E9F3,false);}
        super.render(g,mouseX,mouseY,tick);
        g.drawString(font,font.plainSubstrByWidth(footer,width-12),6,height-28,0xFF87CCBB,false);
        g.drawString(font,font.plainSubstrByWidth(footerError,width-12),6,height-14,EditorClient.error.isEmpty()?0xFF96A5B7:0xFFFF8C7C,false);
    }
    private Vec3 axis(int index){return switch(index){case 0->new Vec3(1,0,0);case 1->new Vec3(0,1,0);default->new Vec3(0,0,1);};}
    private MaplesObject selectedObject(){var scene=EditorClient.scene();var selected=EditorClient.selected();return scene==null||selected==null?null:scene.objects().get(selected);}
    private void gizmo(GuiGraphics g,double mouseX,double mouseY){
        var object=selectedObject();if(object==null||selectTool||pickerChoices!=null)return;Vec3 p=(preview==null?object.transform():preview).position();double size=EditorWorldRenderer.handleSize(p);var center=EditorWorldRenderer.project(p);if(center==null)return;
        g.enableScissor(left,TOP,right,height-BOTTOM);
        if(rotate){for(int i=0;i<64;i++){double a=i*Math.PI/32,b=(i+1)*Math.PI/32;drawLine(g,EditorWorldRenderer.project(p.add(Math.cos(a)*size,0,Math.sin(a)*size)),EditorWorldRenderer.project(p.add(Math.cos(b)*size,0,Math.sin(b)*size)),dragAxis==3?0xFFFFFF80:0xFF80EF90);}}
        else for(int i=0;i<3;i++){var end=EditorWorldRenderer.project(p.add(axis(i).scale(size)));int color=i==0?0xFFFF6969:i==1?0xFF69E78C:0xFF70A8FF;
            if(dragAxis==i||(end!=null&&EditorGeometry.segmentDistance(mouseX,mouseY,center,end)<7))color=0xFFFFFF80;
            drawLine(g,center,end,color);if(end!=null)g.drawString(font,"XYZ".substring(i,i+1),(int)end.x()+2,(int)end.y()-5,color);
        }g.disableScissor();
    }
    private static void drawLine(GuiGraphics g,EditorGeometry.Point a,EditorGeometry.Point b,int color){if(a==null||b==null)return;int steps=(int)Math.min(2000,Math.ceil(Math.hypot(a.x()-b.x(),a.y()-b.y())));for(int i=0;i<=steps;i++){double t=steps==0?0:(double)i/steps;int x=(int)(a.x()+(b.x()-a.x())*t),y=(int)(a.y()+(b.y()-a.y())*t);g.fill(x,y,x+2,y+2,color);}}
    private boolean central(double x,double y){return x>left&&x<right&&y>TOP&&y<height-BOTTOM;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(pickerChoices!=null){super.mouseClicked(x,y,button);return true;}
        if(button==0&&y>TOP&&y<height-BOTTOM&&(Math.abs(x-left)<4||Math.abs(x-right)<4)){splitter=Math.abs(x-left)<4?1:2;setFocused(null);return true;}
        if(super.mouseClicked(x,y,button))return true;if(!central(x,y))return true;
        setFocused(null);if(button==1){looking=true;return true;}if(button!=0||EditorClient.pending())return true;
        var object=selectedObject();var ray=EditorWorldRenderer.ray(x,y);
        if(!selectTool&&object!=null&&ray!=null){var pivot=object.transform().position();double size=EditorWorldRenderer.handleSize(pivot);var center=EditorWorldRenderer.project(pivot);int hit=-1;
            if(rotate){for(int i=0;i<64;i++){double a=i*Math.PI/32,b=(i+1)*Math.PI/32;var aa=EditorWorldRenderer.project(pivot.add(Math.cos(a)*size,0,Math.sin(a)*size));var bb=EditorWorldRenderer.project(pivot.add(Math.cos(b)*size,0,Math.sin(b)*size));if(aa!=null&&bb!=null&&EditorGeometry.segmentDistance(x,y,aa,bb)<7){hit=3;break;}}}
            else if(center!=null)for(int i=0;i<3;i++){var end=EditorWorldRenderer.project(pivot.add(axis(i).scale(size)));if(end!=null&&EditorGeometry.segmentDistance(x,y,center,end)<7){hit=i;break;}}
            if(hit>=0){double start=hit==3?EditorGeometry.yawAngle(ray,pivot):EditorGeometry.axisParameter(ray,pivot,axis(hit));
                if(Double.isFinite(start)){dragAxis=hit;dragStart=start;dragId=object.id();dragRevision=object.revision();dragOriginal=object.transform();preview=dragOriginal;return true;}
                EditorClient.error="editor.maplesadventure.axis_parallel";return true;
            }
        }inspectorScroll=0;EditorClient.select(EditorWorldRenderer.pick(x,y));return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(splitter!=0&&button==0){layout=splitter==1?layout.dragLeft(x):layout.dragRight(x);leftRatio=layout.leftRatio();rightRatio=layout.rightRatio();init();return true;}
        if(looking&&button==1&&minecraft.player!=null){minecraft.player.turn(dx*2,dy*2);return true;}
        if(dragAxis>=0&&button==0){var ray=EditorWorldRenderer.ray(x,y);if(ray==null)return true;double value=dragAxis==3?EditorGeometry.yawAngle(ray,dragOriginal.position()):EditorGeometry.axisParameter(ray,dragOriginal.position(),axis(dragAxis));
            if(Double.isFinite(value))try{preview=dragAxis==3?new EditorTransform(dragOriginal.position(),EditorGeometry.yaw(dragOriginal.yaw()-(float)(value-dragStart)),dragOriginal.pitch()):new EditorTransform(dragOriginal.position().add(axis(dragAxis).scale(value-dragStart)),dragOriginal.yaw(),dragOriginal.pitch());}catch(IllegalArgumentException ignored){}
            return true;
        }return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==0&&splitter!=0){splitter=0;saveLayout();return true;}
        if(button==1)looking=false;
        if(button==0&&dragAxis>=0){var transform=preview;var original=dragOriginal;var id=dragId;long revision=dragRevision;cancelDrag();if(transform!=null&&!transform.equals(original))EditorClient.operation(new EditorOperation.SetTransform(id,revision,transform));return true;}
        return super.mouseReleased(x,y,button);
    }
    private void cancelDrag(){dragAxis=-1;dragId=null;preview=null;dragOriginal=null;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(pickerChoices!=null){pickerScroll=Math.max(0,pickerScroll-(int)Math.round(vertical*3));buildPicker();return true;}
        if(x<left){hierarchyScroll=Math.max(0,hierarchyScroll-(int)Math.round(vertical*3));init();return true;}
        if(x>right){inspectorScroll=Math.clamp(inspectorScroll-(int)Math.round(vertical*36),0,Math.max(0,row-height+60));init();return true;}return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_ESCAPE){if(pickerChoices!=null){pickerChoices=null;init();return true;}if(dragAxis>=0){cancelDrag();return true;}onClose();return true;}
        if((!(getFocused() instanceof EditBox)||key>=GLFW.GLFW_KEY_F1)&&AdventureKeyMappings.EDITOR.matches(key,scan)){onClose();return true;}
        if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&getFocused() instanceof EditBox box&&!EditorClient.pending()){
            if(pickerChoices==null&&selectedObject()!=null&&dirtyDraft){saveDrafts();return true;}
            var action=enterActions.get(box);if(action!=null){action.run();return true;}
        }
        if(pickerChoices!=null)return super.keyPressed(key,scan,modifiers);
        if(key==GLFW.GLFW_KEY_F&&!(getFocused() instanceof EditBox)&&selectedObject()!=null&&minecraft.player!=null){
            var d=selectedObject().transform().position().subtract(minecraft.player.getEyePosition());minecraft.player.setYRot((float)Math.toDegrees(Math.atan2(-d.x,d.z)));minecraft.player.setXRot((float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));return true;
        }
        if(!(getFocused() instanceof EditBox)&&navigationKey(key,scan)){movementKeys.add(key);return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){movementKeys.remove(key);return super.keyReleased(key,scan,modifiers);}
    public void movement(Input input){
        if(!minecraft.isWindowActive()){looking=false;splitter=0;movementKeys.clear();cancelDrag();}
        if(!navigationAllowed())movementKeys.clear();
        // Travel consumes editor key state directly. Do not also accelerate through vanilla flying.
        input.up=false;input.down=false;input.left=false;input.right=false;
        input.forwardImpulse=0;input.leftImpulse=0;input.jumping=false;input.shiftKeyDown=false;
    }
    private boolean navigationAllowed(){return EditorNavigation.acceptsMovement(EditorClient.active(),minecraft.isWindowActive(),getFocused() instanceof EditBox,pickerChoices!=null,splitter!=0||dragAxis>=0);}
    private boolean navigationKey(int key,int scan){var o=minecraft.options;return java.util.stream.Stream.of(o.keyUp,o.keyDown,o.keyLeft,o.keyRight,o.keyJump,o.keyShift).anyMatch(k->k.matches(key,scan));}
    public void travel(net.minecraft.world.entity.player.Player player){
        if(!navigationAllowed())movementKeys.clear();var o=minecraft.options;
        Vec3 motion=EditorNavigation.motion(player.getYRot(),(pressed(o.keyLeft)?1:0)-(pressed(o.keyRight)?1:0),
                (pressed(o.keyUp)?1:0)-(pressed(o.keyDown)?1:0),(pressed(o.keyJump)?1:0)-(pressed(o.keyShift)?1:0));
        player.setDeltaMovement(Vec3.ZERO);player.setSprinting(false);player.resetFallDistance();
        player.move(net.minecraft.world.entity.MoverType.SELF,motion);
        player.setDeltaMovement(Vec3.ZERO);
    }
    private boolean pressed(net.minecraft.client.KeyMapping key){return movementKeys.stream().anyMatch(code->key.matches(code,0));}
    @Override public void onClose(){EditorClient.close();minecraft.setScreen(null);}
    @Override public void removed(){saveLayout();looking=false;movementKeys.clear();cancelDrag();if(minecraft.player!=null)minecraft.player.setDeltaMovement(Vec3.ZERO);if(EditorClient.active())EditorClient.close();super.removed();}
}
