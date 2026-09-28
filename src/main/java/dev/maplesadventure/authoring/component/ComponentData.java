package dev.maplesadventure.authoring.component;

import java.util.Objects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** Opaque persisted value; copying at boundaries prevents addon/UI aliasing of server state. */
public record ComponentData(ResourceLocation type, int version, CompoundTag data) {
    public ComponentData { Objects.requireNonNull(type); Objects.requireNonNull(data); data = data.copy(); }
    @Override public CompoundTag data() { return data.copy(); }
}
