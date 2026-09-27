package dev.maplesadventure.integration.soulscombathud;

/** One outstanding client intent; never predicts charges or starts a second server action. */
public final class FlaskQuickUseState {
    private int kind, previous = -1, target = -1, waitTicks;
    private boolean acknowledged, finished;
    public boolean busy() { return kind != 0; }
    public boolean begin(int kind, int previous, int target) {
        if (busy() || kind < 1 || kind > 2 || previous < 0 || previous > 8 || target < 0 || target > 8) return false;
        this.kind = kind; this.previous = previous; this.target = target;
        waitTicks = 0; acknowledged = false; finished = false;
        return true;
    }
    public void snapshot(int usingKind) {
        if (!busy()) return;
        if (usingKind == kind) acknowledged = true;
        else if (usingKind == 0) finished = true; // Explicit rejection or confirmed completion/cancel.
    }
    public int tick(int currentSlot, boolean useKeyHeld) {
        if (!busy()) return -1;
        if (currentSlot != target) { clear(); return -1; } // Manual change revokes return ownership.
        if (!acknowledged && ++waitTicks >= 100) finished = true;
        if (!finished || useKeyHeld) return -1;
        int restore = previous == target ? -1 : previous;
        clear(); return restore;
    }
    public void clear() { kind = 0; previous = target = -1; acknowledged = finished = false; waitTicks = 0; }
}
