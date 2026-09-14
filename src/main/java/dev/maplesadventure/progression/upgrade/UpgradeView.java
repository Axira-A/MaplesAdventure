package dev.maplesadventure.progression.upgrade;

import dev.maplesadventure.progression.AttributeSnapshot;

/** Local-player-only, server-authored editing baseline. */
public record UpgradeView(UpgradeAccessContext access, long revision, AttributeSnapshot attributes,
                          int experience, int hardCap, double costMultiplier) {
    public java.util.UUID nonce() { return access.nonce(); }
}
