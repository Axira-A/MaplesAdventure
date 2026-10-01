package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.logic.*;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.logic.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.*;
import dev.maplesadventure.editor.network.EditorPayloads;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.server.network.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Opt-in fixture in an isolated world; no test content is generated during normal startup. */
public final class LogicRegression {
    private static final Set<UUID> AUTHORS=new HashSet<>();
    private static final Set<UUID> BLOCK_RETURN=new HashSet<>();
    private static final Set<UUID> BLOCK_TELEPORT=new HashSet<>();
    private static final Map<UUID,Integer> TUTORIAL_MESSAGES=new HashMap<>();
    private static String tutorialText;
    private static final ResourceLocation SCENE=ResourceLocation.parse("weaponregression:logic_persistence"),FLAG=ResourceLocation.parse("weaponregression:logic_entered");
    public static void register(){MaplesEditorApi.registerPermission(ResourceLocation.parse("weaponregression:logic_author"),p->AUTHORS.contains(p.getUUID()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangeGameModeEvent e)->{
            if(BLOCK_RETURN.contains(e.getEntity().getUUID())&&e.getNewGameMode()==GameType.SURVIVAL)e.setCanceled(true);
        });
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e)->e.getDispatcher().register(Commands.literal("ma").then(Commands.literal("logicregression").requires(s->s.hasPermission(2))
            .executes(c->run(c.getSource())).then(Commands.literal("persisted").executes(c->persisted(c.getSource()))))));
    }
    private static void check(boolean ok,String label){if(!ok)throw new IllegalStateException("Logic regression: "+label);MaplesAdventure.LOGGER.info("[Logic regression] PASS {}",label);}
    private static ServerPlayer player(CommandSourceStack s,String name,UUID id){var profile=new GameProfile(id,name);var p=new ServerPlayer(s.getServer(),s.getLevel(),profile,ClientInformation.createDefault()){
        @Override public void teleportTo(ServerLevel level,double x,double y,double z,float yaw,float pitch){if(!BLOCK_TELEPORT.contains(getUUID()))super.teleportTo(level,x,y,z,yaw,pitch);}
    };
        // Real ServerPlayer state/teleport lifecycle, with only transport removed in this fixture.
        p.connection=new ServerGamePacketListenerImpl(s.getServer(),new Connection(PacketFlow.SERVERBOUND),p,CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){
                if(packet instanceof net.minecraft.network.protocol.game.ClientboundSystemChatPacket chat&&chat.content().getString().equals(tutorialText))
                    TUTORIAL_MESSAGES.merge(p.getUUID(),1,Integer::sum);
            }
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){send(packet);}
        };
        p.setPos(s.getLevel().getSharedSpawnPos().getCenter());p.setGameMode(GameType.SURVIVAL);return p;}
    private static void open(ServerPlayer p){EditorSessionService.request(p,new EditorPayloads.Request(UUID.randomUUID(),null,EditorPayloads.Intent.OPEN,null,"",0,null));}
    private static LogicDefinition flag(String type,boolean value,boolean player){return flag(type,value,player,FLAG);}
    private static LogicDefinition flag(String type,boolean value,boolean player,ResourceLocation id){var d=(type.equals("set_flag")?BuiltinLogic.ACTIONS:BuiltinLogic.CONDITIONS).defaults(BuiltinLogic.id(type));var f=new HashMap<>(d.fields());
        f.put("flag",new EditorValue(EditorValue.Kind.RESOURCE_LOCATION,id.toString()));f.put("value",new EditorValue(EditorValue.Kind.BOOLEAN,Boolean.toString(value)));f.put("scope",new EditorValue(EditorValue.Kind.ENUM,player?"PLAYER":"WORLD"));return new LogicDefinition(d.type(),d.version(),f);}
    private static LogicContext context(ServerPlayer p,UUID object){return new LogicContext(p.server,p.serverLevel(),SCENE,object,Optional.of(p),BuiltinLogic.ENTER);}
    private static int run(CommandSourceStack s){var p=player(s,"LogicAuthor",UUID.randomUUID());var other=player(s,"LogicPeer",UUID.randomUUID());AUTHORS.add(p.getUUID());tutorialText="Maker tutorial "+UUID.randomUUID();
        try{
            Vec3 origin=p.position();float yaw=p.getYRot();open(p);
            check(EditorSessionService.active(p)&&p.isSpectator(),"Authorized Survival -> Spectator after storing return point");
            check(p.getData(EditorAttachments.RECOVERY).point().orElseThrow().gameMode()==GameType.SURVIVAL,"Original mode stored");
            p.setPos(origin.add(30,8,20));EditorSessionService.close(p);
            check(!EditorSessionService.active(p)&&p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&p.position().distanceTo(origin)<.001&&p.getYRot()==yaw,"Exit restores position rotation and Survival");
            open(p);var originalReturn=p.getData(EditorAttachments.RECOVERY).point().orElseThrow();BLOCK_RETURN.add(p.getUUID());
            EditorSessionService.close(p);open(p);
            check(!EditorSessionService.active(p)&&p.isSpectator()&&p.getData(EditorAttachments.RECOVERY).point().orElseThrow().equals(originalReturn),"Rejected recovery retains original return point and prevents re-entry");
            BLOCK_RETURN.remove(p.getUUID());EditorRecoveryService.restore(p,false);
            check(!p.isSpectator()&&!p.getData(EditorAttachments.RECOVERY).active(),"Recovery succeeds after external veto clears");
            open(p);var teleportReturn=p.getData(EditorAttachments.RECOVERY).point().orElseThrow();p.setPos(origin.add(4,0,0));BLOCK_TELEPORT.add(p.getUUID());EditorSessionService.close(p);
            check(p.isSpectator()&&p.getData(EditorAttachments.RECOVERY).point().orElseThrow().equals(teleportReturn),"Refused teleport retains recovery and does not pretend gameplay restoration");
            BLOCK_TELEPORT.remove(p.getUUID());EditorRecoveryService.restore(p,false);
            check(!p.isSpectator()&&p.position().distanceTo(origin)<.001&&!p.getData(EditorAttachments.RECOVERY).active(),"Teleport recovery succeeds after veto clears");
            open(p);p.setGameMode(GameType.CREATIVE);EditorSessionService.request(p,new EditorPayloads.Request(UUID.randomUUID(),null,EditorPayloads.Intent.CLOSE,null,"",0,null));
            check(!EditorSessionService.active(p)&&p.isCreative(),"External mode command retained");p.setGameMode(GameType.SURVIVAL);
            open(p);p.setPos(origin.add(9,3,9));EditorSessionService.forget(p);check(!p.isSpectator()&&p.position().distanceTo(origin)<.001,"Logout restores origin and clears recovery");
            open(p);var saved=p.saveWithoutId(new CompoundTag());EditorSessionService.forget(p);
            var orphan=player(s,"OrphanAuthor",p.getUUID());orphan.load(saved);NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerLoggedInEvent(orphan));
            check(orphan.gameMode.getGameModeForPlayer()==GameType.SURVIVAL&&!orphan.getData(EditorAttachments.RECOVERY).active(),"Login from actual serialized player NBT restores orphaned editor");
            open(p);AUTHORS.remove(p.getUUID());EditorSessionService.request(p,new EditorPayloads.Request(UUID.randomUUID(),null,EditorPayloads.Intent.SAVE,null,"",0,null));
            check(!EditorSessionService.active(p)&&!p.isSpectator(),"Permission revoke restores mode");AUTHORS.add(p.getUUID());

            var store=AuthoringSavedData.get(s.getServer());var scene=store.scene(SCENE);if(scene==null)scene=MaplesScene.empty(SCENE,p.level().dimension().location(),"Logic fixture");
            var service=new EditorOperationService(EditorFoundation.COMPONENTS);var created=service.apply(scene,scene.revision(),new EditorOperation.CreateTrigger("Tutorial trigger",new EditorTransform(origin.add(8,0,0),0,0),null));scene=created.scene();var object=scene.objects().get(created.selected());
            var binding=new LogicBinding(UUID.randomUUID(),true,BuiltinLogic.EVENTS.defaults(BuiltinLogic.ENTER),ConditionExpression.leaf(flag("flag_equals",false,false)),
                    List.of(flag("set_flag",true,false),BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message")),BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("play_sound"))));
            scene=service.apply(scene,scene.revision(),new EditorOperation.SetLogic(object.id(),object.revision(),new LogicComponent(List.of(binding)))).scene();store.put(scene);
            GameFlagService.set(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG,false);
            LogicRuntime.evaluatePlayer(p);open(p);p.setPos(object.transform().position());LogicRuntime.evaluatePlayer(p);
            check(!GameFlagService.get(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG),"Editor session is excluded from real spatial trigger evaluation");
            EditorSessionService.close(p);LogicRuntime.evaluatePlayer(p);p.setPos(object.transform().position());LogicRuntime.evaluatePlayer(p);
            check(GameFlagService.get(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG),"Gameplay enter runs flag, message and sound in one compiled binding");
            GameFlagService.set(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG,false);LogicRuntime.evaluatePlayer(p);
            check(!GameFlagService.get(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG),"Remaining inside does not repeat Enter");
            p.setPos(origin);LogicRuntime.evaluatePlayer(p);p.setPos(object.transform().position());LogicRuntime.evaluatePlayer(p);
            check(GameFlagService.get(context(p,object.id()),GameFlagService.Scope.WORLD,FLAG),"Exit and re-enter create a new edge");
            GameFlagService.set(context(p,object.id()),GameFlagService.Scope.PLAYER,FLAG,true);
            check(!GameFlagService.get(context(other,object.id()),GameFlagService.Scope.PLAYER,FLAG),"Player flags independent");
            var clone=player(s,"LogicClone",p.getUUID());clone.restoreFrom(p,false);
            check(clone.getData(EditorAttachments.FLAGS).get(FLAG),"Vanilla restoreFrom/death Clone copies player flag attachment");
            var reload=player(s,"FlagReload",p.getUUID());reload.load(p.saveWithoutId(new CompoundTag()));check(reload.getData(EditorAttachments.FLAGS).get(FLAG),"Player flags survive complete player NBT save/load");
            var flags=GameFlagService.WorldFlags.get(s.getServer());check(GameFlagService.WorldFlags.load(flags.save(new CompoundTag(),p.registryAccess()),p.registryAccess()).save(new CompoundTag(),p.registryAccess()).getCompound("Flags").getBoolean(FLAG.toString()),"World flag SavedData roundtrip");
            // Maker-first scenario: a named catalog is metadata only; a PLAYER condition gates
            // the actual message action independently for each real ServerPlayer instance.
            String tutorialName="已见过教程 "+UUID.randomUUID();scene=service.apply(scene,scene.revision(),new EditorOperation.CreateFlag(tutorialName)).scene();
            var tutorialFlag=scene.flags().values().stream().filter(f->f.name().equals(tutorialName)).findFirst().orElseThrow().id();
            var tutorial=service.apply(scene,scene.revision(),new EditorOperation.CreateTrigger("First visit",new EditorTransform(origin.add(-8,0,0),0,0),null));
            scene=tutorial.scene();var first=scene.objects().get(tutorial.selected());
            var message=BuiltinLogic.ACTIONS.defaults(BuiltinLogic.id("show_message"));var messageFields=new HashMap<>(message.fields());
            messageFields.put("text",new EditorValue(EditorValue.Kind.STRING,tutorialText));message=new LogicDefinition(message.type(),message.version(),messageFields);
            var once=new LogicBinding(UUID.randomUUID(),true,BuiltinLogic.EVENTS.defaults(BuiltinLogic.ENTER),ConditionExpression.leaf(flag("flag_equals",false,true,tutorialFlag)),List.of(message,flag("set_flag",true,true,tutorialFlag)));
            scene=service.apply(scene,scene.revision(),new EditorOperation.SetLogic(first.id(),first.revision(),new LogicComponent(List.of(once)))).scene();store.put(scene);
            check(!GameFlagService.get(context(p,first.id()),GameFlagService.Scope.PLAYER,tutorialFlag)&&!GameFlagService.get(context(other,first.id()),GameFlagService.Scope.PLAYER,tutorialFlag),"Catalog creation does not initialize gameplay values");
            for(var visitor:List.of(p,other)){
                visitor.setPos(origin);LogicRuntime.evaluatePlayer(visitor);visitor.setPos(first.transform().position());LogicRuntime.evaluatePlayer(visitor);
                check(TUTORIAL_MESSAGES.getOrDefault(visitor.getUUID(),0)==1,"First visit sends one message per player");
                visitor.setPos(origin);LogicRuntime.evaluatePlayer(visitor);visitor.setPos(first.transform().position());LogicRuntime.evaluatePlayer(visitor);
                check(TUTORIAL_MESSAGES.getOrDefault(visitor.getUUID(),0)==1,"Re-enter with PLAYER flag does not repeat message");
            }
            s.sendSuccess(()->Component.literal("Logic + spectator lifecycle regression PASS; restart then /ma logicregression persisted"),false);return 1;
        }finally{AUTHORS.clear();BLOCK_RETURN.clear();BLOCK_TELEPORT.clear();TUTORIAL_MESSAGES.clear();tutorialText=null;EditorSessionService.forget(p);EditorSessionService.forget(other);LogicRuntime.forget(p);LogicRuntime.forget(other);}
    }
    private static int persisted(CommandSourceStack s){var scene=AuthoringSavedData.get(s.getServer()).scene(SCENE);
        check(scene!=null&&scene.objects().values().stream().anyMatch(o->o.components().containsKey(LogicComponent.ID)),"Scene and logic survive actual server restart");
        var tag=GameFlagService.WorldFlags.get(s.getServer()).save(new CompoundTag(),s.registryAccess());check(tag.getCompound("Flags").getBoolean(FLAG.toString()),"World flag survives actual server restart");return 1;}
    private LogicRegression(){}
}
