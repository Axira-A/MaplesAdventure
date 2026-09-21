package dev.maplesadventure.api.event;

import dev.maplesadventure.api.status.*;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.Event;

/** Read-only server notification for each affected ailment actually cleared, never for a no-op. */
public final class StatusClearEvent extends Event {
    /** Stable clear semantics; correction preservation is described by the calling API. */
    public enum Reason {
        /** Explicit administrator reset. */ ADMIN,
        /** Real or phantom death. */ DEATH,
        /** Duration/empty runtime expiry. */ EXPIRE,
        /** Fire ends active frost. */ FIRE_RESET,
        /** Cure through an authorized source. */ CURE,
        /** Foreign-session return. */ SESSION_RETURN,
        /** Pending buildup only. */ BUILDUP_ONLY
    }
    private final LivingEntity target;
    private final StatusView before, after;
    private final Reason reason;
    /** Publisher constructor; posting this event does not mutate state.
     *
     * @param target entity
     * @param before previous snapshot
     * @param after committed snapshot
     * @param reason reason */
    public StatusClearEvent(LivingEntity target, StatusView before, StatusView after, Reason reason) {
        this.target=target; this.before=before; this.after=after; this.reason=reason;
    }
    /**
     * @return observe-only target */ public LivingEntity target() { return target; }
    /**
     * @return ailment */ public MaplesStatusType type() { return before.type(); }
    /**
     * @return previous snapshot */ public StatusView before() { return before; }
    /**
     * @return committed snapshot */ public StatusView after() { return after; }
    /**
     * @return cause */ public Reason reason() { return reason; }
}
