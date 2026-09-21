package dev.maplesadventure.api.status;

import java.util.Optional;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable point-in-time server view. Buildup is the last authoritative runtime value, not a
 * client animation. Threshold includes correction; correctionOffset is the configured current
 * stage offset (the final threshold may be capped). Control is independent of an active DOT.
 *
     * @param type canonical ailment
 *
     * @param buildup current accumulated amount
 *
     * @param threshold actual corrected trigger threshold
 *
     * @param immune effective immunity, including target eligibility
 *
     * @param active whether the timed effect is active
 *
     * @param correctionProfile selected correction ID
 *
     * @param correctionOffset current correction offset
 *
     * @param procCount correction stage count, saturated by the runtime
 *
     * @param procDamageMultiplier target's proc multiplier
 *
     * @param controlType Sleep/Madness lock, empty when unlocked
 *
     * @param controlTicks remaining control ticks
 */
public record StatusView(MaplesStatusType type, double buildup, double threshold, boolean immune,
        boolean active, ResourceLocation correctionProfile, double correctionOffset, int procCount,
        double procDamageMultiplier, Optional<MaplesStatusType> controlType, long controlTicks) {
    /** Creates a detached view; no Attachment or mutable collection is retained. */
    public StatusView {
        java.util.Objects.requireNonNull(type);
        java.util.Objects.requireNonNull(correctionProfile);
        java.util.Objects.requireNonNull(controlType);
    }
}
