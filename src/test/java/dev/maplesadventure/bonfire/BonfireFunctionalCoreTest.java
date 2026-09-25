package dev.maplesadventure.bonfire;

import dev.maplesadventure.api.bonfire.*;
import dev.maplesadventure.bonfire.network.BonfirePayloads;
import dev.maplesadventure.progression.runtime.ResourceRestoreResult;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BonfireFunctionalCoreTest {
    @Test void phaseResetNeverReusesAnIndividuallyResetEncounterGeneration() {
        var data = new dev.maplesadventure.multiplayer.encounter.EncounterSavedData();
        var a = dev.maplesadventure.multiplayer.phase.PhaseId.solo(UUID.randomUUID());
        var b = dev.maplesadventure.multiplayer.phase.PhaseId.solo(UUID.randomUUID());
        var definition = new dev.maplesadventure.multiplayer.encounter.EncounterDefinition(id("common"),
                net.minecraft.world.level.Level.OVERWORLD,
                dev.maplesadventure.multiplayer.encounter.EncounterType.COMMON,
                net.minecraft.world.phys.Vec3.ZERO, 16, List.of());
        data.state(a, definition).setGeneration(7);
        data.state(b, definition).setGeneration(900);
        assertEquals(8, data.incrementGeneration(a));
        assertEquals(9, data.incrementGeneration(a));
        assertEquals(900, data.state(b, definition).generation());
        assertEquals(0, data.generation(b));
    }
    private static ResourceLocation id(String path) { return ResourceLocation.parse("test:" + path); }
    private static MaplesBonfireFeatureHandler feature(String name, int order, boolean available, Runnable run) {
        return new MaplesBonfireFeatureHandler() {
            public ResourceLocation id() { return BonfireFunctionalCoreTest.id(name); }
            public String translationKey() { return "test." + name; }
            public int order() { return order; }
            public boolean isAvailable(MaplesBonfireContext c) { return available; }
            public void execute(MaplesBonfireContext c) { run.run(); }
        };
    }
    private static MaplesBonfireRestResetParticipant reset(String name, int priority, Runnable run) {
        return new MaplesBonfireRestResetParticipant() {
            public ResourceLocation id() { return BonfireFunctionalCoreTest.id(name); }
            public int priority() { return priority; }
            public void reset(MaplesBonfireContext c) { run.run(); }
        };
    }
    @Test void featuresAreIntersectionSortedAndRevalidated() {
        var registry = new BonfireExtensionRegistry();
        List<String> calls = new ArrayList<>();
        registry.register(feature("z", 10, true, () -> calls.add("z")));
        registry.register(feature("a", 10, true, () -> calls.add("a")));
        registry.register(feature("first", 0, true, () -> calls.add("first")));
        registry.register(feature("hidden", -10, false, () -> fail("Unavailable action executed")));
        registry.register(feature("unconfigured", -20, true, () -> fail("Unconfigured action executed")));
        var configured = Set.of(id("z"), id("a"), id("first"), id("hidden"), id("unknown"));
        assertEquals(List.of(id("first"), id("a"), id("z")),
                registry.available(configured, null).stream().map(BonfireExtensionRegistry.Feature::id).toList());
        assertFalse(registry.execute(id("unknown"), configured, null));
        assertFalse(registry.execute(id("unconfigured"), configured, null));
        assertFalse(registry.execute(id("hidden"), configured, null));
        assertTrue(registry.execute(id("a"), configured, null));
        assertEquals(List.of("a"), calls);
        assertThrows(IllegalArgumentException.class, () -> registry.register(feature("a", 1, true, () -> {})));
        registry.freeze();
        assertThrows(IllegalStateException.class, () -> registry.register(feature("late", 1, true, () -> {})));
    }
    @Test void resetOrderingAndExceptionIsolation() {
        var registry = new BonfireExtensionRegistry();
        List<String> calls = new ArrayList<>();
        registry.register(reset("z", 4, () -> calls.add("z")));
        registry.register(reset("a", 4, () -> calls.add("a")));
        registry.register(reset("core", -100, () -> calls.add("core")));
        registry.register(reset("broken", 0, () -> { calls.add("broken"); throw new IllegalStateException("test"); }));
        assertThrows(IllegalArgumentException.class, () -> registry.register(reset("a", 0, () -> {})));
        assertEquals(Set.of(id("broken")), registry.reset(null));
        assertEquals(List.of("core", "broken", "a", "z"), calls);
    }
    @Test void failingFeatureCannotEscapeBoundary() {
        var registry = new BonfireExtensionRegistry();
        registry.register(feature("broken", 0, true, () -> { throw new IllegalStateException("test"); }));
        assertFalse(registry.execute(id("broken"), Set.of(id("broken")), null));
        registry.register(new MaplesBonfireFeatureHandler() {
            public ResourceLocation id() { return BonfireFunctionalCoreTest.id("bad_query"); }
            public String translationKey() { return "test.bad_query"; }
            public int order() { return 0; }
            public boolean isAvailable(MaplesBonfireContext c) { throw new IllegalStateException("test"); }
            public void execute(MaplesBonfireContext c) { fail(); }
        });
        assertTrue(registry.available(Set.of(id("bad_query")), null).isEmpty());
    }
    @Test void legacyAndUnknownFeaturePersistence() {
        ListTag old = new ListTag();
        old.add(StringTag.valueOf("level_up"));
        old.add(StringTag.valueOf("flask_allocation"));
        old.add(StringTag.valueOf("absent_addon:craft"));
        old.add(StringTag.valueOf("INVALID VALUE"));
        var migrated = BonfireFeatureIds.load(old);
        assertEquals(Set.of(MaplesBonfireFeatures.LEVEL_UP, MaplesBonfireFeatures.FLASK_ALLOCATION,
                ResourceLocation.parse("absent_addon:craft")), migrated);
        assertEquals(migrated, BonfireFeatureIds.load(BonfireFeatureIds.save(migrated)));
        assertNull(BonfireFeatureIds.parse("a".repeat(129)));
        var registry = new BonfireExtensionRegistry();
        assertTrue(registry.available(migrated, null).isEmpty());
    }
    @Test void publicViewsAreDetached() {
        var pos = new BlockPos.MutableBlockPos(1, 2, 3);
        var ref = new MaplesBonfireRef(id("dimension"), pos, UUID.randomUUID());
        pos.set(9, 9, 9);
        assertEquals(new BlockPos(1, 2, 3), ref.position());
        Set<ResourceLocation> features = new HashSet<>(Set.of(id("craft")));
        var view = new MaplesBonfireView(ref, "Fire", features);
        features.clear();
        assertEquals(Set.of(id("craft")), view.configuredFeatures());
        assertThrows(UnsupportedOperationException.class, () -> view.configuredFeatures().clear());
    }
    @Test void onlyExactStaleRestPointIsForgotten() {
        var state = new PlayerBonfireState();
        var ref = new BonfireRef(id("dimension"), BlockPos.ZERO, UUID.randomUUID());
        state.activate(ref); state.rest(ref, 73.5f);
        state.clearLastRested(new BonfireRef(ref.dimension(), ref.pos(), UUID.randomUUID()));
        assertEquals(73.5f, state.lastRested().orElseThrow().yaw());
        var copy = new PlayerBonfireState();
        copy.deserializeNBT(null, state.serializeNBT(null));
        assertEquals(state.lastRested(), copy.lastRested());
        copy.clearLastRested(ref);
        assertTrue(copy.lastRested().isEmpty());
        assertTrue(copy.isActivated(ref));
        assertTrue(state.lastRested().isPresent());
    }
    @Test void maximumVerificationUsesFractionalRuntimeValue() {
        var result = ResourceRestoreResult.Value.measured(3, 35.5, 35.5);
        assertEquals(ResourceRestoreResult.Status.RESTORED, result.status());
        assertEquals(ResourceRestoreResult.Status.FAILED, ResourceRestoreResult.Value.measured(3, 35, 35.5).status());
        assertEquals(ResourceRestoreResult.Status.FAILED, ResourceRestoreResult.Value.measured(3, Double.NaN, 35.5).status());
    }
    @Test void genericActionAndViewRoundTripWithBounds() {
        var b = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            var action = new BonfirePayloads.Action(UUID.randomUUID(), BonfirePayloads.ActionType.SELECT_FEATURE, id("craft"));
            BonfirePayloads.Action.CODEC.encode(b, action);
            assertEquals(action, BonfirePayloads.Action.CODEC.decode(b));
            var view = new BonfirePayloads.View(action.nonce(), BonfireSessionState.RESTING, "Fire",
                    List.of(new BonfirePayloads.MenuEntry(id("craft"), "test.craft", 42)), 43, 14, 20, BlockPos.ZERO);
            BonfirePayloads.View.CODEC.encode(b, view);
            assertEquals(view, BonfirePayloads.View.CODEC.decode(b));
            assertThrows(IllegalArgumentException.class, () -> new BonfirePayloads.Action(action.nonce(),
                    BonfirePayloads.ActionType.SELECT_FEATURE, null));
            b.clear(); b.writeUUID(action.nonce()); b.writeEnum(BonfireSessionState.RESTING); b.writeUtf("Fire", 64); b.writeVarInt(65);
            assertThrows(IllegalArgumentException.class, () -> BonfirePayloads.View.CODEC.decode(b));
        } finally { b.release(); }
    }
    @Test void all4096ProgressRefsFitClientboundLimit() {
        for (var dimension : List.of(ResourceLocation.parse("minecraft:overworld"),
                ResourceLocation.parse("test:" + "a".repeat(123)))) {
            var b = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
            try {
                var refs = new ArrayList<BonfireRef>();
                for (int i = 0; i < 4096; i++) refs.add(new BonfireRef(dimension, new BlockPos(i, 64, 0), new UUID(0, i)));
                var packet = new BonfirePayloads.Progress(refs);
                BonfirePayloads.Progress.CODEC.encode(b, packet);
                int expected = dimension.toString().length() == 128 ? 630786 : 180226;
                assertEquals(expected, b.readableBytes());
                // NeoForge 1.21.1 clientbound custom payload maximum is 1 MiB; allow payload ID overhead as well.
                assertTrue(b.readableBytes() + 256 < 1048576);
                assertEquals(packet, BonfirePayloads.Progress.CODEC.decode(b));
                System.out.println("Bonfire progress 4096 refs, dimension length=" + dimension.toString().length() + ", bytes=" + expected);
            } finally { b.release(); }
        }
    }
}
