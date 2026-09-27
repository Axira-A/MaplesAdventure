package dev.maplesadventure.flask;

/** Pure one-shot clock, separately testable without a Minecraft world. */
public final class FlaskUseTimeline {
    private int elapsed;
    private boolean claimed;
    public void tick() { elapsed++; }
    public boolean claimEffect() {
        if (claimed || elapsed < FlaskRules.EFFECT_TICK) return false;
        claimed = true; return true;
    }
    public boolean effectClaimed() { return claimed; }
    public boolean finished() { return elapsed >= FlaskRules.USE_TICKS; }
}
