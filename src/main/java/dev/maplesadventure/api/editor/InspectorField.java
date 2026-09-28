package dev.maplesadventure.api.editor;

import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Client-safe schema metadata. Registry target is optional and is rechecked on the server. */
public record InspectorField(String id, String label, EditorValue.Kind kind, double min, double max,
                             double step, int maxLength, boolean nullable, boolean readOnly,
                             List<String> choices, ResourceLocation registry) {
    public InspectorField {
        Objects.requireNonNull(id); Objects.requireNonNull(label); Objects.requireNonNull(kind);
        choices = List.copyOf(choices);
        if (!id.matches("[a-zA-Z0-9_.-]{1,64}") || label.length() > 256 || !Double.isFinite(min)
                || !Double.isFinite(max) || min > max || !Double.isFinite(step) || step < 0
                || maxLength < 0 || maxLength > 1024 || choices.size() > 64
                || choices.stream().anyMatch(s -> s.length() > 128) || kind == EditorValue.Kind.NULL)
            throw new IllegalArgumentException("Invalid inspector schema");
    }
    public static InspectorField number(String id, double min, double max, double step) {
        return new InspectorField(id, "editor.maplesadventure.field." + id, EditorValue.Kind.DOUBLE,
                min, max, step, 64, false, false, List.of(), null);
    }
    public void validate(EditorValue value) {
        if (readOnly) throw new IllegalArgumentException("editor.maplesadventure.read_only");
        if (value.kind() == EditorValue.Kind.NULL && nullable) return;
        if (value.kind() != kind || value.text().length() > maxLength) throw new IllegalArgumentException("editor.maplesadventure.invalid_field");
        if ((kind == EditorValue.Kind.DOUBLE || kind == EditorValue.Kind.INTEGER) && (value.number() < min || value.number() > max))
            throw new IllegalArgumentException("editor.maplesadventure.invalid_bounds");
        if (kind == EditorValue.Kind.ENUM && !choices.contains(value.text())) throw new IllegalArgumentException("editor.maplesadventure.invalid_field");
    }
}
