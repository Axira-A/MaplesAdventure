package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.api.editor.ValidationIssue;
import dev.maplesadventure.authoring.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
public final class LogicValidation {
    public static List<ValidationIssue> object(MaplesObject o,RegistryAccess registries){
        var issues=new ArrayList<ValidationIssue>();var component=o.components().get(LogicComponent.ID);if(component==null)return issues;
        try{if(component.version()!=1)throw new IllegalArgumentException("Unsupported logic component version");
            var bindings=LogicComponent.read(component.data());var volume=TriggerVolume.of(o);
            for(var b:bindings.bindings())try{CompiledBinding.compile(b,volume!=null,registries);}
                catch(RuntimeException|LinkageError ex){issues.add(new ValidationIssue(ValidationIssue.Severity.ERROR,o.id(),LogicComponent.ID,b.id().toString(),"Binding "+b.id()+": "+safe(ex)));}
        }catch(RuntimeException|LinkageError ex){issues.add(new ValidationIssue(ValidationIssue.Severity.ERROR,o.id(),LogicComponent.ID,"",safe(ex)));}
        return List.copyOf(issues);
    }
    public static MaplesScene scene(MaplesScene s,RegistryAccess registries){var issues=new ArrayList<>(s.issues().stream().filter(i->!LogicComponent.ID.equals(i.component())).toList());
        for(var o:s.objects().values())issues.addAll(object(o,registries));
        return new MaplesScene(s.id(),s.dimension(),s.name(),s.dataVersion(),s.revision(),s.objects(),s.groups(),issues,s.readOnly());}
    public static String safe(Throwable e){String text=e.getMessage();return text==null?e.getClass().getSimpleName():text.substring(0,Math.min(512,text.length()));}
    private LogicValidation(){}
}
