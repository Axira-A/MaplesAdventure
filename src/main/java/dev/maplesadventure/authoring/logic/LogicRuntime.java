package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.logic.LogicContext;
import dev.maplesadventure.authoring.persistence.AuthoringSavedData;
import dev.maplesadventure.editor.EditorSessionService;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;

/** Compiled revision cache, spatial candidates and ephemeral enter/exit state. */
public final class LogicRuntime {
    private record Key(ResourceLocation scene,UUID object){}
    private record Trigger(ResourceLocation dimension,TriggerVolume volume,List<CompiledBinding> bindings){}
    private static final Map<Key,Trigger> TRIGGERS=new LinkedHashMap<>();
    private static final Map<ResourceLocation,Long> REVISIONS=new HashMap<>();
    private static final TriggerSpatialIndex<Key> INDEX=new TriggerSpatialIndex<>();
    private static final TriggerEdges<Key> EDGES=new TriggerEdges<>();
    private static final Set<String> FAILED=new HashSet<>();
    private static final LogicExecutionBudget BUDGET=new LogicExecutionBudget();
    private static long epoch=-1;private static int nesting;
    public static void tick(MinecraftServer server){refresh(server);BUDGET.begin(server.getTickCount());if(TRIGGERS.isEmpty())return;
        for(var p:server.getPlayerList().getPlayers())evaluatePlayer(p);
    }
    public static void evaluatePlayer(ServerPlayer p){
            var server=p.server;if(!server.isSameThread())throw new IllegalStateException("Logic evaluation requires server thread");
            refresh(server);BUDGET.begin(server.getTickCount());
            if(EditorSessionService.active(p)||!p.isAlive()||p.hasDisconnected()){forget(p);return;}
            var candidates=INDEX.candidates(p.level().dimension().location(),p.position());var inside=new LinkedHashSet<Key>();
            for(var key:candidates){var t=TRIGGERS.get(key);if(t!=null&&t.volume()!=null&&t.volume().contains(p.position()))inside.add(key);}
            var change=EDGES.update(p.getUUID(),inside);
            for(var k:change.entered())emit(server,p.serverLevel(),k.scene,k.object,Optional.of(p),BuiltinLogic.ENTER);
            for(var k:change.exited())emit(server,p.serverLevel(),k.scene,k.object,Optional.of(p),BuiltinLogic.EXIT);
    }
    private static void refresh(MinecraftServer server){var store=AuthoringSavedData.get(server);if(epoch==store.changeEpoch())return;epoch=store.changeEpoch();
        for(var scene:store.scenes()){
            if(Objects.equals(REVISIONS.get(scene.id()),scene.revision()))continue;REVISIONS.put(scene.id(),scene.revision());
            TRIGGERS.keySet().removeIf(k->k.scene.equals(scene.id()));FAILED.removeIf(k->k.startsWith(scene.id()+"/"));
            if(scene.readOnly())continue;
            for(var o:scene.objects().values()){
                var data=o.components().get(LogicComponent.ID);if(data==null)continue;
                try{if(data.version()!=1)throw new IllegalArgumentException("Unsupported logic version");var volume=TriggerVolume.of(o);var compiled=new ArrayList<CompiledBinding>();
                    for(var binding:LogicComponent.read(data.data()).bindings())if(binding.enabled()){
                        try{compiled.add(CompiledBinding.compile(binding,volume!=null,server.registryAccess()));}
                        catch(RuntimeException|LinkageError e){warn(scene.id(),o.id(),binding.id(),binding.event().type(),e);}
                    }
                    if(!compiled.isEmpty())TRIGGERS.put(new Key(scene.id(),o.id()),new Trigger(scene.dimension(),volume,List.copyOf(compiled)));
                }catch(RuntimeException|LinkageError e){warn(scene.id(),o.id(),null,LogicComponent.ID,e);}
            }
        }
        INDEX.clear();TRIGGERS.forEach((key,t)->{if(t.volume()!=null)INDEX.add(t.dimension(),t.volume(),key);});EDGES.retain(TRIGGERS.keySet());
    }
    public static void emit(MinecraftServer server,ServerLevel level,ResourceLocation scene,UUID object,Optional<ServerPlayer> player,ResourceLocation event){
        if(!server.isSameThread())throw new IllegalStateException("Logic emission requires server thread");
        if(player.isPresent()&&(EditorSessionService.active(player.get())||player.get().serverLevel()!=level))return;
        refresh(server);BUDGET.begin(server.getTickCount());var key=new Key(scene,object);var trigger=TRIGGERS.get(key);
        if(trigger==null||!trigger.dimension.equals(level.dimension().location()))return;
        var descriptor=BuiltinLogic.EVENTS.entry(event);if(descriptor==null||(descriptor.player()&&player.isEmpty()))return;
        if(nesting>=16){warn(scene,object,null,event,new IllegalStateException("Logic event recursion limit"));return;}
        nesting++;try{int count=0;var context=new LogicContext(server,level,scene,object,player,event);
            for(var b:trigger.bindings)if(b.source().event().type().equals(event)){
                if(++count>LogicLimits.BINDINGS_PER_EVENT)break;String id=scene+"/"+object+"/"+b.source().id();if(FAILED.contains(id))continue;
                try{b.execute(context,BUDGET);}catch(RuntimeException|LinkageError e){FAILED.add(id);warn(scene,object,b.source().id(),event,e);}
            }
        }finally{nesting--;}
    }
    private static void warn(ResourceLocation scene,UUID object,UUID binding,ResourceLocation type,Throwable e){MaplesAdventure.LOGGER.warn("Logic disabled scene={} object={} binding={} type={} reason={}",scene,object,binding,type,LogicValidation.safe(e));}
    public static void forget(ServerPlayer p){EDGES.forget(p.getUUID());}
    public static void clear(){epoch=-1;nesting=0;TRIGGERS.clear();REVISIONS.clear();FAILED.clear();INDEX.clear();EDGES.clear();}
    private LogicRuntime(){}
}
