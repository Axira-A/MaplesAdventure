package dev.maplesadventure.interaction;

public enum InteractionPriority {
    LOW(-0.15D),
    NORMAL(0.0D),
    HIGH(0.15D);

    private final double scoreBonus;

    InteractionPriority(double scoreBonus) {
        this.scoreBonus = scoreBonus;
    }

    public double scoreBonus() {
        return scoreBonus;
    }
}
