package dev.maplesadventure.authoring;

import dev.maplesadventure.api.editor.ValidationIssue;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public record MaplesScene(ResourceLocation id, ResourceLocation dimension, String name, int dataVersion,
                          long revision, Map<UUID, MaplesObject> objects, Map<UUID, EditorGroup> groups,
                          List<ValidationIssue> issues, boolean readOnly) {
    public static final int VERSION = 1;
    public MaplesScene {
        Objects.requireNonNull(id); Objects.requireNonNull(dimension); EditorLimits.name(name);
        objects = Map.copyOf(objects); groups = Map.copyOf(groups); issues = List.copyOf(issues);
        if (objects.size() > EditorLimits.OBJECTS || groups.size() > EditorLimits.GROUPS || revision < 0)
            throw new IllegalArgumentException("Invalid scene");
    }
    public static MaplesScene empty(ResourceLocation id, ResourceLocation dimension, String name) {
        return new MaplesScene(id, dimension, name, VERSION, 0, Map.of(), Map.of(), List.of(), false);
    }
}
