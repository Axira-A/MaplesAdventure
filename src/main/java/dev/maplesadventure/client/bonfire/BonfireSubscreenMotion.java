package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireTransitionMath;
import java.util.function.LongSupplier;

/** Shared, frame-rate-independent motion for Bonfire child pages. No gameplay/session ownership. */
public final class BonfireSubscreenMotion {
    public static final double DURATION_MS = 320;
    public static final float DISTANCE = 24;
    private final LongSupplier time;
    private final long opened;
    private boolean exiting, completed;
    private long exitStarted;
    private float exitAlpha, exitOffset;

    public BonfireSubscreenMotion() { this(System::nanoTime); }
    public BonfireSubscreenMotion(LongSupplier time) { this.time = time; opened = time.getAsLong(); }
    private double elapsed(long since) { return (time.getAsLong() - since) / 1_000_000.0; }
    public float alpha() {
        return exiting ? exitAlpha * (1 - BonfireTransitionMath.menuAlpha(elapsed(exitStarted)))
                : BonfireTransitionMath.menuAlpha(elapsed(opened));
    }
    public float offset() {
        return exiting ? exitOffset + (DISTANCE - exitOffset) * BonfireTransitionMath.menuAlpha(elapsed(exitStarted))
                : DISTANCE * (1 - alpha());
    }
    public boolean interactive() { return !exiting && elapsed(opened) >= DURATION_MS; }
    public boolean exiting() { return exiting; }
    public void beginExit() {
        if (exiting) return;
        exitAlpha = alpha(); exitOffset = offset(); exitStarted = time.getAsLong(); exiting = true;
    }
    public boolean claimCompletion() {
        if (!exiting || completed || elapsed(exitStarted) < DURATION_MS) return false;
        completed = true;
        return true;
    }
}
