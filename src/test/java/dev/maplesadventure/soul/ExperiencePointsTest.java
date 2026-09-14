package dev.maplesadventure.soul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExperiencePointsTest {
    @Test
    void matchesVanillaCumulativeLevelBoundaries() {
        assertEquals(0, ExperiencePoints.totalToReachLevel(0));
        assertEquals(7, ExperiencePoints.totalToReachLevel(1));
        assertEquals(352, ExperiencePoints.totalToReachLevel(16));
        assertEquals(394, ExperiencePoints.totalToReachLevel(17));
        assertEquals(1395, ExperiencePoints.totalToReachLevel(30));
        assertEquals(1507, ExperiencePoints.totalToReachLevel(31));
        assertEquals(1628, ExperiencePoints.totalToReachLevel(32));
    }

    @Test
    void totalPointsSelectTheContainingLevel() {
        int[] samples = {0, 1, 6, 7, 351, 352, 393, 394, 1395, 1506, 1507, 1627, 1628, 12_000, 1_000_000};
        for (int points : samples) {
            int level = ExperiencePoints.levelForTotal(points);
            assertTrue(ExperiencePoints.totalToReachLevel(level) <= points, "lower boundary for " + points);
            assertTrue(ExperiencePoints.totalToReachLevel(level + 1) > points, "upper boundary for " + points);
        }
    }
}
