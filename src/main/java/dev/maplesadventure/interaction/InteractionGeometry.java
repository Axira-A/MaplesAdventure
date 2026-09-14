package dev.maplesadventure.interaction;

/** Pure geometry helpers kept independent from the Minecraft runtime for boundary tests. */
public final class InteractionGeometry {
    public static double distanceBetweenBoxes(
            double firstMinX, double firstMinY, double firstMinZ,
            double firstMaxX, double firstMaxY, double firstMaxZ,
            double secondMinX, double secondMinY, double secondMinZ,
            double secondMaxX, double secondMaxY, double secondMaxZ
    ) {
        double x = Math.max(Math.max(firstMinX - secondMaxX, secondMinX - firstMaxX), 0.0D);
        double y = Math.max(Math.max(firstMinY - secondMaxY, secondMinY - firstMaxY), 0.0D);
        double z = Math.max(Math.max(firstMinZ - secondMaxZ, secondMinZ - firstMaxZ), 0.0D);
        return Math.sqrt(x * x + y * y + z * z);
    }

    private InteractionGeometry() {
    }
}
