package dev.maplesadventure.flask;

/** A committed resource amount, distributed once over six ticks, independently of drink animation. */
public final class FlaskRecoveryTimeline {
    private final double total;
    private int elapsed;
    private double delivered;

    public FlaskRecoveryTimeline(double total) {
        if (!Double.isFinite(total) || total < 0 || total > 1_000_000)
            throw new IllegalArgumentException("Invalid flask restoration");
        this.total = total;
    }

    public double nextAmount() {
        if (finished()) return 0;
        elapsed++;
        double target = elapsed == FlaskRules.RECOVERY_TICKS ? total
                : total * elapsed / FlaskRules.RECOVERY_TICKS;
        double amount = target - delivered;
        delivered = target;
        return amount;
    }

    public boolean finished() { return elapsed >= FlaskRules.RECOVERY_TICKS; }
}
