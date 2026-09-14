package dev.maplesadventure.progression;

import java.util.Locale;

public enum BatchUpgradeStatus {
    SUCCESS, INSUFFICIENT_EXPERIENCE, AT_CAP, INVALID_SESSION, INVALID_CONTEXT, STALE_STATE,
    TRANSACTION_FAILED, INVALID_DELTA;
    public String translationKey() { return "screen.maplesadventure.level_up.result." + name().toLowerCase(Locale.ROOT); }
}

