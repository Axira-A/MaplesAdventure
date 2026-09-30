package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.logic.*;
import dev.maplesadventure.authoring.component.ComponentRegistry;
import java.util.*;
import java.util.function.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.*;

/** Reuses the component codec/schema contract, but keeps the three type namespaces distinct. */
public final class LogicTypeRegistry {
    public enum Kind { EVENT, CONDITION, ACTION }
    public record Entry<T>(ComponentDescriptor<T> schema,boolean player,boolean volume,
                           BiPredicate<LogicContext,T> condition,BiConsumer<LogicContext,T> action) {}
    public record Resolved(boolean player,boolean volume,Predicate<LogicContext> condition,Consumer<LogicContext> action){}
    private final Map<ResourceLocation,Entry<?>> entries=new LinkedHashMap<>();
    private final ComponentRegistry schemas=new ComponentRegistry();
    public void register(EventType<?> type){register(new Entry<>(type.schema(),type.suppliesPlayer(),type.requiresVolume(),null,null));}
    public void register(ConditionType<?> type){registerCondition(type);}
    private <T> void registerCondition(ConditionType<T> t){Objects.requireNonNull(t.evaluator());register(new Entry<>(t.schema(),t.requiresPlayer(),false,t.evaluator(),null));}
    public void register(ActionType<?> type){registerAction(type);}
    private <T> void registerAction(ActionType<T> t){Objects.requireNonNull(t.executor());register(new Entry<>(t.schema(),t.requiresPlayer(),false,null,t.executor()));}
    private void register(Entry<?> e){if(entries.size()>=LogicLimits.TYPES||e.schema().fields().size()>LogicLimits.FIELDS)throw new IllegalArgumentException("Logic type limit");schemas.register(e.schema());entries.put(e.schema().id(),e);}
    public void freeze(){schemas.freeze();}
    public Collection<Entry<?>> entries(){return List.copyOf(entries.values());}
    public Entry<?> entry(ResourceLocation id){return entries.get(id);}
    public LogicDefinition defaults(ResourceLocation id){var c=schemas.create(id);return new LogicDefinition(id,c.version(),schemas.fields(c));}
    public Resolved resolve(LogicDefinition d,RegistryAccess registries){var e=entries.get(d.type());if(e==null)throw new IllegalArgumentException("Unknown logic type "+d.type());return resolve(e,d,registries);}
    private <T> Resolved resolve(Entry<T> e,LogicDefinition d,RegistryAccess registries){
        if(d.version()!=e.schema().dataVersion())throw new IllegalArgumentException("Unknown logic version "+d.type());
        var data=schemas.create(d.type());var defaults=schemas.fields(data);var patch=new LinkedHashMap<String,EditorValue>();
        if(!defaults.keySet().equals(d.fields().keySet()))throw new IllegalArgumentException("Missing/unknown logic field "+d.type());
        for(var f:e.schema().fields()){
            var v=d.fields().get(f.schema().id());f.schema().validateValue(v);
            if(f.schema().readOnly()){if(!v.equals(defaults.get(f.schema().id())))throw new IllegalArgumentException("Read-only logic field");}
            else patch.put(f.schema().id(),v);
        }
        if(!patch.isEmpty())data=schemas.patch(data,patch);
        // Validate resulting values, including codec normalization and dependent setters.
        if(!schemas.fields(data).equals(d.fields()))throw new IllegalArgumentException("Logic fields changed during validation");
        for(var f:e.schema().fields())if(f.schema().registry()!=null){
            var v=d.fields().get(f.schema().id());if(v.kind()==EditorValue.Kind.NULL)continue;
            if(registries!=null){var registry=registries.registry(ResourceKey.createRegistryKey(f.schema().registry()));
                if(registry.isEmpty()||!registry.get().containsKey(ResourceLocation.parse(v.text())))throw new IllegalArgumentException("Missing registry entry "+v.text());}
        }
        T value=e.schema().codec().parse(NbtOps.INSTANCE,data.data()).getOrThrow();
        return new Resolved(e.player(),e.volume(),e.condition()==null?null:ctx->e.condition().test(ctx,value),e.action()==null?null:ctx->e.action().accept(ctx,value));
    }
}
