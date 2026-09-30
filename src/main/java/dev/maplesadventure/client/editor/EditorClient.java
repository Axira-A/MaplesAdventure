package dev.maplesadventure.client.editor;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.operation.EditorOperation;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import dev.maplesadventure.editor.EditorFoundation;
import dev.maplesadventure.editor.network.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

/** Local drafts/selection only. All accepted scene data arrives through server packets. */
public final class EditorClient {
    public record Schema(ResourceLocation id,String label,List<InspectorField> fields){}
    public record LogicSchema(Schema schema,dev.maplesadventure.authoring.logic.LogicDefinition defaults){}
    public static final Map<dev.maplesadventure.authoring.logic.LogicTypeRegistry.Kind,Map<ResourceLocation,LogicSchema>> logicSchemas=new EnumMap<>(dev.maplesadventure.authoring.logic.LogicTypeRegistry.Kind.class);
    private static UUID pendingOpen,session,pending;
    private static long openedAt,requestedAt;
    private static final EditorPageAssembler transfers=new EditorPageAssembler();
    public static final Map<ResourceLocation,String> scenes=new LinkedHashMap<>();
    public static final Map<ResourceLocation,Schema> schemas=new LinkedHashMap<>();
    private static CompoundTag sceneTag;
    private static MaplesScene scene;
    private static final Map<UUID,Map<ResourceLocation,Map<String,EditorValue>>> fieldCache=new HashMap<>();
    private static UUID selection;
    public static String error="";
    public static boolean storeReadOnly;
    public static boolean active(){return session!=null;}
    public static boolean pending(){return pending!=null;}
    public static boolean travel(net.minecraft.world.entity.player.Player player){
        var mc=Minecraft.getInstance();
        if(!active()||player!=mc.player||!player.isSpectator()||!(mc.screen instanceof EditorScreen screen))return false;
        screen.travel(player);return true;
    }
    public static MaplesScene scene(){return scene;}
    public static UUID selected(){return selection;}
    public static void select(UUID id){selection=id;refresh();}
    public static void register(){
        NeoForge.EVENT_BUS.addListener(EditorWorldRenderer::render);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e)->clear());
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e)->{
            if(pendingOpen!=null&&System.nanoTime()-openedAt>10_000_000_000L){pendingOpen=null;error="editor.maplesadventure.timeout";}
            if(pending!=null&&System.nanoTime()-requestedAt>10_000_000_000L){pending=null;error="editor.maplesadventure.timeout";refresh();}
            if(active()&&!(Minecraft.getInstance().screen instanceof EditorScreen))close();
        });
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.MovementInputUpdateEvent e)->{
            if(Minecraft.getInstance().screen instanceof EditorScreen screen)screen.movement(e.getInput());
        });
    }
    public static void open(){if(pendingOpen!=null||active())return;pendingOpen=UUID.randomUUID();openedAt=System.nanoTime();
        PacketDistributor.sendToServer(new EditorPayloads.Request(pendingOpen,null,EditorPayloads.Intent.OPEN,null,"",0,null));}
    public static void close(){UUID old=session;clear();if(old!=null&&Minecraft.getInstance().getConnection()!=null)
        PacketDistributor.sendToServer(new EditorPayloads.Request(UUID.randomUUID(),old,EditorPayloads.Intent.CLOSE,null,"",0,null));}
    public static void clear(){pendingOpen=null;session=null;pending=null;scene=null;sceneTag=null;selection=null;scenes.clear();schemas.clear();logicSchemas.clear();fieldCache.clear();transfers.clear();error="";storeReadOnly=false;EditorWorldRenderer.clear();}
    public static void request(EditorPayloads.Intent intent,ResourceLocation id,String name,EditorOperation op){
        if(!active()||pending())return;pending=UUID.randomUUID();requestedAt=System.nanoTime();error="";
        PacketDistributor.sendToServer(new EditorPayloads.Request(pending,session,intent,id,name,scene==null?0:scene.revision(),op));refresh();
    }
    public static void operation(EditorOperation op){if(scene!=null&&!scene.readOnly())request(EditorPayloads.Intent.OPERATION,scene.id(),"",op);}
    public static Map<String,EditorValue> fields(UUID object,ResourceLocation component){
        return fieldCache.getOrDefault(object,Map.of()).getOrDefault(component,Map.of());
    }
    public static void receive(EditorPayloads.Page page){
        if(page.kind()==EditorPayloads.Kind.SESSION){if(pendingOpen==null)return;}
        else if(!Objects.equals(page.session(),session)&&!Objects.equals(page.session(),pendingOpen))return;
        try{
            var bytes=transfers.accept(page);if(bytes==null)return;
            var tag=SceneSerialization.read(bytes,EditorLimits.SCENE_BYTES*2);accept(page.kind(),page.session(),tag);
        }catch(RuntimeException failure){MaplesAdventure.LOGGER.warn("Editor snapshot rejected",failure);error="editor.maplesadventure.invalid_snapshot";transfers.clear();pending=null;refresh();}
    }
    private static void accept(EditorPayloads.Kind kind,UUID nonce,CompoundTag tag){switch(kind){
        case SESSION->{
            if(!tag.hasUUID("Request")||!Objects.equals(tag.getUUID("Request"),pendingOpen))return;
            session=nonce;pendingOpen=null;
            if(Minecraft.getInstance().screen!=null){close();return;}
            Minecraft.getInstance().setScreen(new EditorScreen());
        }
        case CLOSED->{close();if(Minecraft.getInstance().screen instanceof EditorScreen)Minecraft.getInstance().setScreen(null);}
        case CATALOG->{scenes.clear();storeReadOnly=tag.getBoolean("ReadOnly");var list=tag.getList("Scenes",Tag.TAG_COMPOUND);for(int i=0;i<list.size();i++){var t=list.getCompound(i);scenes.put(ResourceLocation.parse(t.getString("Id")),t.getString("Name"));}refresh();}
        case SCHEMA->{schemas.clear();var types=tag.getList("Types",Tag.TAG_COMPOUND);if(types.size()>1024)throw new IllegalArgumentException("Schema limit");
            for(int i=0;i<types.size();i++){var t=types.getCompound(i);var fields=new ArrayList<InspectorField>();var fl=t.getList("Fields",Tag.TAG_COMPOUND);if(fl.size()>64)throw new IllegalArgumentException("Fields limit");for(int j=0;j<fl.size();j++)fields.add(EditorViews.field(fl.getCompound(j)));var id=ResourceLocation.parse(t.getString("Id"));schemas.put(id,new Schema(id,t.getString("Label"),List.copyOf(fields)));}
            logicSchemas.clear();for(var k:dev.maplesadventure.authoring.logic.LogicTypeRegistry.Kind.values()){
                var entries=tag.getList("Logic"+k.name(),Tag.TAG_COMPOUND);if(entries.size()>256)throw new IllegalArgumentException("Logic schema limit");var map=new LinkedHashMap<ResourceLocation,LogicSchema>();
                for(var e:entries){var t=(CompoundTag)e;var fields=new ArrayList<InspectorField>();for(var f:t.getList("Fields",Tag.TAG_COMPOUND))fields.add(EditorViews.field((CompoundTag)f));
                    var id=ResourceLocation.parse(t.getString("Id"));map.put(id,new LogicSchema(new Schema(id,t.getString("Label"),List.copyOf(fields)),dev.maplesadventure.authoring.logic.LogicComponent.definition(t.getCompound("Defaults"))));}
                logicSchemas.put(k,Map.copyOf(map));
            }refresh();}
        case SNAPSHOT->{sceneTag=tag;readScene();refresh();}
        case DELTA->{
            if(scene==null||!scene.id().toString().equals(tag.getString("Id")))return;
            if(scene.revision()!=tag.getLong("Before")){pending=null;request(EditorPayloads.Intent.RESYNC,null,"",null);return;}
            merge("Objects","RemovedObjects",tag);merge("Groups","RemovedGroups",tag);sceneTag.putString("Name",tag.getString("Name"));sceneTag.putLong("Revision",tag.getLong("Revision"));sceneTag.put("Issues",tag.getList("Issues",Tag.TAG_COMPOUND).copy());readScene();refresh();
        }
        case RESULT->{
            if(pendingOpen!=null&&tag.hasUUID("Request")&&pendingOpen.equals(tag.getUUID("Request"))){pendingOpen=null;if(Minecraft.getInstance().player!=null)Minecraft.getInstance().player.displayClientMessage(Component.translatable(tag.getString("Error")),true);return;}
            if(!tag.hasUUID("Request")||!Objects.equals(pending,tag.getUUID("Request")))return;
            pending=null;error=tag.getString("Error");if(tag.hasUUID("Selected"))selection=tag.getUUID("Selected");refresh();
        }
    }}
    private static void readScene(){
        // Wire-only field metadata must not count against persisted component/object byte limits.
        var stored=sceneTag.copy();stored.remove("Issues");stored.remove("ReadOnly");
        for(var element:stored.getList("Objects",Tag.TAG_COMPOUND))((CompoundTag)element).remove("EditorFields");
        scene=SceneSerialization.load(stored,EditorFoundation.COMPONENTS);fieldCache.clear();
        var issues=new ArrayList<ValidationIssue>();
        for(var element:sceneTag.getList("Issues",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)element;
            issues.add(new ValidationIssue(ValidationIssue.Severity.valueOf(t.getString("Severity")),t.hasUUID("Object")?t.getUUID("Object"):null,
                    t.contains("Component")?ResourceLocation.parse(t.getString("Component")):null,t.getString("Field"),t.getString("Message")));
        }
        scene=new MaplesScene(scene.id(),scene.dimension(),scene.name(),scene.dataVersion(),scene.revision(),scene.objects(),scene.groups(),issues,scene.readOnly()||sceneTag.getBoolean("ReadOnly"));
        for(var element:sceneTag.getList("Objects",Tag.TAG_COMPOUND)){
            var t=(CompoundTag)element;var components=t.getCompound("EditorFields");var byType=new LinkedHashMap<ResourceLocation,Map<String,EditorValue>>();
            for(var type:components.getAllKeys()){var values=components.getCompound(type);var fields=new LinkedHashMap<String,EditorValue>();for(var key:values.getAllKeys())fields.put(key,EditorViews.value(values.getCompound(key)));byType.put(ResourceLocation.parse(type),Map.copyOf(fields));}
            fieldCache.put(t.getUUID("Id"),Map.copyOf(byType));
        }
        if(selection!=null&&!scene.objects().containsKey(selection)&&!scene.groups().containsKey(selection))selection=null;
    }
    private static void merge(String field,String removed,CompoundTag delta){var values=new LinkedHashMap<UUID,CompoundTag>();
        for(var element:sceneTag.getList(field,Tag.TAG_COMPOUND)){var t=(CompoundTag)element;values.put(t.getUUID("Id"),t);}
        for(var element:delta.getList(removed,Tag.TAG_STRING))values.remove(UUID.fromString(element.getAsString()));
        for(var element:delta.getList(field,Tag.TAG_COMPOUND)){var t=(CompoundTag)element;values.put(t.getUUID("Id"),t);}
        var list=new ListTag();values.values().forEach(list::add);sceneTag.put(field,list);
    }
    public static void refresh(){if(Minecraft.getInstance().screen instanceof EditorScreen screen)screen.authoritativeUpdate();}
    private EditorClient(){}
}
