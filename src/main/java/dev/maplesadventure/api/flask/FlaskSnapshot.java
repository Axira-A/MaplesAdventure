package dev.maplesadventure.api.flask;

/** Detached immutable player-owned state; never an ItemStack component. */
public record FlaskSnapshot(int totalCapacity, int potencyLevel, int crimsonAllocated, int crimsonRemaining,
                            int ashenAllocated, int ashenRemaining) {
    public FlaskSnapshot {
        if (totalCapacity < 4 || totalCapacity > 14 || potencyLevel < 0 || potencyLevel > 12
                || crimsonAllocated < 0 || ashenAllocated < 0 || crimsonAllocated + ashenAllocated != totalCapacity
                || crimsonRemaining < 0 || crimsonRemaining > crimsonAllocated
                || ashenRemaining < 0 || ashenRemaining > ashenAllocated) throw new IllegalArgumentException("Invalid flask state");
    }
    public int remaining(FlaskKind kind) { return kind == FlaskKind.CRIMSON ? crimsonRemaining : ashenRemaining; }
    public int allocated(FlaskKind kind) { return kind == FlaskKind.CRIMSON ? crimsonAllocated : ashenAllocated; }
    public FlaskSnapshot refill() { return new FlaskSnapshot(totalCapacity, potencyLevel, crimsonAllocated, crimsonAllocated, ashenAllocated, ashenAllocated); }
    public FlaskSnapshot consume(FlaskKind kind) {
        if (remaining(kind) == 0) throw new IllegalStateException("Empty flask");
        return new FlaskSnapshot(totalCapacity, potencyLevel, crimsonAllocated,
                crimsonRemaining - (kind == FlaskKind.CRIMSON ? 1 : 0), ashenAllocated,
                ashenRemaining - (kind == FlaskKind.ASHEN ? 1 : 0));
    }
}
