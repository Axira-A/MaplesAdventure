package dev.maplesadventure.api.event;

import java.util.Optional;
import java.util.UUID;
import dev.maplesadventure.api.status.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/**
 * Read-only logical-server notification on NeoForge.EVENT_BUS after accepted accumulation.
 * Fired once, including hits that proc. A proc's effects run before notifications. Reentrant API
 * mutations are rejected; do not call hurt or directly change entities from this notification.
 */
public final class StatusBuildupEvent extends Event {
    private final LivingEntity target;
    private final StatusView before, after;
    private final Optional<UUID> source;
    private final double amount;
    /** Internal publisher constructor; integrations listen, never post synthetic gameplay events.
     *
     * @param target affected entity
     * @param before previous snapshot
     * @param after committed snapshot
     *
     * @param source responsible UUID, empty for environment
     * @param amount accepted buildup */
    public StatusBuildupEvent(LivingEntity target, StatusView before, StatusView after, Optional<UUID> source, double amount) {
        this.target=target; this.before=before; this.after=after; this.source=source; this.amount=amount;
    }
    /**
     * @return affected entity; observe only */ public LivingEntity target() { return target; }
    /**
     * @return canonical type */ public MaplesStatusType type() { return before.type(); }
    /**
     * @return previous detached state */ public StatusView before() { return before; }
    /**
     * @return committed detached state */ public StatusView after() { return after; }
    /**
     * @return responsible source UUID */ public Optional<UUID> source() { return source; }
    /**
     * @return accepted accumulation before overflow discard */ public double amount() { return amount; }
}
