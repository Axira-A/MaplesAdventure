package dev.maplesadventure.api.status;

import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import dev.maplesadventure.integration.api.StatusApiBridge;

/** Server-authoritative facade over the existing status service; does not own gameplay state. */
public final class MaplesStatusApi {
    private MaplesStatusApi() {}
    /**
     * Adds buildup after source, Phase, immunity and active-state checks.
     *
     * @param target living server target
     *
     * @param type canonical ailment; null yields UNSUPPORTED_STATUS
     *
     * @param amount finite positive amount at most 100000
     *
     * @param source actual source, never a fabricated environmental substitute
     *
     * @return detailed immutable outcome
     */
    public static StatusApplyResult apply(LivingEntity target, MaplesStatusType type, double amount, StatusSource source) {
        return StatusApiBridge.apply(target, type, amount, source, false);
    }
    /**
     * Requests one proc through the same buildup checks, not unconditional damage.
     *
     * @param target living server target
     * @param type ailment
     * @param source actual source
     *
     * @return detailed result; immune/active/Phase-denied requests do not proc
     */
    public static StatusApplyResult requestProc(LivingEntity target, MaplesStatusType type, StatusSource source) {
        return StatusApiBridge.apply(target, type, 0, source, true);
    }
    /**
     * Queries without creating an Attachment.
     *
     * @param target server target
     * @param type ailment
     *
     * @return empty on null, client, wrong thread, removed/dead target or unsupported type
     */
    public static Optional<StatusView> query(LivingEntity target, MaplesStatusType type) {
        return StatusApiBridge.query(target, type);
    }
    /**
     * Cures one ailment and its control lock, preserving repeated-proc correction history.
     *
     * @param target server target
     * @param type ailment
     * @return immutable outcome
     */
    public static StatusApplyResult clear(LivingEntity target, MaplesStatusType type) {
        return StatusApiBridge.clear(target, type, false);
    }
    /**
     * Removes only pending buildup, not timed effects/control/correction history.
     *
     * @param target server target
     * @param type ailment
     * @return immutable outcome
     */
    public static StatusApplyResult clearBuildup(LivingEntity target, MaplesStatusType type) {
        return StatusApiBridge.clear(target, type, true);
    }
}
