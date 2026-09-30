package dev.maplesadventure.api.editor;
import dev.maplesadventure.api.editor.logic.*;
import dev.maplesadventure.authoring.logic.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;

/** Experimental authoring API 1.1, independent of the stable gameplay API v1. Register before server start. */
public final class MaplesAuthoringApi {
    public static void registerEventType(EventType<?> type){BuiltinLogic.EVENTS.register(type);}
    public static void registerConditionType(ConditionType<?> type){BuiltinLogic.CONDITIONS.register(type);}
    public static void registerActionType(ActionType<?> type){BuiltinLogic.ACTIONS.register(type);}
    public static void emit(ServerLevel level,ResourceLocation scene,UUID object,Optional<ServerPlayer> player,ResourceLocation event){LogicRuntime.emit(level.getServer(),level,scene,object,player,event);}
    public static boolean worldFlag(LogicContext ctx,ResourceLocation flag){return GameFlagService.get(ctx,GameFlagService.Scope.WORLD,flag);}
    public static void worldFlag(LogicContext ctx,ResourceLocation flag,boolean value){GameFlagService.set(ctx,GameFlagService.Scope.WORLD,flag,value);}
    public static boolean playerFlag(LogicContext ctx,ResourceLocation flag){return GameFlagService.get(ctx,GameFlagService.Scope.PLAYER,flag);}
    public static void playerFlag(LogicContext ctx,ResourceLocation flag,boolean value){GameFlagService.set(ctx,GameFlagService.Scope.PLAYER,flag,value);}
    private MaplesAuthoringApi(){}
}
