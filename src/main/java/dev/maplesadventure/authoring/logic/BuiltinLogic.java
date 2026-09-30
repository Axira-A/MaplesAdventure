package dev.maplesadventure.authoring.logic;

import com.mojang.serialization.*;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.logic.*;
import java.util.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.sounds.SoundSource;

public final class BuiltinLogic {
    public static final ResourceLocation ENTER=id("player_enter_volume"),EXIT=id("player_exit_volume");
    public static final LogicTypeRegistry EVENTS=new LogicTypeRegistry(),CONDITIONS=new LogicTypeRegistry(),ACTIONS=new LogicTypeRegistry();
    static {
        EVENTS.register(new EventType<>(empty("player_enter_volume"),true,true));
        EVENTS.register(new EventType<>(empty("player_exit_volume"),true,true));
        CONDITIONS.register(new ConditionType<>(empty("always"),false,(c,v)->true));
        var flagFields=List.of(field("scope",EditorValue.Kind.ENUM,16,List.of("WORLD","PLAYER"),null),
                field("flag",EditorValue.Kind.RESOURCE_LOCATION,256,List.of(),null),field("value",EditorValue.Kind.BOOLEAN,5,List.of(),null));
        var flagDefaults=Map.of("scope",value(EditorValue.Kind.ENUM,"WORLD"),"flag",value(EditorValue.Kind.RESOURCE_LOCATION,"maplesadventure:example"),"value",value(EditorValue.Kind.BOOLEAN,"false"));
        CONDITIONS.register(new ConditionType<>(schema("flag_equals",flagFields,flagDefaults),false,(c,v)->GameFlagService.get(c,scope(v),ResourceLocation.parse(v.get("flag").text()))==Boolean.parseBoolean(v.get("value").text())));
        ACTIONS.register(new ActionType<>(schema("set_flag",flagFields,flagDefaults),false,(c,v)->GameFlagService.set(c,scope(v),ResourceLocation.parse(v.get("flag").text()),Boolean.parseBoolean(v.get("value").text()))));
        ACTIONS.register(new ActionType<>(schema("show_message",List.of(field("text",EditorValue.Kind.STRING,512,List.of(),null),field("translate",EditorValue.Kind.BOOLEAN,5,List.of(),null)),
                Map.of("text",value(EditorValue.Kind.STRING,"Trigger executed"),"translate",value(EditorValue.Kind.BOOLEAN,"false"))),true,
                (c,v)->c.player().orElseThrow().sendSystemMessage(Boolean.parseBoolean(v.get("translate").text())?Component.translatable(v.get("text").text()):Component.literal(v.get("text").text()))));
        ACTIONS.register(new ActionType<>(schema("play_sound",List.of(field("sound",EditorValue.Kind.RESOURCE_LOCATION,256,List.of(),Registries.SOUND_EVENT.location())),
                Map.of("sound",value(EditorValue.Kind.RESOURCE_LOCATION,"minecraft:block.note_block.pling"))),true,(c,v)->{
            var sound=c.level().registryAccess().registryOrThrow(Registries.SOUND_EVENT).get(ResourceLocation.parse(v.get("sound").text()));
            if(sound==null)throw new IllegalArgumentException("Sound removed: "+v.get("sound").text());
            c.player().orElseThrow().playNotifySound(sound,SoundSource.PLAYERS,1,1);
        }));
    }
    private static GameFlagService.Scope scope(Map<String,EditorValue> v){return GameFlagService.Scope.valueOf(v.get("scope").text());}
    public static boolean needsPlayer(LogicDefinition d){return (d.type().equals(id("flag_equals"))||d.type().equals(id("set_flag")))&&"PLAYER".equals(d.fields().getOrDefault("scope",value(EditorValue.Kind.ENUM,"WORLD")).text());}
    public static LogicTypeRegistry registry(LogicTypeRegistry.Kind k){return switch(k){case EVENT->EVENTS;case CONDITION->CONDITIONS;case ACTION->ACTIONS;};}
    public static void freeze(){EVENTS.freeze();CONDITIONS.freeze();ACTIONS.freeze();}
    private static ComponentDescriptor<Map<String,EditorValue>> empty(String name){return schema(name,List.of(),Map.of());}
    /** Built-ins use typed field maps; extensions may supply any immutable codec-backed record. */
    private static ComponentDescriptor<Map<String,EditorValue>> schema(String name,List<InspectorField> fields,Map<String,EditorValue> defaults){
        Codec<EditorValue> valueCodec=Codec.STRING.listOf().comapFlatMap(l->{try{return l.size()==2?DataResult.success(new EditorValue(EditorValue.Kind.valueOf(l.get(0)),l.get(1))):DataResult.error(()->"Scalar pair");}
            catch(RuntimeException e){return DataResult.error(()->"Invalid scalar");}},v->List.of(v.kind().name(),v.text()));
        Codec<Map<String,EditorValue>> codec=Codec.unboundedMap(Codec.STRING,valueCodec);
        var accessors=new ArrayList<ComponentDescriptor.Field<Map<String,EditorValue>>>();
        for(var f:fields)accessors.add(new ComponentDescriptor.Field<>(f,v->v.get(f.id()),(v,next)->{var copy=new LinkedHashMap<>(v);copy.put(f.id(),next);return Map.copyOf(copy);}));
        return new ComponentDescriptor<>(id(name),1,"editor.maplesadventure.logic."+name,()->Map.copyOf(defaults),codec,accessors,v->List.of());
    }
    private static InspectorField field(String id,EditorValue.Kind kind,int length,List<String> choices,ResourceLocation registry){return new InspectorField(id,"editor.maplesadventure.logic.field."+id,kind,0,1,1,length,false,false,choices,registry);}
    private static EditorValue value(EditorValue.Kind kind,String text){return new EditorValue(kind,text);}
    public static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath("maplesadventure",path);}
    private BuiltinLogic(){}
}
