package dev.maplesadventure.client.render;

public record PhantomTint(int red, int green, int blue, int alpha, boolean fullBright) {
    public static final PhantomTint RESIDUAL_ECHO = new PhantomTint(220, 230, 245, 255, true);
    public static final PhantomTint GOLD_COOPERATOR = new PhantomTint(255, 205, 72, 220, true);
    public static final PhantomTint RED_INVADER = new PhantomTint(210, 38, 42, 220, true);
    public static final int FULL_BRIGHT = 0x00F000F0;
}
