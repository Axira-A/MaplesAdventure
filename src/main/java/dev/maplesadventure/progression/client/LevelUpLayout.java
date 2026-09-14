package dev.maplesadventure.progression.client;

/** Deterministic responsive geometry, split from rendering so every target scale can be tested. */
public record LevelUpLayout(
        Box panel, Box summary, Box attributes, Box derived, Box description, Box footer,
        int titleY, int rowHeight, boolean compact, boolean dense, boolean wide
) {
    public record Box(int x, int y, int width, int height) {
        public int right() { return x + width; }
        public int bottom() { return y + height; }
        public boolean contains(double px, double py) {
            return px >= x && px < right() && py >= y && py < bottom();
        }
    }

    public static LevelUpLayout calculate(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(920, Math.max(296, screenWidth - 24));
        int panelHeight = Math.min(500, Math.max(168, screenHeight - 16));
        int left = (screenWidth - panelWidth) / 2;
        int top = (screenHeight - panelHeight) / 2;
        // 427x240 is the common 854x480 / GUI-scale-2 case.  It is tight, but it
        // still has enough room for the derived-stat column; reserve the true
        // single-column fallback for substantially smaller logical canvases.
        boolean compact = panelHeight < 190 || panelWidth < 350;
        boolean dense = panelHeight < 300 || panelWidth < 540;
        boolean wide = panelWidth >= 540 && panelHeight >= 300;

        int titleHeight = dense ? 20 : 30;
        int summaryHeight = dense ? 30 : 44;
        Box summary = new Box(left + 12, top + titleHeight, panelWidth - 24, summaryHeight);
        int footerHeight = dense ? 26 : 36;
        Box footer = new Box(left + 12, top + panelHeight - footerHeight - 8,
                panelWidth - 24, footerHeight);
        int bodyTop = summary.bottom() + (compact ? 4 : dense ? 6 : 10);
        int bodyHeight = Math.max(80, footer.y() - bodyTop - (compact ? 3 : dense ? 6 : 8));
        int gap = compact ? 0 : dense ? 6 : 12;
        int rightWidth = compact ? 0 : (wide ? Math.max(190, (panelWidth - 36) * 34 / 100)
                : Math.max(134, (panelWidth - 36) * 35 / 100));
        int attributesWidth = panelWidth - 24 - (rightWidth == 0 ? 0 : rightWidth + gap);
        Box attributes = new Box(left + 12, bodyTop, attributesWidth, bodyHeight);
        // Even the 320x180 fallback retains a small, clickable detailed-stats
        // affordance in the title band. The table itself stays single-column.
        Box derived = rightWidth == 0
                ? new Box(left + panelWidth - 112, top + 3, 100, 14)
                : new Box(attributes.right() + gap, bodyTop, rightWidth,
                dense ? bodyHeight : Math.max(90, bodyHeight * 66 / 100));
        Box description = rightWidth == 0 ? new Box(0, 0, 0, 0)
                : new Box(derived.x(), derived.bottom() + 8, rightWidth,
                Math.max(0, bodyTop + bodyHeight - derived.bottom() - 8));
        int header = compact ? 0 : dense ? 14 : 18;
        int rowHeight = Math.max(10, (bodyHeight - header) / 8);
        return new LevelUpLayout(new Box(left, top, panelWidth, panelHeight), summary, attributes,
                derived, description, footer, top + (dense ? 6 : 9), rowHeight, compact, dense, wide);
    }
}
