package dev.maplesadventure.authoring.logic;
/** One server-wide tick budget, also shared by events emitted recursively by addons. */
public final class LogicExecutionBudget {
    private long tick=Long.MIN_VALUE;private int remaining;
    public void begin(long current){if(tick!=current){tick=current;remaining=LogicLimits.OPERATIONS_PER_TICK;}}
    public void use(){if(remaining<=0)throw new IllegalStateException("Logic tick budget exhausted");remaining--;}
    public int remaining(){return remaining;}
}
