package dev.maplesadventure.authoring;

import dev.maplesadventure.api.editor.ValidationIssue;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public record MaplesScene(ResourceLocation id, ResourceLocation dimension, String name, int dataVersion,
                          long revision, Map<UUID, MaplesObject> objects, Map<UUID, EditorGroup> groups,
                          List<ValidationIssue> issues, boolean readOnly, Map<ResourceLocation,FlagDefinition> flags) {
    public static final int VERSION = 2;
    public MaplesScene(ResourceLocation id, ResourceLocation dimension, String name, int dataVersion,
                       long revision, Map<UUID,MaplesObject> objects, Map<UUID,EditorGroup> groups,
                       List<ValidationIssue> issues, boolean readOnly) {
        this(id,dimension,name,dataVersion,revision,objects,groups,issues,readOnly,Map.of());
    }
    public MaplesScene {
        Objects.requireNonNull(id); Objects.requireNonNull(dimension); EditorLimits.name(name);
        objects = Map.copyOf(objects); groups = Map.copyOf(groups); issues = List.copyOf(issues); flags=Map.copyOf(flags);
        if (objects.size() > EditorLimits.OBJECTS || groups.size() > EditorLimits.GROUPS || flags.size()>1024 || revision < 0)
            throw new IllegalArgumentException("Invalid scene");
        flags.forEach((key,value)->{if(!key.equals(value.id()))throw new IllegalArgumentException("Invalid flag catalog");});
    }
    public static MaplesScene empty(ResourceLocation id, ResourceLocation dimension, String name) {
        return new MaplesScene(id, dimension, name, VERSION, 0, Map.of(), Map.of(), List.of(), false);
    }
}
