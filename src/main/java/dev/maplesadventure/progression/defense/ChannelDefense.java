package dev.maplesadventure.progression.defense;

/** Maples channel defense, independent of Vanilla armor and absorption hearts. */
public record ChannelDefense(double defense, double absorption) {
    public static final ChannelDefense NONE = new ChannelDefense(0, 0);
    public ChannelDefense {
        if (!Double.isFinite(defense) || defense < 0 || defense > 10000
                || !Double.isFinite(absorption) || absorption < -.50 || absorption > .80)
            throw new IllegalArgumentException("Defense must be 0..10000 and absorption -0.50..0.80, finite");
    }
}
