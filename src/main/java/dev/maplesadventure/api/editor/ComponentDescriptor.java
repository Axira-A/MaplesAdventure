package dev.maplesadventure.api.editor;

import com.mojang.serialization.Codec;
import java.util.*;
import java.util.function.*;
import net.minecraft.resources.ResourceLocation;

/**
 * Experimental authoring API. T must be immutable. Register during common setup, before server start.
 * Codecs and accessors must be side-effect free; they never receive mutable world state.
 * Client renderers belong in the separate client registration API.
 */
public record ComponentDescriptor<T>(ResourceLocation id, int dataVersion, String translationKey,
                                     Supplier<T> defaults, Codec<T> codec, List<Field<T>> fields,
                                     Function<T, List<ValidationIssue>> validator) {
    public record Field<T>(InspectorField schema, Function<T, EditorValue> read, BiFunction<T, EditorValue, T> write) {
        public Field { Objects.requireNonNull(schema); Objects.requireNonNull(read); Objects.requireNonNull(write); }
    }
    public ComponentDescriptor {
        Objects.requireNonNull(id); Objects.requireNonNull(defaults); Objects.requireNonNull(codec); Objects.requireNonNull(validator);
        fields = List.copyOf(fields);
        if (dataVersion < 1 || translationKey == null || translationKey.length() > 256 || fields.size() > 64
                || fields.stream().map(f -> f.schema().id()).distinct().count() != fields.size())
            throw new IllegalArgumentException("Invalid component descriptor");
    }
}
