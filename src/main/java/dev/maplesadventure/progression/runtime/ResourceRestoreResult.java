package dev.maplesadventure.progression.runtime;

/** Detached diagnostic result; no extra resource pool and no optional-mod types. */
public record ResourceRestoreResult(Value health, Value mana, Value stamina) {
    public enum Status { RESTORED, UNAVAILABLE, FAILED }
    public record Value(Status status, double before, double after, double maximum) {
        static Value unavailable() { return new Value(Status.UNAVAILABLE, 0, 0, 0); }
        static Value failed() { return new Value(Status.FAILED, 0, 0, 0); }
        public static Value measured(double before, double after, double maximum) {
            double tolerance = Math.max(1.0e-4, Math.abs(maximum) * 1.0e-6);
            boolean full = Double.isFinite(after) && Double.isFinite(maximum) && maximum >= 0
                    && Math.abs(after - maximum) <= tolerance;
            return new Value(full ? Status.RESTORED : Status.FAILED, before, after, maximum);
        }
    }
}
