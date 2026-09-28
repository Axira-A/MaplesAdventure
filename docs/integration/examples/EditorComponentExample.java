package examples;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.maplesadventure.api.editor.*;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/** Compile-checked, authoring-only extension. Invoke register() from your common setup. */
public final class EditorComponentExample {
    public static final ResourceLocation TYPE = ResourceLocation.fromNamespaceAndPath("example", "annotation");
    public record Annotation(String text, double extent) {}
    private static final Codec<Annotation> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("text").forGetter(Annotation::text),
            Codec.DOUBLE.fieldOf("extent").forGetter(Annotation::extent)).apply(i, Annotation::new));

    public static void register() {
        MaplesEditorApi.registerComponent(new ComponentDescriptor<>(TYPE, 1, "editor.example.annotation",
                () -> new Annotation("", 1), CODEC, List.of(
                new ComponentDescriptor.Field<>(new InspectorField("text", "editor.example.text", EditorValue.Kind.STRING,
                        0, 0, 0, 256, false, false, List.of(), null),
                        v -> new EditorValue(EditorValue.Kind.STRING, v.text()), (v, field) -> new Annotation(field.text(), v.extent())),
                new ComponentDescriptor.Field<>(InspectorField.number("extent", 0.1, 32, 0.1),
                        v -> new EditorValue(EditorValue.Kind.DOUBLE, Double.toString(v.extent())),
                        (v, field) -> new Annotation(v.text(), field.number()))),
                v -> v.text().length() <= 256 && Double.isFinite(v.extent()) && v.extent() >= 0.1 && v.extent() <= 32
                        ? List.of() : List.of(ValidationIssue.error("editor.example.invalid_annotation"))));
    }
    private EditorComponentExample() {}
}
