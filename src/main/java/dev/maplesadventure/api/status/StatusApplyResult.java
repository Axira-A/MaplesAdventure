package dev.maplesadventure.api.status;

import java.util.Optional;

/**
 * Immutable operation outcome. Invalid target/context failures have empty snapshots.
 *
     * @param outcome reason or success
 *
     * @param before snapshot before the operation when available
 *
     * @param after snapshot after the operation when available, including lethal procs
 */
public record StatusApplyResult(Outcome outcome, Optional<StatusView> before, Optional<StatusView> after) {
    /** Stable outcome names; clients cannot submit these as gameplay authority. */
    public enum Outcome {
        /** Accumulation accepted. */ APPLIED,
        /** One proc was committed. */ PROCCED,
        /** Target is immune. */ IMMUNE,
        /** Timed status is already active. */ ALREADY_ACTIVE,
        /** Phase relationship forbids this source. */ PHASE_DENIED,
        /** Target is null, removed or dead. */ INVALID_TARGET,
        /** Amount is non-finite, non-positive or above 100000. */ INVALID_AMOUNT,
        /** Unknown/null status. */ UNSUPPORTED_STATUS,
        /** Source no longer valid or belongs to another level. */ INVALID_SOURCE,
        /** Client-side call rejected. */ NOT_SERVER,
        /** Call was not on the server thread. */ WRONG_THREAD,
        /** Mutation from an API notification/provider callback rejected. */ REENTRANT,
        /** Requested state was cleared. */ CLEARED
    }
    /** Validates required result fields. */
    public StatusApplyResult {
        java.util.Objects.requireNonNull(outcome);
        java.util.Objects.requireNonNull(before);
        java.util.Objects.requireNonNull(after);
    }
    /**
     * @return true only when this operation committed a proc */
    public boolean procced() { return outcome == Outcome.PROCCED; }
}
