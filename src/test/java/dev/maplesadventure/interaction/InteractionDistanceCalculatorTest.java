package dev.maplesadventure.interaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class InteractionDistanceCalculatorTest {
    private static final double EPSILON = 1.0E-9D;
    private static final double PLAYER_MIN_X = 0.0D;
    private static final double PLAYER_MIN_Y = 0.0D;
    private static final double PLAYER_MIN_Z = 0.0D;
    private static final double PLAYER_MAX_X = 0.6D;
    private static final double PLAYER_MAX_Y = 1.8D;
    private static final double PLAYER_MAX_Z = 0.6D;

    @Test
    void touchingTargetSurfaceHasZeroDistance() {
        assertEquals(0.0D, distanceTo(0.6D, 0.0D, 0.0D, 1.6D, 1.0D, 1.0D), EPSILON);
    }

    @Test
    void horizontalGapUsesSurfaceDistanceAtStrictBoundary() {
        assertEquals(1.25D, distanceTo(1.85D, 0.0D, 0.0D, 2.85D, 1.0D, 1.0D), EPSILON);
        assertEquals(1.26D, distanceTo(1.86D, 0.0D, 0.0D, 2.86D, 1.0D, 1.0D), EPSILON);
    }

    @Test
    void lowShapeTouchingFeetIsNotMeasuredFromEyeHeight() {
        assertEquals(0.0D, distanceTo(0.2D, -0.125D, 0.2D, 0.4D, 0.0D, 0.4D), EPSILON);
    }

    @Test
    void diagonalDistanceUsesAllThreeAxes() {
        assertEquals(Math.sqrt(2.0D), distanceTo(1.6D, 0.0D, 1.6D, 2.0D, 1.0D, 2.0D), EPSILON);
    }

    private static double distanceTo(
            double minX, double minY, double minZ,
            double maxX, double maxY, double maxZ
    ) {
        return InteractionGeometry.distanceBetweenBoxes(
                PLAYER_MIN_X, PLAYER_MIN_Y, PLAYER_MIN_Z,
                PLAYER_MAX_X, PLAYER_MAX_Y, PLAYER_MAX_Z,
                minX, minY, minZ,
                maxX, maxY, maxZ
        );
    }
}
