package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.api.flask.FlaskSnapshot;
import dev.maplesadventure.integration.soulscombathud.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SoulsFlaskCompatibilityTest {
    @Test void unsupportedVersionsDoNotApplyHudMixins() {
        assertTrue(SoulsHudCompatibility.supportedVersion("1.3.1"));
        assertTrue(SoulsHudCompatibility.supportedVersion("1.3.1+mc1.21.1-neoforge"));
        for (String version : List.of("1.2.3", "1.3.10", "1.3.2", "2.0.0", ""))
            assertFalse(SoulsHudCompatibility.supportedVersion(version));
        assertFalse(SoulsHudCompatibility.supportedVersion(null));
    }
    @Test void missingChangedOrStaticMismatchMethodsFailClosedBeforeInjection() {
        SoulsHudCompatibility.CONTRACTS.forEach((target, contracts) -> {
            var node = new org.objectweb.asm.tree.ClassNode();
            for (var contract : contracts) node.methods.add(new org.objectweb.asm.tree.MethodNode(
                    contract.isStatic() ? org.objectweb.asm.Opcodes.ACC_STATIC : 0,
                    contract.name(), contract.descriptor(), null, null));
            assertTrue(SoulsHudCompatibility.matches(target, node));
            var method = node.methods.getFirst();
            method.access ^= org.objectweb.asm.Opcodes.ACC_STATIC;
            assertFalse(SoulsHudCompatibility.matches(target, node));
            method.access ^= org.objectweb.asm.Opcodes.ACC_STATIC;
            String descriptor = method.desc; method.desc = "()Ljava/lang/String;";
            assertFalse(SoulsHudCompatibility.matches(target, node));
            method.desc = descriptor; node.methods.removeFirst();
            assertFalse(SoulsHudCompatibility.matches(target, node));
        });
        assertFalse(SoulsHudCompatibility.matches("unknown", new org.objectweb.asm.tree.ClassNode()));
    }
    @Test void selectedFlaskOnlyAndBlacklistWins() {
        assertTrue(FlaskHudPolicy.enabled(FlaskKind.CRIMSON, false, false));
        assertTrue(FlaskHudPolicy.enabled(FlaskKind.ASHEN, true, false));
        assertFalse(FlaskHudPolicy.enabled(FlaskKind.ASHEN, false, false));
        for (boolean mana : List.of(true, false)) {
            assertFalse(FlaskHudPolicy.enabled(null, mana, false));
            for (FlaskKind kind : FlaskKind.values()) assertFalse(FlaskHudPolicy.enabled(kind, mana, true));
        }
        var state = new FlaskSnapshot(8, 3, 5, 0, 3, 2);
        assertEquals("0 / 5", FlaskHudPolicy.label(state, FlaskKind.CRIMSON));
        assertEquals("2 / 3", FlaskHudPolicy.label(state, FlaskKind.ASHEN));
    }
    @Test void counterHugsLeftEdgeAndNeverExtendsBelowFrame() {
        var counter = FlaskHudLayout.counter(0, 18, 34, 9);
        assertEquals(-49, counter.x()); assertEquals(-15, counter.right()); assertEquals(33, counter.bottom());
        for (float scale : new float[]{.5F, 1, 2, 4})
            assertTrue(FlaskHudLayout.fits(counter, scale, 240, 100, 854, 480));
        assertFalse(FlaskHudLayout.fits(counter, 4, 80, 100, 854, 480));
    }
    @Test void defaultPreviewsNameAndOffhandStayUnchangedAndClearOfCounter() {
        var count = FlaskHudLayout.counter(0, 18, 34, 9);
        // Actual 1.3.1 default preview positions: observe, never relocate them.
        var row = List.of(new FlaskHudLayout.Rect(16, 22, 14, 14),
                new FlaskHudLayout.Rect(34, 22, 14, 14), new FlaskHudLayout.Rect(52, 22, 14, 14));
        var offhand = new FlaskHudLayout.Rect(-40, -16, 24, 32);
        var name = new FlaskHudLayout.Rect(-12, 38, 100, 9);
        for (var preview : row) assertFalse(preview.intersects(count, 3));
        assertFalse(offhand.intersects(count, 3)); assertFalse(name.intersects(count, 3));
    }
    @Test void customLayoutCollisionAndClippingCanBeDetectedWithoutMovingOtherElements() {
        var counter = FlaskHudLayout.counter(10, 25, 38, 9);
        var overlappingPreview = new FlaskHudLayout.Rect(-30, 30, 14, 14);
        assertTrue(overlappingPreview.intersects(counter, 1));
        assertFalse(FlaskHudLayout.fits(counter, 1, 20, 20, 320, 240));
        assertTrue(FlaskHudLayout.fits(counter, 1, 160, 120, 320, 240));
        assertFalse(FlaskHudLayout.fits(counter, 1, 160, 220, 320, 240));
    }
    @Test void oneIntentWaitsForServerFinishNotVanillaUseState() {
        var state = new FlaskQuickUseState();
        assertTrue(state.begin(1, 0, 3)); assertFalse(state.begin(1, 0, 3));
        for (int i = 0; i < 20; i++) assertEquals(-1, state.tick(3, false));
        state.snapshot(1);
        for (int i = 0; i < 130; i++) assertEquals(-1, state.tick(3, false));
        state.snapshot(1); // Effect frame charge snapshot is not another action.
        assertTrue(state.busy());
        state.snapshot(0); assertEquals(0, state.tick(3, false));
        assertEquals(-1, state.tick(3, false)); assertFalse(state.busy());
    }
    @Test void rejectionInterruptionAndTimeoutReturnOnce() {
        for (boolean acknowledged : List.of(false, true)) {
            var state = new FlaskQuickUseState(); state.begin(2, 2, 5);
            if (acknowledged) state.snapshot(2);
            state.snapshot(0); assertEquals(-1, state.tick(5, true));
            assertEquals(2, state.tick(5, false)); assertFalse(state.busy());
        }
        var state = new FlaskQuickUseState(); state.begin(1, 2, 5);
        for (int i = 0; i < 99; i++) assertEquals(-1, state.tick(5, false));
        assertEquals(2, state.tick(5, false));
    }
    @Test void manualSelectionAndLifecycleClearNeverStealSlotBack() {
        var state = new FlaskQuickUseState(); state.begin(1, 0, 3); state.snapshot(1);
        assertEquals(-1, state.tick(4, false)); assertFalse(state.busy());
        state.snapshot(0); assertEquals(-1, state.tick(3, false));
        state.begin(2, 0, 3); state.clear(); state.snapshot(2); state.snapshot(0);
        assertFalse(state.busy()); assertEquals(-1, state.tick(3, false));
        assertFalse(state.begin(1, 9, 3)); assertFalse(state.begin(1, 0, 9));
        assertTrue(state.begin(1, 3, 3)); state.snapshot(0); assertEquals(-1, state.tick(3, false));
        assertFalse(state.busy());
    }
}
