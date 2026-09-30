package dev.maplesadventure.editor;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.network.*;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.network.PacketDistributor;

/** Ephemeral subscriptions only. Scene ownership and contents live in SavedData. */
public final class EditorSessionService {
    private static final class Session {
        final UUID nonce=UUID.randomUUID();final ResourceLocation dimension;
        final Set<UUID> completed=new LinkedHashSet<>();ResourceLocation selected;long lastSnapshot=-100;
        Session(ServerPlayer p){dimension=p.level().dimension().location();}
    }
    private static final Map<UUID,Session> SESSIONS=new HashMap<>();
    private static final Map<UUID,EditorRateLimit> RATES=new HashMap<>();
    private static final Map<UUID,Long> WARNED=new HashMap<>();
    public static boolean active(ServerPlayer p){return SESSIONS.containsKey(p.getUUID());}
    private static boolean available(ServerPlayer p){return p.isAlive()&&!p.isRemoved()
            &&!dev.maplesadventure.bonfire.BonfireSessionService.isBusy(p)
            &&!dev.maplesadventure.api.flask.FlaskApi.isUsingFlask(p)
            &&!dev.maplesadventure.progression.status.StatusControlLockService.locked(p);}
    public static void request(ServerPlayer p,EditorPayloads.Request r){
        if(!p.server.isSameThread())throw new IllegalStateException("Editor mutations require server thread");
        long tick=p.server.getTickCount();
        Session s=SESSIONS.get(p.getUUID());
        if(s!=null&&!p.isSpectator()){close(p,true);return;}
        // Cleanup cannot be starved by a burst of authoring writes.
        if(r.intent()==EditorPayloads.Intent.CLOSE){if(s!=null&&s.nonce.equals(r.session()))close(p);return;}
        if(!RATES.computeIfAbsent(p.getUUID(),id->new EditorRateLimit()).take(tick)){
            if(s!=null&&s.nonce.equals(r.session())&&tick-WARNED.getOrDefault(p.getUUID(),-200L)>=20){
                WARNED.put(p.getUUID(),tick);reply(p,s.nonce,r.requestId(),"editor.maplesadventure.rate",null);
            }return;
        }
        if(!EditorPermissions.authorized(p)){
            if(tick-WARNED.getOrDefault(p.getUUID(),-200L)>=100){WARNED.put(p.getUUID(),tick);MaplesAdventure.LOGGER.warn("Unauthorized editor request player={}",p.getUUID());}
            close(p);reply(p,r.session()==null?r.requestId():r.session(),r.requestId(),"editor.maplesadventure.unauthorized",null);return;
        }
        if(r.intent()==EditorPayloads.Intent.OPEN){
            if(!available(p)){reply(p,r.requestId(),r.requestId(),"editor.maplesadventure.busy",null);return;}
            if(s!=null)close(p);
            try{EditorRecoveryService.begin(p);}catch(RuntimeException failure){
                MaplesAdventure.LOGGER.warn("Editor spectator entry failed for {}",p.getUUID(),failure);
                reply(p,r.requestId(),r.requestId(),"editor.maplesadventure.spectator_refused",null);return;
            }
            s=new Session(p);SESSIONS.put(p.getUUID(),s);
            try{
                // Resolve addon metadata before opening a client workspace. Callback failures must
                // not strand the player in Spectator or expose a partially initialized registry.
                var schema=EditorViews.schema();
                var opened=new CompoundTag();opened.putUUID("Request",r.requestId());send(p,s.nonce,EditorPayloads.Kind.SESSION,opened);
                send(p,s.nonce,EditorPayloads.Kind.SCHEMA,schema);catalog(p,s);
            }catch(RuntimeException|LinkageError failure){
                close(p);MaplesAdventure.LOGGER.error("Editor descriptor initialization failed for {}",p.getUUID(),failure);
                reply(p,r.requestId(),r.requestId(),"editor.maplesadventure.invalid_snapshot",null);
            }return;
        }
        if(s==null||!s.nonce.equals(r.session())||!s.dimension.equals(p.level().dimension().location())||!available(p)){close(p);return;}
        if(s.completed.contains(r.requestId()))return;
        s.completed.add(r.requestId());if(s.completed.size()>128)s.completed.remove(s.completed.iterator().next());
        var store=AuthoringSavedData.get(p.server);
        try{
            switch(r.intent()){
                case CREATE_SCENE->{
                    if(store.scene(r.scene())!=null)throw new EditorOperationService.Rejected("editor.maplesadventure.duplicate_scene");
                    store.put(MaplesScene.empty(r.scene(),s.dimension,r.name()));s.selected=r.scene();catalogAll(p.server);snapshot(p,s,store.scene(s.selected));reply(p,s.nonce,r.requestId(),"",null);
                }
                case SELECT_SCENE->{var scene=required(store,r.scene(),s.dimension);s.selected=scene.id();snapshot(p,s,scene);reply(p,s.nonce,r.requestId(),"",null);}
                case SAVE->{if(s.selected==null)throw new IllegalArgumentException("editor.maplesadventure.missing_reference");required(store,s.selected,s.dimension);store.setDirty();reply(p,s.nonce,r.requestId(),"",null);}
                case RESYNC,VALIDATE->{if(s.selected!=null){if(tick-s.lastSnapshot<20){reply(p,s.nonce,r.requestId(),"editor.maplesadventure.rate",null);return;}s.lastSnapshot=tick;snapshot(p,s,required(store,s.selected,s.dimension));}reply(p,s.nonce,r.requestId(),"",null);}
                case OPERATION->{
                    if(!Objects.equals(r.scene(),s.selected))throw new EditorOperationService.Rejected("editor.maplesadventure.missing_reference");
                    var old=required(store,s.selected,s.dimension);validateWorld(p,r.operation());
                    var applied=new EditorOperationService(EditorFoundation.COMPONENTS).apply(old,r.revision(),r.operation());
                    var result=new EditorOperationService.Result(dev.maplesadventure.authoring.logic.LogicValidation.scene(applied.scene(),p.registryAccess()),applied.selected());
                    if(r.operation() instanceof EditorOperation.SetLogic logic&&result.scene().issues().stream().anyMatch(i->logic.id().equals(i.object())&&dev.maplesadventure.authoring.logic.LogicComponent.ID.equals(i.component())))
                        throw new EditorOperationService.Rejected("editor.maplesadventure.invalid_logic");
                    if(r.operation() instanceof EditorOperation.CommitDraft draft&&draft.logic()!=null&&result.scene().issues().stream().anyMatch(i->draft.id().equals(i.object())&&dev.maplesadventure.authoring.logic.LogicComponent.ID.equals(i.component())))
                        throw new EditorOperationService.Rejected("editor.maplesadventure.invalid_logic");
                    validateReferences(p,old,result.scene());
                    var delta=EditorViews.delta(old,result.scene()); // Serialize callbacks before committing, so errors cannot half-commit.
                    if(SceneSerialization.bytes(delta).length>EditorLimits.SCENE_BYTES*2)throw new IllegalArgumentException("editor.maplesadventure.limit");
                    store.put(result.scene());
                    for(var entry:List.copyOf(SESSIONS.entrySet())){var viewer=p.server.getPlayerList().getPlayer(entry.getKey());var session=entry.getValue();
                        if(viewer!=null&&EditorPermissions.authorized(viewer)&&Objects.equals(session.selected,old.id())&&session.dimension.equals(viewer.level().dimension().location()))send(viewer,session.nonce,EditorPayloads.Kind.DELTA,delta);}
                    if(r.operation() instanceof EditorOperation.RenameScene)catalogAll(p.server);
                    reply(p,s.nonce,r.requestId(),"",result.selected());
                }
                default->{ }
            }
        }catch(RuntimeException|LinkageError error){
            String reason=error.getMessage()!=null&&error.getMessage().startsWith("editor.")?error.getMessage():"editor.maplesadventure.invalid_operation";
            if(!(error instanceof IllegalArgumentException)&&!(error instanceof EditorOperationService.Rejected))MaplesAdventure.LOGGER.error("Editor operation failed scene={} player={}",s.selected,p.getUUID(),error);
            if(reason.equals("editor.maplesadventure.stale")&&s.selected!=null){snapshot(p,s,store.scene(s.selected));MaplesAdventure.LOGGER.debug("Rejected stale editor operation scene={} player={}",s.selected,p.getUUID());}
            reply(p,s.nonce,r.requestId(),reason,null);
        }
    }
    private static MaplesScene required(AuthoringSavedData store,ResourceLocation id,ResourceLocation dimension){
        var scene=store.scene(id);if(scene==null||!scene.dimension().equals(dimension))throw new EditorOperationService.Rejected("editor.maplesadventure.missing_reference");return scene;
    }
    private static void validateWorld(ServerPlayer p,EditorOperation op){
        EditorTransform transform=op instanceof EditorOperation.CreateObject o?o.transform():op instanceof EditorOperation.CreateTrigger o?o.transform():op instanceof EditorOperation.CommitDraft o?o.transform():op instanceof EditorOperation.SetTransform o?o.transform():null;
        if(transform!=null){var pos=transform.position();if(pos.y<p.level().getMinBuildHeight()||pos.y>p.level().getMaxBuildHeight()||!p.level().getWorldBorder().isWithinBounds(pos.x,pos.z))throw new IllegalArgumentException("editor.maplesadventure.invalid_transform");}
        if(op instanceof EditorOperation.PatchComponent patch){
            var descriptor=EditorFoundation.COMPONENTS.descriptor(patch.type());if(descriptor==null)throw new IllegalArgumentException("editor.maplesadventure.unknown_component");
            for(var f:descriptor.fields())if(f.schema().registry()!=null&&patch.fields().containsKey(f.schema().id())){
                var v=patch.fields().get(f.schema().id());if(v.kind()==EditorValue.Kind.NULL&&f.schema().nullable())continue;
                f.schema().validate(v);
                var registry=p.registryAccess().registry(ResourceKey.createRegistryKey(f.schema().registry()));
                if(registry.isEmpty()||!registry.get().containsKey(ResourceLocation.parse(v.text())))throw new IllegalArgumentException("editor.maplesadventure.missing_reference");
            }
        }
    }
    private static void validateReferences(ServerPlayer p,MaplesScene old,MaplesScene next){
        // Defaults and addon setters can change registry fields as well as explicit field patches.
        // Recheck the resulting changed components, without blocking unrelated edits to opaque data.
        for(var object:next.objects().values()){
            var previous=old.objects().get(object.id());
            for(var component:object.components().values()){
                if(previous!=null&&component.equals(previous.components().get(component.type())))continue;
                var descriptor=EditorFoundation.COMPONENTS.descriptor(component.type());
                if(descriptor==null||descriptor.fields().stream().noneMatch(f->f.schema().registry()!=null))continue;
                var values=EditorFoundation.COMPONENTS.fields(component);
                for(var field:descriptor.fields()){
                    var schema=field.schema();if(schema.registry()==null)continue;
                    var value=values.get(schema.id());schema.validateValue(value);
                    if(value.kind()==EditorValue.Kind.NULL)continue;
                    var registry=p.registryAccess().registry(ResourceKey.createRegistryKey(schema.registry()));
                    if(registry.isEmpty()||!registry.get().containsKey(ResourceLocation.parse(value.text())))
                        throw new IllegalArgumentException("editor.maplesadventure.missing_reference");
                }
            }
        }
    }
    private static void catalog(ServerPlayer p,Session s){var root=new CompoundTag();var list=new ListTag();
        for(var scene:AuthoringSavedData.get(p.server).scenes())if(scene.dimension().equals(s.dimension)){var t=new CompoundTag();t.putString("Id",scene.id().toString());t.putString("Name",scene.name());list.add(t);}root.put("Scenes",list);root.putBoolean("ReadOnly",AuthoringSavedData.get(p.server).readOnly());send(p,s.nonce,EditorPayloads.Kind.CATALOG,root);}
    private static void catalogAll(MinecraftServer server){for(var entry:List.copyOf(SESSIONS.entrySet())){var p=server.getPlayerList().getPlayer(entry.getKey());if(p!=null&&EditorPermissions.authorized(p))catalog(p,entry.getValue());}}
    private static void snapshot(ServerPlayer p,Session s,MaplesScene scene){if(scene!=null)send(p,s.nonce,EditorPayloads.Kind.SNAPSHOT,EditorViews.scene(dev.maplesadventure.authoring.logic.LogicValidation.scene(scene,p.registryAccess())));}
    private static void reply(ServerPlayer p,UUID session,UUID request,String error,UUID selected){var tag=new CompoundTag();tag.putUUID("Request",request);tag.putString("Error",error);if(selected!=null)tag.putUUID("Selected",selected);send(p,session,EditorPayloads.Kind.RESULT,tag);}
    private static void send(ServerPlayer p,UUID session,EditorPayloads.Kind kind,CompoundTag tag){
        byte[] bytes=SceneSerialization.bytes(tag);if(bytes.length>EditorLimits.SCENE_BYTES*2)throw new IllegalArgumentException("Editor snapshot limit");
        int count=Math.max(1,(bytes.length+EditorLimits.PAGE_BYTES-1)/EditorLimits.PAGE_BYTES);UUID transfer=UUID.randomUUID();
        for(int i=0;i<count;i++)PacketDistributor.sendToPlayer(p,new EditorPayloads.Page(session,transfer,kind,i,count,Arrays.copyOfRange(bytes,i*EditorLimits.PAGE_BYTES,Math.min(bytes.length,(i+1)*EditorLimits.PAGE_BYTES))));
    }
    public static void close(ServerPlayer p){close(p,false);}
    private static void close(ServerPlayer p,boolean preserveExternalMode){
        var s=SESSIONS.remove(p.getUUID());if(s==null)return;
        try{if(!p.hasDisconnected())send(p,s.nonce,EditorPayloads.Kind.CLOSED,new CompoundTag());}
        finally{EditorRecoveryService.restore(p,preserveExternalMode);}
    }
    public static void forget(ServerPlayer p){close(p);RATES.remove(p.getUUID());WARNED.remove(p.getUUID());}
    public static void tick(MinecraftServer server){for(var entry:List.copyOf(SESSIONS.entrySet())){var p=server.getPlayerList().getPlayer(entry.getKey());
        if(p==null){SESSIONS.remove(entry.getKey());continue;}
        if(!p.isSpectator()){close(p,true);continue;}
        if(p.getCamera()!=p)p.setCamera(p);
        if(!EditorPermissions.authorized(p)||!available(p)||!entry.getValue().dimension.equals(p.level().dimension().location()))close(p);}}
    public static void closeAll(MinecraftServer server){for(var id:List.copyOf(SESSIONS.keySet())){var p=server.getPlayerList().getPlayer(id);if(p!=null)close(p);}}
    public static void clear(){SESSIONS.clear();RATES.clear();WARNED.clear();}
    private EditorSessionService(){}
}
