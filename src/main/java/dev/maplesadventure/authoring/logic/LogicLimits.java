package dev.maplesadventure.authoring.logic;
/** Persistence, wire and execution share these bounds. */
public final class LogicLimits {
    public static final int BINDINGS=8, ACTIONS=16, DEPTH=8, NODES=64, FIELDS=32, TYPES=256;
    public static final int OPERATIONS_PER_TICK=4096, BINDINGS_PER_EVENT=8, FLAGS=4096;
    private LogicLimits(){}
}
