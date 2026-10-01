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
        // The listener must own this ServerPlayer: teleport updates the listener's player.
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(source.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND),p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(profile,false)){
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet){}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet,net.minecraft.network.PacketSendListener listener){}
        };
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
            na=maker(source,a,b,na,nb);
            send(a,na,EditorPayloads.Intent.SELECT_SCENE,id,2,null);
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
    private static UUID maker(CommandSourceStack source,ServerPlayer a,ServerPlayer b,UUID na,UUID nb)throws ReflectiveOperationException{
        var store=AuthoringSavedData.get(source.getServer());var ids=new HashSet<ResourceLocation>();store.scenes().forEach(s->ids.add(s.id()));
        send(a,na,EditorPayloads.Intent.CREATE_SCENE,null,0,null);
        var id=store.scenes().stream().filter(s->!ids.contains(s.id())).findFirst().orElseThrow().id();
        check(id.toString().startsWith("maplesadventure:scenes/"),"Maker Scene ID generated by server");
        send(b,nb,EditorPayloads.Intent.SELECT_SCENE,id,0,null);
        send(a,na,EditorPayloads.Intent.OPERATION,id,0,new EditorOperation.CreateFlag("已见过教程"));
        var flag=store.scene(id).flags().values().iterator().next();
        send(b,nb,EditorPayloads.Intent.UNDO,id,1,null);check(store.scene(id).flags().isEmpty()&&store.scene(id).revision()==2,"Second author undoes shared catalog operation");
        send(a,na,EditorPayloads.Intent.REDO,id,2,null);check(store.scene(id).flags().containsKey(flag.id())&&store.scene(id).revision()==3,"Shared redo preserves flag ID and raises revision");
        send(b,nb,EditorPayloads.Intent.UNDO,id,2,null);check(store.scene(id).revision()==3,"Stale history request rejected");
        send(a,na,EditorPayloads.Intent.OPERATION,id,3,new EditorOperation.CreateTrigger("区域",new EditorTransform(a.position(),0,0),null));
        var object=store.scene(id).objects().values().iterator().next();
        send(b,nb,EditorPayloads.Intent.OPERATION,id,4,new EditorOperation.RenameObject(object.id(),object.revision(),"欢迎区域"));
        send(a,na,EditorPayloads.Intent.UNDO,id,5,null);var reverted=store.scene(id).objects().get(object.id());
        check(reverted.name().equals("区域")&&reverted.revision()>object.revision(),"Shared object restore keeps identity with new target revision");
        send(a,na,EditorPayloads.Intent.OPERATION,id,6,new EditorOperation.RenameObject(object.id(),object.revision(),"old request"));
        check(store.scene(id).revision()==6,"Undo does not reactivate old target revision");
        var roundtrip=AuthoringSavedData.load(store.save(new net.minecraft.nbt.CompoundTag(),a.registryAccess()),a.registryAccess());
        check(roundtrip.scene(id).flags().equals(store.scene(id).flags()),"Scene v2 Chinese catalog persisted");
        var editing=a.position().add(1,0,1);source.getLevel().getChunkAt(net.minecraft.core.BlockPos.containing(editing));
        a.setPos(editing);a.setYRot(42);a.setXRot(10);
        send(a,na,EditorPayloads.Intent.PLAYTEST,null,0,null);
        check(!EditorSessionService.active(a)&&!a.isSpectator()&&!a.getData(EditorAttachments.RECOVERY).active(),"Playtest restores gameplay before closing acknowledgement");
        var gameplay=a.position().add(-1,0,-1);a.setPos(gameplay);
        send(a,null,EditorPayloads.Intent.OPEN,null,0,null);check(a.position().distanceTo(editing)<.001&&a.getYRot()==42,"F8 return restores trusted loaded editor pose");
        EditorSessionService.close(a);check(a.position().distanceTo(gameplay)<.001&&!a.isSpectator(),"Returning to edit does not overwrite the new gameplay return point");
        send(a,null,EditorPayloads.Intent.OPEN,null,0,null);
        na=nonce(a);send(a,na,EditorPayloads.Intent.SELECT_SCENE,id,6,null);a.setPos(new Vec3(29_000_000,80,29_000_000));send(a,na,EditorPayloads.Intent.PLAYTEST,null,0,null);
        var fallback=a.position();send(a,null,EditorPayloads.Intent.OPEN,null,0,null);
        check(a.position().distanceTo(fallback)<.001,"Unloaded editor pose falls back without chunk force load");
        EditorSessionService.close(a);
        // Restore the original session handle for the remaining legacy fixture checks.
        send(a,null,EditorPayloads.Intent.OPEN,null,0,null);
        return nonce(a);
    }
    private static int persisted(CommandSourceStack source) {
        check(AuthoringSavedData.get(source.getServer()).scenes().stream().anyMatch(s -> s.id().getPath().startsWith("editor_") && s.objects().size() == 2 && s.revision() == 2), "Saved Scene and UUIDs survive server restart");
        check(AuthoringSavedData.get(source.getServer()).scenes().stream().anyMatch(s -> s.dataVersion()==2&&s.flags().values().stream().anyMatch(f->f.name().equals("已见过教程"))), "Scene v2 named catalog survives actual server restart");
        source.sendSuccess(() -> Component.literal("Editor persisted regression PASS; Scene v2 catalog, Scene and object UUIDs restored"), false);
        return 1;
    }
    private EditorRegression() {}
}
