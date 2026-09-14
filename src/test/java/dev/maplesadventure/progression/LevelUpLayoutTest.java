package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.client.LevelUpLayout;
import org.junit.jupiter.api.Test;

class LevelUpLayoutTest {
    @Test void supportedResolutionAndGuiScalePairsRemainInBounds() {
        int[][] resolutions = {{1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] resolution : resolutions) for (int scale : new int[]{2, 3, 4}) {
            int width = resolution[0] / scale, height = resolution[1] / scale;
            LevelUpLayout layout = LevelUpLayout.calculate(width, height);
            assertInside(layout.panel(), width, height);
            assertNested(layout.summary(), layout.panel());
            assertNested(layout.attributes(), layout.panel());
            assertNested(layout.derived(), layout.panel());
            assertNested(layout.footer(), layout.panel());
            assertTrue(layout.rowHeight() >= 10);
            assertTrue(layout.attributes().height() >= layout.rowHeight() * 8);
            if (!layout.compact()) {
                assertNested(layout.description(), layout.panel());
                assertTrue(layout.attributes().right() < layout.derived().x());
            }
            assertTrue(layout.attributes().bottom() <= layout.footer().y());
        }
    }
    @Test void compactAndWideModesAreDeterministic() {
        assertTrue(LevelUpLayout.calculate(320, 180).compact());
        assertFalse(LevelUpLayout.calculate(427, 240).compact());
        assertTrue(LevelUpLayout.calculate(427, 240).dense());
        assertFalse(LevelUpLayout.calculate(640, 360).compact());
        assertFalse(LevelUpLayout.calculate(640, 360).dense());
        assertTrue(LevelUpLayout.calculate(640, 360).wide());
        assertTrue(LevelUpLayout.calculate(960, 540).wide());
    }
    private static void assertInside(LevelUpLayout.Box box, int width, int height) {
        assertTrue(box.x() >= 0 && box.y() >= 0 && box.right() <= width && box.bottom() <= height,
                () -> box + " outside " + width + 'x' + height);
    }
    private static void assertNested(LevelUpLayout.Box child, LevelUpLayout.Box parent) {
        assertTrue(child.x() >= parent.x() && child.y() >= parent.y()
                && child.right() <= parent.right() && child.bottom() <= parent.bottom(),
                () -> child + " outside " + parent);
    }
}
