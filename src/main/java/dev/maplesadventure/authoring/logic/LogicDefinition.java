package dev.maplesadventure.authoring.logic;
import dev.maplesadventure.api.editor.EditorValue;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
/** Unknown type IDs/fields survive persistence; only registered descriptors may compile them. */
public record LogicDefinition(ResourceLocation type, int version, Map<String,EditorValue> fields) {
    public LogicDefinition {
        Objects.requireNonNull(type); fields=Collections.unmodifiableMap(new TreeMap<>(fields));
        if(version<1||fields.size()>LogicLimits.FIELDS||type.toString().length()>256
                ||fields.keySet().stream().anyMatch(k->!k.matches("[a-zA-Z0-9_.-]{1,64}")))throw new IllegalArgumentException("Logic definition limit");
    }
}
