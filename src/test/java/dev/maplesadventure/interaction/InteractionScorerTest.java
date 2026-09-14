package dev.maplesadventure.interaction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class InteractionScorerTest {
    @Test
    void distanceRemainsTheLargestDefaultSignal() {
        double nearSideTarget = InteractionScorer.calculate(
                1.0D, 5.0D, 0.2D, 0.0D, 0.0D, 0.50D, 0.32D, 0.18D
        );
        double farCenteredTarget = InteractionScorer.calculate(
                4.5D, 5.0D, 1.0D, 1.0D, 0.0D, 0.50D, 0.32D, 0.18D
        );
        assertTrue(nearSideTarget > farCenteredTarget);
    }

    @Test
    void bodyFacingOutweighsCameraCenterByDefault() {
        double bodyFacing = InteractionScorer.calculate(
                2.0D, 5.0D, 1.0D, 0.0D, 0.0D, 0.50D, 0.32D, 0.18D
        );
        double cameraFacing = InteractionScorer.calculate(
                2.0D, 5.0D, 0.0D, 1.0D, 0.0D, 0.50D, 0.32D, 0.18D
        );
        assertTrue(bodyFacing > cameraFacing);
    }

}
