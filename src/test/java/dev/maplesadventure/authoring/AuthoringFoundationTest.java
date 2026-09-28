package dev.maplesadventure.authoring;

import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.authoring.component.*;
import dev.maplesadventure.authoring.operation.*;
import dev.maplesadventure.authoring.persistence.SceneSerialization;
import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthoringFoundationTest {
    private static final ResourceLocation ID = ResourceLocation.parse("test:scene");
    private final ComponentRegistry registry = BuiltinComponents.createRegistry();
    private MaplesScene empty() { return MaplesScene.empty(ID, ResourceLocation.parse("minecraft:overworld"), "Scene"); }
    private MaplesScene apply(MaplesScene scene, EditorOperation op) {
        return new EditorOperationService(registry).apply(scene, scene.revision(), op).scene();
    }
    @Test void roundtripAndIdentity() {
        MaplesScene scene = apply(empty(), new EditorOperation.CreateObject("Marker", new EditorTransform(new Vec3(1.25, 64, -7.5), 27, -8), null, true));
        UUID id = scene.objects().keySet().iterator().next();
        scene = apply(scene, new EditorOperation.RenameObject(id, 0, "Renamed"));
        assertTrue(scene.objects().containsKey(id));
        scene = apply(scene, new EditorOperation.DuplicateObject(id, 1));
        assertEquals(2, scene.objects().size());
        assertEquals(scene, SceneSerialization.load(SceneSerialization.save(scene), registry));
        assertEquals(3, scene.revision());
    }
    @Test void staleAndInvalidMutationsAreAtomic() {
        MaplesScene scene = apply(empty(), new EditorOperation.CreateObject("x", EditorTransform.origin(), null, false));
        var service = new EditorOperationService(registry);
        assertThrows(EditorOperationService.Rejected.class, () -> service.apply(scene, 0, new EditorOperation.CreateGroup("g", null)));
        assertEquals(1, scene.objects().size());
        assertThrows(IllegalArgumentException.class, () -> new EditorTransform(new Vec3(Double.NaN, 0, 0), 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EditorTransform(Vec3.ZERO, 0, Float.POSITIVE_INFINITY));
    }
    @Test void componentsValidateAndCannotRepeat() {
        MaplesScene scene = apply(empty(), new EditorOperation.CreateObject("x", EditorTransform.origin(), null, false));
        UUID id = scene.objects().keySet().iterator().next();
        var radius = ResourceLocation.parse("maplesadventure:radius");
        scene = apply(scene, new EditorOperation.AddComponent(id, 0, radius));
        final MaplesScene before = scene;
        assertThrows(EditorOperationService.Rejected.class, () -> apply(before, new EditorOperation.AddComponent(id, 1, radius)));
        assertThrows(EditorOperationService.Rejected.class, () -> apply(before, new EditorOperation.PatchComponent(id, 1, radius, Map.of("radius", EditorValue.decimal(-1)))));
        assertEquals(1, before.objects().get(id).revision());
        scene = apply(scene, new EditorOperation.PatchComponent(id, 1, radius, Map.of("radius", EditorValue.decimal(8.5))));
        assertEquals(8.5, registry.fields(scene.objects().get(id).components().get(radius)).get("radius").number());
    }
    @Test void groupsAreOrganizationOnlyAndDeleteLiftsMembers() {
        MaplesScene scene = apply(empty(), new EditorOperation.CreateGroup("root", null));
        UUID group = scene.groups().keySet().iterator().next();
        scene = apply(scene, new EditorOperation.CreateObject("x", EditorTransform.origin(), group, true));
        UUID object = scene.objects().keySet().iterator().next();
        scene = apply(scene, new EditorOperation.DeleteGroup(group, 0));
        assertNull(scene.objects().get(object).group());
        assertEquals(EditorTransform.origin(), scene.objects().get(object).transform());
    }
    @Test void unknownComponentSurvivesWithoutExecuting() {
        var tag = SceneSerialization.save(apply(empty(), new EditorOperation.CreateObject("x", EditorTransform.origin(), null, true)));
        var component = tag.getList("Objects", 10).getCompound(0).getList("Components", 10).getCompound(0);
        component.putString("Type", "absent:component");
        component.getCompound("Data").putString("Secret", "preserved");
        var scene = SceneSerialization.load(tag, registry);
        assertFalse(scene.issues().isEmpty());
        var saved = SceneSerialization.save(scene).getList("Objects", 10).getCompound(0).getList("Components", 10).getCompound(0);
        assertEquals("preserved", saved.getCompound("Data").getString("Secret"));
    }
}
