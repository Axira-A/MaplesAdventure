package dev.maplesadventure.multiplayer.phase.loot;

import dev.maplesadventure.multiplayer.phase.mob.ModPhaseAttachments;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import net.minecraft.world.entity.ExperienceOrb;

/** Server-thread scoped provenance used only while Vanilla splits and creates an XP award. */
public final class PhaseExperienceSpawnContext {
    private static final ThreadLocal<Deque<Entry>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    public static Scope enter(PhaseObjectState state) {
        STACK.get().push(new Entry(state));
        return new Scope(true);
    }

    /** ExperienceOrb.award always establishes a scope so untagged awards cannot merge into phased orbs. */
    public static Scope enterSharedIfAbsent() {
        if (!STACK.get().isEmpty()) return new Scope(false);
        STACK.get().push(new Entry(null));
        return new Scope(true);
    }

    public static boolean isScoped() { return !STACK.get().isEmpty(); }

    public static Optional<PhaseObjectState> current() {
        Deque<Entry> stack = STACK.get();
        return stack.isEmpty() ? Optional.empty() : Optional.ofNullable(stack.peek().state);
    }

    public static boolean canMergeWith(ExperienceOrb existing) {
        PhaseObjectState current = current().orElse(null);
        PhaseObjectState other = existing.getExistingData(ModPhaseAttachments.PHASE_OBJECT).orElse(null);
        return current == null ? other == null : other != null && current.canMergeWith(other);
    }

    public static final class Scope implements AutoCloseable {
        private final boolean ownsEntry;
        private boolean closed;

        private Scope(boolean ownsEntry) { this.ownsEntry = ownsEntry; }

        @Override
        public void close() {
            if (closed || !ownsEntry) return;
            closed = true;
            Deque<Entry> stack = STACK.get();
            if (!stack.isEmpty()) stack.pop();
            if (stack.isEmpty()) STACK.remove();
        }
    }

    private record Entry(PhaseObjectState state) {}
    private PhaseExperienceSpawnContext() {}
}
