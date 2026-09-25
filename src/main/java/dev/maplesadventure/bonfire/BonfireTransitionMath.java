package dev.maplesadventure.bonfire;

/** Shared timing math; gameplay commit remains server-owned. */
public final class BonfireTransitionMath {
    public static float menuAlpha(double elapsedMillis) {
        double t = Math.clamp(elapsedMillis / 320.0, 0.0, 1.0);
        return (float) (t * t * (3.0 - 2.0 * t));
    }

    public static int withOpacity(int color, float opacity) {
        int alpha = Math.round((color >>> 24) * Math.clamp(opacity, 0, 1));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public static float veilOpacity(double horizontalFraction) {
        double t = Math.clamp(horizontalFraction, 0.0, 1.0);
        return (float) (1.0 - t * t * (3.0 - 2.0 * t));
    }
    public static boolean shouldCommit(BonfireSessionState state, long elapsedTicks,
                                       int commitTick, boolean alreadyCommitted) {
        return state == BonfireSessionState.SITTING_DOWN && !alreadyCommitted
                && commitTick > 0 && elapsedTicks >= commitTick;
    }

    public static float fadeAlpha(double elapsedTicks, int durationTicks,
                                  int commitTick, int fadeInTick) {
        if (durationTicks <= 0 || commitTick <= 0 || fadeInTick <= commitTick
                || fadeInTick >= durationTicks) return 0;
        // Let the first part of sitting be visible, then briefly cover the authoritative rest/reset.
        double start = Math.min(6, commitTick / 2.0);
        double end = Math.min(durationTicks, fadeInTick + 8);
        if (elapsedTicks < commitTick) return smooth((elapsedTicks - start) / (commitTick - start));
        if (elapsedTicks < fadeInTick) return 1;
        return 1 - smooth((elapsedTicks - fadeInTick) / (end - fadeInTick));
    }

    private static float smooth(double value) {
        double t = Math.clamp(value, 0, 1);
        return (float) (t * t * (3 - 2 * t));
    }

    private BonfireTransitionMath() {}
}
