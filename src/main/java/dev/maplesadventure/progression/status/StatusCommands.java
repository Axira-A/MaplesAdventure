package dev.maplesadventure.progression.status;
import com.mojang.brigadier.arguments.*;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class StatusCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new StatusCommands()); }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        for(String root:new String[]{"ma","maplesadventure"}) event.getDispatcher().register(Commands.literal(root).then(
            Commands.literal("status").requires(s->s.hasPermission(2))
                .then(actions("info",false,false)).then(actions("clearall",false,false))
                .then(actions("clear",true,false)).then(actions("proc",true,false)).then(actions("add",true,true))));
    }
    private static com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> actions(String action,boolean type,boolean amount) {
        var root=Commands.literal(action);
        if(!type) return root.executes(c->run(c,action,false)).then(Commands.literal("self").executes(c->run(c,action,true)));
        var arg=Commands.argument("type",StringArgumentType.word()).suggests((c,b)->SharedSuggestionProvider.suggest(java.util.Arrays.stream(StatusEffectType.values()).map(StatusEffectType::id),b));
        if(amount) arg.then(Commands.argument("amount",DoubleArgumentType.doubleArg(0,100000)).executes(c->run(c,action,false))
                .then(Commands.literal("self").executes(c->run(c,action,true))));
        else arg.executes(c->run(c,action,false)).then(Commands.literal("self").executes(c->run(c,action,true)));
        return root.then(arg);
    }
    private static int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> c,String action,boolean self) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var source=c.getSource(); var player=source.getPlayerOrException(); LivingEntity target=player;
        if(!self) {
            var from=player.getEyePosition(); var end=from.add(player.getLookAngle().scale(32));
            end=player.level().clip(new net.minecraft.world.level.ClipContext(from,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player)).getLocation();
            var hit=ProjectileUtil.getEntityHitResult(player,from,end,player.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),
                    e->e instanceof LivingEntity&&e.isAlive()&&e.isPickable()&&dev.maplesadventure.multiplayer.phase.PhaseRelations.canSee(player,e),from.distanceToSqr(end));
            if(hit==null) { source.sendFailure(Component.literal("Look at a visible living entity, or append self.")); return 0; }
            target=(LivingEntity)hit.getEntity();
        }
        try {
            if(action.equals("clearall")) StatusRuntimeService.clearAll(target,StatusRuntimeService.ClearReason.ADMIN);
            else if(!action.equals("info")) {
                var type=StatusEffectType.parse(StringArgumentType.getString(c,"type"));
                if(action.equals("clear")) StatusRuntimeService.clear(target,type,StatusRuntimeService.ClearReason.ADMIN);
                else if(action.equals("proc")) StatusBuildupService.proc(target,type,StatusSourceContext.admin(type));
                else StatusBuildupService.apply(target,type,DoubleArgumentType.getDouble(c,"amount"),StatusSourceContext.admin(type));
            }
        } catch(IllegalArgumentException bad) { source.sendFailure(Component.literal(bad.getMessage())); return 0; }
        var state=StatusRuntimeService.state(target); long now=StatusRuntimeService.now(target);
        say(source,target.getName().getString()+" "+target.getUUID()+" type="+net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(target.getType())+" revision="+state.revision());
        for(var type:StatusEffectType.values()) {
            var r=StatusResistanceService.resolve(target,type); var e=state.get(type); boolean active=e!=null&&e.active(now);
            say(source,type+" current="+(e==null?0:e.current)+" threshold="+r.threshold()+" ratio="+(e==null?0:e.current/r.threshold())+
                    " immune="+r.immune()+" mode="+(active?"ACTIVE_DURATION":"BUILDUP")+" remaining="+(active?e.activeEnd-now:0)+
                    " last="+(e==null?0:e.lastBuildup)+" source="+(e==null?null:e.source)+" definition="+type.definitionId());
        }
        return 1;
    }
    private static void say(CommandSourceStack s,String text) { s.sendSuccess(()->Component.literal(text),false); }
}
