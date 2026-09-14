package dev.maplesadventure.progression;

/** Small fault-testable commit boundary. The service validates authorization and price first. */
public final class AttributeUpgradeTransaction {
    public interface Writer {
        void experience(int points);
        void attributes(PlayerAttributeState state);
        void sync();
    }
    public static void commit(Writer writer, int oldXp, PlayerAttributeState before,
                              int newXp, PlayerAttributeState after) {
        try {
            writer.experience(newXp);
            writer.attributes(after.cleanCopy());
            writer.sync();
        } catch (RuntimeException failure) {
            // Try every rollback step even if one setter fails; preserve the original exception.
            try { writer.experience(oldXp); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            try { writer.attributes(before.cleanCopy()); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            try { writer.sync(); } catch (RuntimeException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        }
    }
    private AttributeUpgradeTransaction() {}
}
