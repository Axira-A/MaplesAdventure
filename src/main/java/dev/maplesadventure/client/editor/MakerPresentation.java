package dev.maplesadventure.client.editor;

import dev.maplesadventure.authoring.logic.*;
import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.authoring.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

/** Pure presentation mapping; neither changes runtime definitions nor requires a client world. */
public final class MakerPresentation {
    public static String name(String localized,ResourceLocation id,boolean advanced){
        String label=localized==null||localized.isBlank()||localized.startsWith("editor.")?id.getPath().replace('_',' '):localized;
        return advanced?label+" · "+id:label;
    }
    public static String conditionKey(ConditionExpression.Kind kind){return "editor.maplesadventure.logic."+switch(kind){case ALL->"all";case ANY->"any";case NOT->"inverted";case LEAF->"condition";};}
    public static ConditionExpression group(ConditionExpression expression,ConditionExpression.Kind kind){
        if(kind!=ConditionExpression.Kind.ALL&&kind!=ConditionExpression.Kind.ANY)throw new IllegalArgumentException("Group kind");
        return new ConditionExpression(kind,null,expression.kind()==ConditionExpression.Kind.ALL||expression.kind()==ConditionExpression.Kind.ANY?expression.children():List.of(expression));
    }
    public static ConditionExpression invert(ConditionExpression expression){return expression.kind()==ConditionExpression.Kind.NOT?expression.children().getFirst():new ConditionExpression(ConditionExpression.Kind.NOT,null,List.of(expression));}
    public static LogicDefinition newDefinition(LogicDefinition defaults){
        if(!defaults.type().equals(BuiltinLogic.id("flag_equals"))&&!defaults.type().equals(BuiltinLogic.id("set_flag")))return defaults;
        var fields=new LinkedHashMap<>(defaults.fields());fields.put("scope",new EditorValue(EditorValue.Kind.ENUM,"PLAYER"));
        return new LogicDefinition(defaults.type(),defaults.version(),fields);
    }
    public static Map<ResourceLocation,String> flags(MaplesScene scene){
        var result=new TreeMap<ResourceLocation,String>(Comparator.comparing(ResourceLocation::toString));
        scene.flags().forEach((id,f)->result.put(id,f.name()));
        for(var object:scene.objects().values()){var c=object.components().get(LogicComponent.ID);if(c==null||c.version()!=1)continue;
            try{for(var b:LogicComponent.read(c.data()).bindings()){collect(result,b.event());condition(result,b.conditions());b.actions().forEach(a->collect(result,a));}}
            catch(IllegalArgumentException ignored){/* Opaque data remains untouched; cannot provide candidates. */}
        }return Collections.unmodifiableMap(result);
    }
    private static void condition(Map<ResourceLocation,String> result,ConditionExpression c){if(c.leaf()!=null)collect(result,c.leaf());c.children().forEach(v->condition(result,v));}
    private static void collect(Map<ResourceLocation,String> result,LogicDefinition d){var f=d.fields().get("flag");if(f!=null&&f.kind()==EditorValue.Kind.RESOURCE_LOCATION){var id=ResourceLocation.tryParse(f.text());if(id!=null)result.putIfAbsent(id,id.getPath());}}
    private MakerPresentation(){}
}
