package dev.maplesadventure.client.editor;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.*;
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

/** Functional authoring UI; no world blur, free camera, gameplay stats or direct server-state writes. */
public final class EditorScreen extends Screen {
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
    public void authoritativeUpdate(){cancelDrag();draft.clear();dirtyDraft=false;deleteConfirm=false;if(minecraft!=null)init();}
    @Override protected void init(){
        rebuilding=true;clearWidgets();labels.clear();formFields.clear();enterActions.clear();left=Math.clamp(width/4,90,210);right=width-Math.clamp(width/3,105,270);
        int toolWidth=Math.max(24,(width-left-64)/3);
        button(5,5,left-10,18,Component.literal(EditorClient.scene()==null?tr("select_scene"):EditorClient.scene().name()),()->{
            var ids=new ArrayList<>(EditorClient.scenes.keySet());if(!ids.isEmpty()){createScene=false;inspectorScroll=0;int index=EditorClient.scene()==null?-1:ids.indexOf(EditorClient.scene().id());EditorClient.request(EditorPayloads.Intent.SELECT_SCENE,ids.get((index+1)%ids.size()),"",null);}
        });
        button(left+4,5,toolWidth-3,18,key("new_scene"),()->{createScene=!createScene;draft.clear();inspectorScroll=0;init();});
        button(left+4+toolWidth,5,toolWidth-3,18,key(rotate?"rotate":"move"),()->{rotate=!rotate;cancelDrag();init();});
        button(left+4+toolWidth*2,5,toolWidth-3,18,key("validate"),()->EditorClient.request(EditorPayloads.Intent.VALIDATE,null,"",null));
        button(width-55,5,50,18,key("exit"),this::onClose);
        if(EditorClient.scene()!=null){
            button(5,29,(left-15)/2,18,key("empty"),()->create(false));
            button(10+(left-15)/2,29,(left-15)/2,18,key("marker"),()->create(true));
            button(5,50,left-10,18,key("new_group"),()->EditorClient.operation(new EditorOperation.CreateGroup(tr("group"),selectedGroup())));
            List<Object> entries=new ArrayList<>();hierarchy(null,entries,0);int visible=Math.max(1,(height-100)/20);hierarchyScroll=Math.clamp(hierarchyScroll,0,Math.max(0,entries.size()-visible));
            for(int i=hierarchyScroll;i<Math.min(entries.size(),hierarchyScroll+visible);i++){
                var entry=entries.get(i);UUID id;String name;UUID parent;
                if(entry instanceof EditorGroup g){id=g.id();name=(collapsed.contains(id)?"+ ":"− ")+g.name();parent=g.parent();}
                else{var o=(MaplesObject)entry;id=o.id();name=o.name();parent=o.group();}
                int indent=Math.min(40,depth(parent)*8),y=74+(i-hierarchyScroll)*20;
                var b=button(5+indent,y,left-10-indent,18,Component.literal((id.equals(EditorClient.selected())?"> ":"")+name),()->{createScene=false;inspectorScroll=0;EditorClient.select(id);});
                b.setTooltip(Tooltip.create(Component.literal(name+"\n"+id)));
            }
        }else labels.add(new Label(8,35,tr("create_hint"),0xFFB8BEC8));
        row=35;buildInspector();
        rebuilding=false;
    }
    private void hierarchy(UUID parent,List<Object> result,int depth){if(depth>EditorLimits.DEPTH)return;var scene=EditorClient.scene();
        scene.groups().values().stream().filter(g->Objects.equals(parent,g.parent())).sorted(Comparator.comparing(EditorGroup::name).thenComparing(EditorGroup::id)).forEach(g->{result.add(g);if(!collapsed.contains(g.id()))hierarchy(g.id(),result,depth+1);});
        scene.objects().values().stream().filter(o->Objects.equals(parent,o.group())).sorted(Comparator.comparing(MaplesObject::name).thenComparing(MaplesObject::id)).forEach(result::add);
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
        var t=o.transform();label(tr("transform"));
        field("x","X",Double.toString(t.position().x),64);field("y","Y",Double.toString(t.position().y),64);field("z","Z",Double.toString(t.position().z),64);
        field("yaw","Yaw",Float.toString(t.yaw()),64);field("pitch","Pitch",Float.toString(t.pitch()),64);
        action(tr("apply_transform"),()->{try{EditorClient.operation(new EditorOperation.SetTransform(o.id(),o.revision(),new EditorTransform(new Vec3(number("x"),number("y"),number("z")),(float)number("yaw"),(float)number("pitch"))));}catch(RuntimeException failure){invalid();}});
        parent(o.group(),id->EditorClient.operation(new EditorOperation.MoveObject(o.id(),o.revision(),id)),null);
        action(tr("duplicate"),()->EditorClient.operation(new EditorOperation.DuplicateObject(o.id(),o.revision())));
        deletion(o.id(),()->EditorClient.operation(new EditorOperation.DeleteObject(o.id(),o.revision())));
        for(var component:o.components().values().stream().sorted(Comparator.comparing(c->c.type().toString())).toList()){
            var schema=EditorClient.schemas.get(component.type());var values=EditorClient.fields(o.id(),component.type());
            label(schema==null?component.type().toString():Component.translatable(schema.label()).getString());
            if(schema!=null&&!values.isEmpty()){
                for(var spec:schema.fields()){
                    var value=values.get(spec.id());if(value==null)continue;String key=component.type()+"/"+spec.id();
                    var box=field(key,Component.translatable(spec.label()).getString(),value.text(),spec.maxLength(),spec.readOnly());
                    if(box!=null)box.setTooltip(Tooltip.create(fieldHint(spec)));
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
        label(tr("add_component"));for(var schema:EditorClient.schemas.values())if(!o.components().containsKey(schema.id()))action(Component.translatable(schema.label()).getString(),()->EditorClient.operation(new EditorOperation.AddComponent(o.id(),o.revision(),schema.id())));
        issues(o.id());
    }
    private void issues(UUID object){var scene=EditorClient.scene();if(scene.readOnly())label(tr("read_only"));
        for(var issue:scene.issues())if(issue.object()==null||Objects.equals(issue.object(),object))label(issue.severity()+": "+Component.translatable(issue.message()).getString());}
    private void parent(UUID current,java.util.function.Consumer<UUID> change,UUID excluded){
        var scene=EditorClient.scene();String name=current==null?tr("root"):scene.groups().containsKey(current)?scene.groups().get(current).name():"?";
        action(tr("group")+": "+name,()->{List<UUID> ids=new ArrayList<>();ids.add(null);scene.groups().values().stream().sorted(Comparator.comparing(EditorGroup::name)).filter(g->!g.id().equals(excluded)).forEach(g->ids.add(g.id()));change.accept(ids.get((ids.indexOf(current)+1)%ids.size()));});
    }
    private void deletion(UUID id,Runnable delete){action(tr(deleteConfirm&&Objects.equals(deleting,id)?"confirm_delete":"delete"),()->{if(deleteConfirm&&Objects.equals(deleting,id))delete.run();else{deleting=id;deleteConfirm=true;init();}});}
    private void label(String text){int y=row-inspectorScroll;row+=16;if(y>=32&&y<height-30)labels.add(new Label(right+6,y,text,0xFFE0E5EC));}
    private void field(String id,String name,String fallback,int max){
        field(id,name,fallback,max,false);
    }
    private EditBox field(String id,String name,String fallback,int max,boolean readOnly){
        draft.putIfAbsent(id,fallback);int y=row-inspectorScroll;row+=36;if(y<32||y+32>height-30)return null;
        labels.add(new Label(right+6,y,name,0xFFAEBACA));var box=new EditBox(font,right+6,y+12,width-right-12,18,Component.literal(name));box.setMaxLength(max);box.setValue(draft.get(id));
        box.setResponder(value->{if(!rebuilding){draft.put(id,value);dirtyDraft=true;}});box.setEditable(!readOnly&&!EditorClient.pending()&&!EditorClient.storeReadOnly&&(EditorClient.scene()==null||!EditorClient.scene().readOnly()));
        addRenderableWidget(box);
        if(!readOnly)formFields.add(box);
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
    private void action(String label,Runnable action){for(var box:formFields)enterActions.put(box,action);formFields.clear();int y=row-inspectorScroll;row+=23;if(y>=32&&y+20<height-30)button(right+6,y,width-right-12,20,Component.literal(label),action);}
    private Button button(int x,int y,int w,int h,Component label,Runnable action){var b=Button.builder(label,ignored->action.run()).bounds(x,y,Math.max(12,w),h).build();b.active=!EditorClient.pending();return addRenderableWidget(b);}
    private double number(String id){double value=Double.parseDouble(draft.get(id));if(!Double.isFinite(value))throw new IllegalArgumentException();return value;}
    private void invalid(){EditorClient.error="editor.maplesadventure.invalid_field";}
    private static Component key(String key){return Component.translatable("editor.maplesadventure."+key);}
    private static String tr(String key){return key(key).getString();}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float tick){
        if(central(mouseX,mouseY)&&!looking&&dragAxis<0)EditorWorldRenderer.hover(mouseX,mouseY);else EditorWorldRenderer.clearHover();
        g.fill(0,0,width,27,0xEE121820);g.fill(0,27,left,height-25,0xE51B2330);g.fill(right,27,width,height-25,0xE51B2330);g.fill(0,height-25,width,height,0xEE121820);
        for(var label:labels)g.drawString(font,font.plainSubstrByWidth(label.text(),label.x()>=right?width-right-12:left-12),label.x(),label.y(),label.color());
        gizmo(g,mouseX,mouseY);
        super.render(g,mouseX,mouseY,tick);
        String footer=EditorClient.pending()?tr("pending"):!EditorClient.error.isEmpty()?Component.translatable(EditorClient.error).getString():tr("controls");
        g.drawString(font,font.plainSubstrByWidth(footer,width-12),6,height-16,EditorClient.error.isEmpty()?0xFFD2DBE6:0xFFFF8C7C);
    }
    private Vec3 axis(int index){return switch(index){case 0->new Vec3(1,0,0);case 1->new Vec3(0,1,0);default->new Vec3(0,0,1);};}
    private MaplesObject selectedObject(){var scene=EditorClient.scene();var selected=EditorClient.selected();return scene==null||selected==null?null:scene.objects().get(selected);}
    private void gizmo(GuiGraphics g,double mouseX,double mouseY){
        var object=selectedObject();if(object==null)return;Vec3 p=(preview==null?object.transform():preview).position();double size=EditorWorldRenderer.handleSize(p);var center=EditorWorldRenderer.project(p);if(center==null)return;
        g.enableScissor(left,27,right,height-25);
        if(rotate){for(int i=0;i<64;i++){double a=i*Math.PI/32,b=(i+1)*Math.PI/32;drawLine(g,EditorWorldRenderer.project(p.add(Math.cos(a)*size,0,Math.sin(a)*size)),EditorWorldRenderer.project(p.add(Math.cos(b)*size,0,Math.sin(b)*size)),dragAxis==3?0xFFFFFF80:0xFF80EF90);}}
        else for(int i=0;i<3;i++){var end=EditorWorldRenderer.project(p.add(axis(i).scale(size)));int color=i==0?0xFFFF6969:i==1?0xFF69E78C:0xFF70A8FF;
            if(dragAxis==i||(end!=null&&EditorGeometry.segmentDistance(mouseX,mouseY,center,end)<7))color=0xFFFFFF80;
            drawLine(g,center,end,color);if(end!=null)g.drawString(font,"XYZ".substring(i,i+1),(int)end.x()+2,(int)end.y()-5,color);
        }g.disableScissor();
    }
    private static void drawLine(GuiGraphics g,EditorGeometry.Point a,EditorGeometry.Point b,int color){if(a==null||b==null)return;int steps=(int)Math.min(2000,Math.ceil(Math.hypot(a.x()-b.x(),a.y()-b.y())));for(int i=0;i<=steps;i++){double t=steps==0?0:(double)i/steps;int x=(int)(a.x()+(b.x()-a.x())*t),y=(int)(a.y()+(b.y()-a.y())*t);g.fill(x,y,x+2,y+2,color);}}
    private boolean central(double x,double y){return x>left&&x<right&&y>27&&y<height-25;}
    @Override public boolean mouseClicked(double x,double y,int button){
        if(super.mouseClicked(x,y,button))return true;if(!central(x,y))return true;
        setFocused(null);if(button==1){looking=true;return true;}if(button!=0||EditorClient.pending())return true;
        var object=selectedObject();var ray=EditorWorldRenderer.ray(x,y);
        if(object!=null&&ray!=null){var pivot=object.transform().position();double size=EditorWorldRenderer.handleSize(pivot);var center=EditorWorldRenderer.project(pivot);int hit=-1;
            if(rotate){for(int i=0;i<64;i++){double a=i*Math.PI/32,b=(i+1)*Math.PI/32;var aa=EditorWorldRenderer.project(pivot.add(Math.cos(a)*size,0,Math.sin(a)*size));var bb=EditorWorldRenderer.project(pivot.add(Math.cos(b)*size,0,Math.sin(b)*size));if(aa!=null&&bb!=null&&EditorGeometry.segmentDistance(x,y,aa,bb)<7){hit=3;break;}}}
            else if(center!=null)for(int i=0;i<3;i++){var end=EditorWorldRenderer.project(pivot.add(axis(i).scale(size)));if(end!=null&&EditorGeometry.segmentDistance(x,y,center,end)<7){hit=i;break;}}
            if(hit>=0){double start=hit==3?EditorGeometry.yawAngle(ray,pivot):EditorGeometry.axisParameter(ray,pivot,axis(hit));
                if(Double.isFinite(start)){dragAxis=hit;dragStart=start;dragId=object.id();dragRevision=object.revision();dragOriginal=object.transform();preview=dragOriginal;return true;}
                EditorClient.error="editor.maplesadventure.axis_parallel";return true;
            }
        }inspectorScroll=0;EditorClient.select(EditorWorldRenderer.pick(x,y));return true;
    }
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){
        if(looking&&button==1&&minecraft.player!=null){minecraft.player.turn(dx*2,dy*2);return true;}
        if(dragAxis>=0&&button==0){var ray=EditorWorldRenderer.ray(x,y);if(ray==null)return true;double value=dragAxis==3?EditorGeometry.yawAngle(ray,dragOriginal.position()):EditorGeometry.axisParameter(ray,dragOriginal.position(),axis(dragAxis));
            if(Double.isFinite(value))try{preview=dragAxis==3?new EditorTransform(dragOriginal.position(),EditorGeometry.yaw(dragOriginal.yaw()-(float)(value-dragStart)),dragOriginal.pitch()):new EditorTransform(dragOriginal.position().add(axis(dragAxis).scale(value-dragStart)),dragOriginal.yaw(),dragOriginal.pitch());}catch(IllegalArgumentException ignored){}
            return true;
        }return super.mouseDragged(x,y,button,dx,dy);
    }
    @Override public boolean mouseReleased(double x,double y,int button){
        if(button==1){looking=false;movementKeys.clear();}
        if(button==0&&dragAxis>=0){var transform=preview;var original=dragOriginal;var id=dragId;long revision=dragRevision;cancelDrag();if(transform!=null&&!transform.equals(original))EditorClient.operation(new EditorOperation.SetTransform(id,revision,transform));return true;}
        return super.mouseReleased(x,y,button);
    }
    private void cancelDrag(){dragAxis=-1;dragId=null;preview=null;dragOriginal=null;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){
        if(x<left){hierarchyScroll=Math.max(0,hierarchyScroll-(int)Math.round(vertical*3));init();return true;}
        if(x>right){inspectorScroll=Math.clamp(inspectorScroll-(int)Math.round(vertical*36),0,Math.max(0,row-height+60));init();return true;}return true;
    }
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(key==GLFW.GLFW_KEY_ESCAPE){if(dragAxis>=0){cancelDrag();return true;}onClose();return true;}
        if((!(getFocused() instanceof EditBox)||key>=GLFW.GLFW_KEY_F1)&&AdventureKeyMappings.EDITOR.matches(key,scan)){onClose();return true;}
        if((key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER)&&getFocused() instanceof EditBox box&&!EditorClient.pending()){
            var action=enterActions.get(box);if(action!=null){action.run();return true;}
        }
        if(looking){movementKeys.add(key);return true;}return super.keyPressed(key,scan,modifiers);
    }
    @Override public boolean keyReleased(int key,int scan,int modifiers){movementKeys.remove(key);return super.keyReleased(key,scan,modifiers);}
    public void movement(Input input){
        if(!minecraft.isWindowActive()){looking=false;movementKeys.clear();cancelDrag();}
        boolean move=looking&&minecraft.isWindowActive();if(!move)movementKeys.clear();var options=minecraft.options;
        input.up=move&&pressed(options.keyUp);input.down=move&&pressed(options.keyDown);input.left=move&&pressed(options.keyLeft);input.right=move&&pressed(options.keyRight);
        input.forwardImpulse=(input.up?1:0)-(input.down?1:0);input.leftImpulse=(input.left?1:0)-(input.right?1:0);
        input.jumping=move&&pressed(options.keyJump);input.shiftKeyDown=move&&pressed(options.keyShift);
    }
    private boolean pressed(net.minecraft.client.KeyMapping key){return movementKeys.stream().anyMatch(code->key.matches(code,0));}
    @Override public void onClose(){EditorClient.close();minecraft.setScreen(null);}
    @Override public void removed(){looking=false;movementKeys.clear();cancelDrag();if(EditorClient.active())EditorClient.close();super.removed();}
}
