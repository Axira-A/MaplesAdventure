package dev.maplesadventure.progression;

public record UpgradeResult(Status status, Attribute attribute, long cost, int experienceBefore,
                            int experienceAfter, PlayerAttributeState state) {
    public boolean success() { return status == Status.SUCCESS; }

    public enum Status {
        SUCCESS,
        INVALID_CONTEXT,
        AT_CAP,
        INSUFFICIENT_EXPERIENCE,
        COST_OUT_OF_RANGE,
        TRANSACTION_FAILED
    }
}
