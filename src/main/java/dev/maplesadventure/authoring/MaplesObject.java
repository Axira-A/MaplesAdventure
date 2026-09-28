package dev.maplesadventure.authoring;

import dev.maplesadventure.authoring.component.ComponentData;
import java.util.*;
import net.minecraft.resources.ResourceLocation;

public record MaplesObject(UUID id, String name, EditorTransform transform, UUID group,
                           Map<ResourceLocation, ComponentData> components, long revision) {
    public MaplesObject {
        Objects.requireNonNull(id); Objects.requireNonNull(transform); EditorLimits.name(name);
        components = Map.copyOf(components);
        if (components.size() > EditorLimits.COMPONENTS || revision < 0) throw new IllegalArgumentException("Invalid object");
    }
}
