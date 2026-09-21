package dev.maplesadventure.api.event;

import java.util.Optional;
import java.util.UUID;
import dev.maplesadventure.api.status.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * Read-only notification after ONE committed proc and its ordinary effects/death handling.
 * Not cancellable. The target may have died or returned as a phantom. Do not replay proc damage.
 * API mutations inside any status notification are rejected to prevent recursive procs.
 */
public final class StatusProcEvent extends Event {
    private final LivingEntity target;
    private final StatusView before, after;
    private final Optional<UUID> source;
    /** Publisher constructor; posting this event does not invoke gameplay.
     *
     * @param target affected entity
     * @param before pre-proc state
     * @param after committed state
     *
     * @param source responsible UUID or empty */
    public StatusProcEvent(LivingEntity target, StatusView before, StatusView after, Optional<UUID> source) {
        this.target=target; this.before=before; this.after=after; this.source=source;
    }
    /**
     * @return observe-only target */ public LivingEntity target() { return target; }
    /**
     * @return canonical proc type */ public MaplesStatusType type() { return before.type(); }
    /**
     * @return previous snapshot */ public StatusView before() { return before; }
    /**
     * @return committed snapshot */ public StatusView after() { return after; }
    /**
     * @return responsible UUID */ public Optional<UUID> source() { return source; }
}
