package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.api.editor.logic.LogicContext;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.RegistryAccess;

/** Resolve every condition/action before executing any side effect. */
public record CompiledBinding(LogicBinding source, BiPredicate<LogicContext,LogicExecutionBudget> condition,List<Consumer<LogicContext>> actions) {
    public CompiledBinding {actions=List.copyOf(actions);}
    public static CompiledBinding compile(LogicBinding binding,boolean hasVolume,RegistryAccess registries){
        var event=BuiltinLogic.EVENTS.resolve(binding.event(),registries);
        if(event.volume()&&!hasVolume)throw new IllegalArgumentException("Volume event needs box_volume or radius on this object");
        var condition=compile(binding.conditions(),event.player(),registries);
        var actions=new ArrayList<Consumer<LogicContext>>();
        for(var action:binding.actions()){var resolved=BuiltinLogic.ACTIONS.resolve(action,registries);context(resolved,action,event.player());actions.add(resolved.action());}
        return new CompiledBinding(binding,condition,actions);
    }
    private static void context(LogicTypeRegistry.Resolved r,LogicDefinition d,boolean player){if(!player&&(r.player()||BuiltinLogic.needsPlayer(d)))throw new IllegalArgumentException("Logic type requires player context: "+d.type());}
    private static BiPredicate<LogicContext,LogicExecutionBudget> compile(ConditionExpression c,boolean player,RegistryAccess registries){
        if(c.kind()==ConditionExpression.Kind.LEAF){var r=BuiltinLogic.CONDITIONS.resolve(c.leaf(),registries);context(r,c.leaf(),player);return(ctx,b)->{b.use();return r.condition().test(ctx);};}
        var children=c.children().stream().map(n->compile(n,player,registries)).toList();
        return(ctx,b)->{b.use();return switch(c.kind()){
            case ALL->children.stream().allMatch(n->n.test(ctx,b));case ANY->children.stream().anyMatch(n->n.test(ctx,b));
            case NOT->!children.getFirst().test(ctx,b);default->throw new IllegalStateException("Invalid condition");};};
    }
    public void execute(LogicContext ctx,LogicExecutionBudget budget){budget.use();if(condition.test(ctx,budget))for(int i=0;i<actions.size();i++){
        budget.use();try{actions.get(i).accept(ctx);}catch(RuntimeException|LinkageError e){throw new IllegalStateException("Action "+source.actions().get(i).type()+": "+LogicValidation.safe(e),e);}
    }}
}
