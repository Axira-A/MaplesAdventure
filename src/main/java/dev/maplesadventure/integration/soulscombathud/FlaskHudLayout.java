package dev.maplesadventure.integration.soulscombathud;

/** HUD-local geometry. No game state, third-party types, or screen-coordinate assumptions. */
public final class FlaskHudLayout {
    public static final int GAP = 3;
    public record Rect(float x, float y, float width, float height) {
        public float right() { return x + width; }
        public float bottom() { return y + height; }
        public boolean intersects(Rect other, float gap) {
            return x < other.right() + gap && right() + gap > other.x
                    && y < other.bottom() + gap && bottom() + gap > other.y;
        }
    }
    public static Rect counter(int centerX, int centerY, int textWidth, int textHeight) {
        return new Rect(centerX - 12 - GAP - textWidth, centerY + 16 - 1 - textHeight, textWidth, textHeight);
    }
    public static boolean fits(Rect rect, float scale, float offsetX, float offsetY, int width, int height) {
        return scale > 0 && rect.x * scale + offsetX >= 2 && rect.y * scale + offsetY >= 2
                && rect.right() * scale + offsetX <= width - 2 && rect.bottom() * scale + offsetY <= height - 2;
    }
    private FlaskHudLayout() {}
}
