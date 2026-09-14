package dev.maplesadventure.interaction;

import dev.maplesadventure.config.InteractionConfig;

public final class InteractionScorer {
    public static double calculate(
            double distance,
            double candidateRange,
            double playerFacingAlignment,
            double cameraAlignment,
            InteractionPriority priority
    ) {
        return calculate(
                distance,
                candidateRange,
                playerFacingAlignment,
                cameraAlignment,
                priority.scoreBonus(),
                InteractionConfig.DISTANCE_WEIGHT.get(),
                InteractionConfig.FACING_WEIGHT.get(),
                InteractionConfig.SCREEN_CENTER_WEIGHT.get()
        );
    }

    static double calculate(
            double distance,
            double candidateRange,
            double playerFacingAlignment,
            double cameraAlignment,
            double priorityBonus,
            double distanceWeight,
            double facingWeight,
            double screenCenterWeight
    ) {
        double safeRange = Math.max(0.001D, candidateRange);
        double normalizedDistance = 1.0D - clamp01(distance / safeRange);
        double normalizedFacing = (clampSigned(playerFacingAlignment) + 1.0D) * 0.5D;
        double normalizedCamera = (clampSigned(cameraAlignment) + 1.0D) * 0.5D;
        return normalizedDistance * distanceWeight
                + normalizedFacing * facingWeight
                + normalizedCamera * screenCenterWeight
                + priorityBonus;
    }

    private static double clamp01(double value) {
        return Math.max(0.0D, Math.min(1.0D, value));
    }

    private static double clampSigned(double value) {
        return Math.max(-1.0D, Math.min(1.0D, value));
    }

    private InteractionScorer() {
    }
}
