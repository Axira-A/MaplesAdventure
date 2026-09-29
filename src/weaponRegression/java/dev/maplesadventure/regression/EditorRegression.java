package dev.maplesadventure.regression;

import com.mojang.authlib.GameProfile;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.api.editor.MaplesEditorApi;
import dev.maplesadventure.api.editor.ComponentDescriptor;
import dev.maplesadventure.api.editor.InspectorField;
import dev.maplesadventure.api.editor.EditorValue;
import dev.maplesadventure.authoring.*;
import dev.maplesadventure.authoring.operation.EditorOperation;
import dev.maplesadventure.authoring.persistence.*;
import dev.maplesadventure.editor.*;
import dev.maplesadventure.editor.network.EditorPayloads;
import java.util.*;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Isolated fixture only. Exercises the real server request boundary, not a client visual test. */
public final class EditorRegression {
    private static final Set<UUID> AUTHORS = new HashSet<>();
    private static final ResourceLocation BAD_REFERENCE=ResourceLocation.parse("weaponregression:missing_item_reference");
    public static void register() {
        MaplesEditorApi.registerPermission(ResourceLocation.fromNamespaceAndPath("weaponregression", "editor"), p -> AUTHORS.contains(p.getUUID()));
        var itemField=new InspectorField("item","Item",EditorValue.Kind.RESOURCE_LOCATION,0,0,0,128,false,false,List.of(),ResourceLocation.withDefaultNamespace("item"));
        MaplesEditorApi.registerComponent(new ComponentDescriptor<>(BAD_REFERENCE,1,"Missing item reference",()->"weaponregression:missing_item",
                com.mojang.serialization.Codec.STRING.fieldOf("item").codec(),List.of(new ComponentDescriptor.Field<String>(itemField,
                value->new EditorValue(EditorValue.Kind.RESOURCE_LOCATION,value),(old,value)->value.text())),value->List.of()));
        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("ma").then(Commands.literal("editorregression").requires(s -> s.hasPermission(2))
                        .executes(c -> run(c.getSource()))
                        .then(Commands.literal("persisted").executes(c -> persisted(c.getSource()))))));
    }
    private static void check(boolean ok, String label) {
        if (!ok) throw new IllegalStateException("Editor regression: " + label);
        MaplesAdventure.LOGGER.info("[Editor regression] PASS {}", label);
    }
    private static ServerPlayer player(CommandSourceStack source, String name) {
        var profile = new GameProfile(UUID.randomUUID(), name);
        var p = new ServerPlayer(source.getServer(), source.getLevel(), profile, ClientInformation.createDefault());
        p.connection = FakePlayerFactory.get(source.getLevel(), profile).connection;
        p.setPos(source.getLevel().getSharedSpawnPos().getCenter());
        return p;
    }
    @SuppressWarnings("unchecked") private static UUID nonce(ServerPlayer p) throws ReflectiveOperationException {
        // Fixture inspection only; production exposes no mutable session handles.
        var field = EditorSessionService.class.getDeclaredField("SESSIONS"); field.setAccessible(true);
        var session = ((Map<UUID, ?>) field.get(null)).get(p.getUUID());
        var n = session.getClass().getDeclaredField("nonce"); n.setAccessible(true); return (UUID) n.get(session);
    }
    private static void send(ServerPlayer p, UUID session, EditorPayloads.Intent intent, ResourceLocation scene, long revision, EditorOperation op) {
        EditorSessionService.request(p, new EditorPayloads.Request(UUID.randomUUID(), session, intent, scene, "Fixture", revision, op));
    }
    private static int run(CommandSourceStack source) {
        var a = player(source, "EditorFixtureA"); var b = player(source, "EditorFixtureB"); var guest = player(source, "EditorGuest");
        AUTHORS.add(a.getUUID()); AUTHORS.add(b.getUUID());
        try {
            guest.setGameMode(GameType.CREATIVE);
            send(guest, null, EditorPayloads.Intent.OPEN, null, 0, null);
            check(!EditorSessionService.active(guest), "Creative without permission rejected");
            send(a, null, EditorPayloads.Intent.OPEN, null, 0, null); send(b, null, EditorPayloads.Intent.OPEN, null, 0, null);
            check(EditorSessionService.active(a) && EditorSessionService.active(b), "Two authorized sessions");
            var na = nonce(a); var nb = nonce(b);
            var id = ResourceLocation.fromNamespaceAndPath("weaponregression", "editor_" + UUID.randomUUID().toString().replace("-", ""));
            var store = AuthoringSavedData.get(source.getServer());
            send(a, na, EditorPayloads.Intent.CREATE_SCENE, id, 0, null);
            send(b, nb, EditorPayloads.Intent.SELECT_SCENE, id, 0, null);
            check(store.scene(id) != null && store.scene(id).revision() == 0, "Server creates explicit dimension Scene");
            var transform = new EditorTransform(a.position().add(2, 0, 0), 25, 0);
            var op = new EditorOperation.CreateObject("Marker", transform, null, true);
            var once = new EditorPayloads.Request(UUID.randomUUID(), na, EditorPayloads.Intent.OPERATION, id, "", 0, op);
            EditorSessionService.request(a, once); EditorSessionService.request(a, once);
            check(store.scene(id).objects().size() == 1 && store.scene(id).revision() == 1, "Duplicate request executes once");
            send(b, nb, EditorPayloads.Intent.OPERATION, id, 0, new EditorOperation.RenameScene("stale"));
            check(store.scene(id).name().equals("Fixture") && store.scene(id).revision() == 1, "Concurrent stale revision cannot overwrite");
            send(guest, na, EditorPayloads.Intent.OPERATION, id, 1, new EditorOperation.RenameScene("forged"));
            check(store.scene(id).name().equals("Fixture"), "Forged nonce/permission cannot write");
            var object = store.scene(id).objects().values().iterator().next();
            send(a,na,EditorPayloads.Intent.OPERATION,id,1,new EditorOperation.AddComponent(object.id(),object.revision(),BAD_REFERENCE));
            check(store.scene(id).revision()==1&&!store.scene(id).objects().get(object.id()).components().containsKey(BAD_REFERENCE), "Invalid registry reference in addon default rejects atomically");
            send(a, na, EditorPayloads.Intent.OPERATION, id, 1, new EditorOperation.SetTransform(object.id(), object.revision(), new EditorTransform(new Vec3(0, 10000, 0), 0, 0)));
            check(store.scene(id).revision() == 1, "Out-of-dimension height rejected atomically");
            var foreign = ResourceLocation.fromNamespaceAndPath("weaponregression", "foreign_" + UUID.randomUUID().toString().replace("-", ""));
            store.put(MaplesScene.empty(foreign, ResourceLocation.withDefaultNamespace("the_nether"), "Foreign"));
            send(a, na, EditorPayloads.Intent.SELECT_SCENE, foreign, 0, null);
            send(a, na, EditorPayloads.Intent.OPERATION, foreign, 0, new EditorOperation.RenameScene("forged"));
            check(store.scene(foreign).revision() == 0, "Cross-dimension subscription/write refused");
            send(a, na, EditorPayloads.Intent.OPERATION, id, 1, new EditorOperation.DuplicateObject(object.id(), object.revision()));
            check(store.scene(id).objects().size() == 2 && store.scene(id).objects().values().stream().allMatch(o -> o.transform().equals(transform)), "Duplicate UUID differs; position remains absolute");
            var restored = AuthoringSavedData.load(store.save(new net.minecraft.nbt.CompoundTag(), a.registryAccess()), a.registryAccess());
            check(restored.scene(id).objects().equals(store.scene(id).objects()), "World persistence roundtrip");
            AUTHORS.remove(a.getUUID());
            send(a, na, EditorPayloads.Intent.OPERATION, id, 2, new EditorOperation.RenameScene("revoked"));
            check(!EditorSessionService.active(a) && store.scene(id).revision() == 2, "Permission revoked closes session before write");
            EditorSessionService.close(b);
            check(!EditorSessionService.active(b), "Session close removes subscription");
            source.sendSuccess(() -> Component.literal("Editor regression PASS; restart then /ma editorregression persisted"), false);
            return 1;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
        finally { AUTHORS.clear(); EditorSessionService.forget(a); EditorSessionService.forget(b); EditorSessionService.forget(guest); }
    }
    private static int persisted(CommandSourceStack source) {
        check(AuthoringSavedData.get(source.getServer()).scenes().stream().anyMatch(s -> s.id().getPath().startsWith("editor_") && s.objects().size() == 2 && s.revision() == 2), "Saved Scene and UUIDs survive server restart");
        return 1;
    }
    private EditorRegression() {}
}
