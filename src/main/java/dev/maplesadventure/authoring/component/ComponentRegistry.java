package dev.maplesadventure.authoring.component;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.EditorLimits;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;

/** Registered codecs only. No names from a save or packet are ever used for class loading. */
public final class ComponentRegistry {
    private final Map<ResourceLocation, ComponentDescriptor<?>> types = new LinkedHashMap<>();
    private boolean frozen;
    public synchronized void register(ComponentDescriptor<?> type) {
        if (frozen) throw new IllegalStateException("Editor component registry frozen");
        if (types.containsKey(type.id())) throw new IllegalArgumentException("Duplicate component " + type.id());
        if (types.size() >= 1024) throw new IllegalArgumentException("Too many component types");
        // Validate defaults before accepting the registration.
        create(type); types.put(type.id(), type);
    }
    public synchronized void freeze() { frozen = true; }
    public Collection<ComponentDescriptor<?>> descriptors() { return List.copyOf(types.values()); }
    public ComponentDescriptor<?> descriptor(ResourceLocation id) { return types.get(id); }
    public ComponentData create(ResourceLocation id) { return create(required(id)); }
    private <T> ComponentData create(ComponentDescriptor<T> type) { return encode(type, type.defaults().get()); }
    private ComponentDescriptor<?> required(ResourceLocation id) {
        var result = types.get(id);
        if (result == null) throw new IllegalArgumentException("editor.maplesadventure.unknown_component");
        return result;
    }
    private <T> T decode(ComponentDescriptor<T> type, ComponentData data) {
        if (data.version() != type.dataVersion()) throw new IllegalArgumentException("editor.maplesadventure.unknown_version");
        if (SceneSerialization.bytes(data.data()).length > EditorLimits.COMPONENT_BYTES) throw new IllegalArgumentException("editor.maplesadventure.limit");
        return type.codec().parse(NbtOps.INSTANCE, data.data()).getOrThrow();
    }
    private <T> ComponentData encode(ComponentDescriptor<T> type, T value) {
        if (type.validator().apply(value).stream().anyMatch(i -> i.severity() == ValidationIssue.Severity.ERROR))
            throw new IllegalArgumentException("editor.maplesadventure.invalid_component");
        // A broken accessor must fail the transaction, not create an unreadable Inspector entry.
        for(var field:type.fields())field.schema().validateValue(field.read().apply(value));
        Tag tag = type.codec().encodeStart(NbtOps.INSTANCE, value).getOrThrow();
        if (!(tag instanceof CompoundTag compound) || SceneSerialization.bytes(compound).length > EditorLimits.COMPONENT_BYTES)
            throw new IllegalArgumentException("editor.maplesadventure.limit");
        return new ComponentData(type.id(), type.dataVersion(), compound);
    }
    public Map<String, EditorValue> fields(ComponentData data) { return fields(required(data.type()), data); }
    private <T> Map<String, EditorValue> fields(ComponentDescriptor<T> type, ComponentData data) {
        T value = decode(type, data); Map<String, EditorValue> fields = new LinkedHashMap<>();
        for (var field : type.fields()) fields.put(field.schema().id(), field.read().apply(value));
        return Map.copyOf(fields);
    }
    public ComponentData patch(ComponentData data, Map<String, EditorValue> fields) { return patch(required(data.type()), data, fields); }
    private <T> ComponentData patch(ComponentDescriptor<T> type, ComponentData data, Map<String, EditorValue> patch) {
        if (patch.isEmpty() || patch.size() > EditorLimits.FIELDS) throw new IllegalArgumentException("editor.maplesadventure.limit");
        T value = decode(type, data);
        for (var entry : patch.entrySet()) {
            var field = type.fields().stream().filter(f -> f.schema().id().equals(entry.getKey())).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("editor.maplesadventure.invalid_field"));
            field.schema().validate(entry.getValue()); value = field.write().apply(value, entry.getValue());
        }
        return encode(type, value);
    }
    public List<ValidationIssue> validate(ComponentData data) {
        try { return validate(required(data.type()), data); }
        catch (RuntimeException | LinkageError error) { return List.of(ValidationIssue.error(
                descriptor(data.type()) == null ? "editor.maplesadventure.unknown_component" : "editor.maplesadventure.invalid_component")); }
    }
    private <T> List<ValidationIssue> validate(ComponentDescriptor<T> type, ComponentData data) {
        T value=decode(type,data);
        for(var field:type.fields())field.schema().validateValue(field.read().apply(value));
        return List.copyOf(type.validator().apply(value));
    }
}
