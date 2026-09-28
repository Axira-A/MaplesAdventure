package dev.maplesadventure.authoring.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.maplesadventure.api.editor.*;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public final class BuiltinComponents {
    public static final ResourceLocation MARKER = ResourceLocation.parse("maplesadventure:marker");
    public static final ResourceLocation BOX = ResourceLocation.parse("maplesadventure:box_volume");
    public static final ResourceLocation RADIUS = ResourceLocation.parse("maplesadventure:radius");
    public record Marker(boolean visible) {}
    public record Radius(double radius) {}
    public record Box(double sizeX, double sizeY, double sizeZ) {}
    public static ComponentRegistry createRegistry() {
        var registry = new ComponentRegistry();
        registry.register(new ComponentDescriptor<>(MARKER, 1, "editor.maplesadventure.component.marker", () -> new Marker(true),
                Codec.BOOL.fieldOf("visible").xmap(Marker::new, Marker::visible).codec(), List.of(new ComponentDescriptor.Field<>(
                    new InspectorField("visible", "editor.maplesadventure.field.visible", EditorValue.Kind.BOOLEAN, 0, 1, 1, 5, false, false, List.of(), null),
                    m -> new EditorValue(EditorValue.Kind.BOOLEAN, Boolean.toString(m.visible())), (m,v) -> new Marker(Boolean.parseBoolean(v.text())))), m -> List.of()));
        registry.register(new ComponentDescriptor<>(RADIUS, 1, "editor.maplesadventure.component.radius", () -> new Radius(4),
                Codec.DOUBLE.fieldOf("radius").xmap(Radius::new, Radius::radius).codec(), List.of(new ComponentDescriptor.Field<>(
                    InspectorField.number("radius", 0, 1024, .25), r -> EditorValue.decimal(r.radius()), (r,v) -> new Radius(v.number()))),
                r -> valid(r.radius(), 0, 1024)));
        Codec<Box> boxCodec = RecordCodecBuilder.create(i -> i.group(Codec.DOUBLE.fieldOf("sizeX").forGetter(Box::sizeX),
                Codec.DOUBLE.fieldOf("sizeY").forGetter(Box::sizeY), Codec.DOUBLE.fieldOf("sizeZ").forGetter(Box::sizeZ)).apply(i, Box::new));
        registry.register(new ComponentDescriptor<>(BOX, 1, "editor.maplesadventure.component.box_volume", () -> new Box(4,4,4), boxCodec,
                List.of(new ComponentDescriptor.Field<>(InspectorField.number("sizeX", .01, 2048, .25), b -> EditorValue.decimal(b.sizeX()), (b,v) -> new Box(v.number(),b.sizeY(),b.sizeZ())),
                        new ComponentDescriptor.Field<>(InspectorField.number("sizeY", .01, 2048, .25), b -> EditorValue.decimal(b.sizeY()), (b,v) -> new Box(b.sizeX(),v.number(),b.sizeZ())),
                        new ComponentDescriptor.Field<>(InspectorField.number("sizeZ", .01, 2048, .25), b -> EditorValue.decimal(b.sizeZ()), (b,v) -> new Box(b.sizeX(),b.sizeY(),v.number()))),
                b -> { var issues = new ArrayList<ValidationIssue>(); issues.addAll(valid(b.sizeX(),.01,2048)); issues.addAll(valid(b.sizeY(),.01,2048)); issues.addAll(valid(b.sizeZ(),.01,2048)); return issues; }));
        return registry;
    }
    private static List<ValidationIssue> valid(double value, double min, double max) {
        return Double.isFinite(value) && value >= min && value <= max ? List.of() : List.of(ValidationIssue.error("editor.maplesadventure.invalid_bounds"));
    }
    private BuiltinComponents() {}
}
