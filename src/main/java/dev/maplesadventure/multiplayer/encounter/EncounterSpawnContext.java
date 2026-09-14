package dev.maplesadventure.multiplayer.encounter;

/** Narrow server-thread scope distinguishing MaplesAdventure encounter creation from automatic spawning. */
public final class EncounterSpawnContext implements AutoCloseable {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    public static EncounterSpawnContext enter() {
        DEPTH.set(DEPTH.get() + 1);
        return new EncounterSpawnContext();
    }
    public static boolean active() { return DEPTH.get() > 0; }
    @Override public void close() {
        int next = DEPTH.get() - 1;
        if (next <= 0) DEPTH.remove(); else DEPTH.set(next);
    }
    private EncounterSpawnContext() {}
}
