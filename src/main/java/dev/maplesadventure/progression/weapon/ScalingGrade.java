package dev.maplesadventure.progression.weapon;

/** Presentation only. Combat must never convert a grade back into a coefficient. */
public enum ScalingGrade {
    NONE, E, D, C, B, A, S;
    public static ScalingGrade of(double coefficient) {
        if (!Double.isFinite(coefficient) || coefficient < 0 || coefficient > 1.5)
            throw new IllegalArgumentException("Invalid scaling coefficient");
        return coefficient < .10 ? NONE : coefficient < .25 ? E : coefficient < .40 ? D
                : coefficient < .55 ? C : coefficient < .70 ? B : coefficient < .85 ? A : S;
    }
    public String display() { return this == NONE ? "—" : name(); }
}
