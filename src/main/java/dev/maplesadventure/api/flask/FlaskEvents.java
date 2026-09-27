package dev.maplesadventure.api.flask;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/** Logical-server events. Effect cancellation skips both charge and restoration, then cancels the action. */
public final class FlaskEvents {
    public abstract static class Use extends Event {
        private final ServerPlayer player; private final FlaskKind kind;
        protected Use(ServerPlayer player, FlaskKind kind) { this.player = player; this.kind = kind; }
        public ServerPlayer player() { return player; }
        public FlaskKind kind() { return kind; }
    }
    public static final class Start extends Use implements ICancellableEvent {
        public Start(ServerPlayer p, FlaskKind k) { super(p, k); }
    }
    /** Commits the total restoration, delivered over the following six ticks, not an instantaneous heal. */
    public static final class Effect extends Use implements ICancellableEvent {
        private double amount;
        public Effect(ServerPlayer p, FlaskKind k, double amount) { super(p, k); setAmount(amount); }
        public double amount() { return amount; }
        public void setAmount(double amount) {
            if (!Double.isFinite(amount) || amount < 0 || amount > 1_000_000) throw new IllegalArgumentException("Invalid flask restoration");
            this.amount = amount;
        }
    }
    public static final class Finish extends Use { public Finish(ServerPlayer p, FlaskKind k) { super(p, k); } }
    public static final class Cancel extends Use {
        private final FlaskCancelReason reason; private final boolean effectApplied;
        public Cancel(ServerPlayer p, FlaskKind k, FlaskCancelReason reason, boolean effectApplied) {
            super(p, k); this.reason = reason; this.effectApplied = effectApplied;
        }
        public FlaskCancelReason reason() { return reason; }
        public boolean effectApplied() { return effectApplied; }
    }
    public static final class Refill extends Event {
        private final ServerPlayer player; private final FlaskRechargeReason reason;
        public Refill(ServerPlayer p, FlaskRechargeReason reason) { player = p; this.reason = reason; }
        public ServerPlayer player() { return player; }
        public FlaskRechargeReason reason() { return reason; }
    }
    public static final class Upgrade extends Event {
        private final ServerPlayer player; private final FlaskSnapshot before, after;
        public Upgrade(ServerPlayer p, FlaskSnapshot before, FlaskSnapshot after) { player = p; this.before = before; this.after = after; }
        public ServerPlayer player() { return player; }
        public FlaskSnapshot before() { return before; }
        public FlaskSnapshot after() { return after; }
    }
    private FlaskEvents() {}
}
