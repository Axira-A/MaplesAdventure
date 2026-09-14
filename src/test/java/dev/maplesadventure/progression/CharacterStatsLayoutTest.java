package dev.maplesadventure.progression;

import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.client.CharacterStatsLayout;
import dev.maplesadventure.progression.client.LevelUpLayout;
import org.junit.jupiter.api.Test;

class CharacterStatsLayoutTest {
    @Test void detailedPanelFitsEveryRequiredResolutionAndScale() {
        int[][] resolutions = {{1280, 720}, {1920, 1080}, {2560, 1440}};
        for (int[] resolution : resolutions) for (int scale : new int[]{2, 3, 4}) {
            int width = resolution[0] / scale, height = resolution[1] / scale;
            CharacterStatsLayout layout = CharacterStatsLayout.calculate(width, height);
            assertInside(layout.panel(), width, height);
            assertNested(layout.list(), layout.panel());
            assertNested(layout.footer(), layout.panel());
            assertTrue(layout.list().height() >= 54);
            assertTrue(layout.list().bottom() <= layout.footer().y());
        }
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
