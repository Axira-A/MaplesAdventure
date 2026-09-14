package dev.maplesadventure.progression.client;

/** Pure responsive geometry for the reusable scrolling character-stat screen. */
public record CharacterStatsLayout(LevelUpLayout.Box panel, LevelUpLayout.Box list,
                                   LevelUpLayout.Box footer, int titleY) {
    public static CharacterStatsLayout calculate(int screenWidth, int screenHeight) {
        int panelWidth = Math.min(720, Math.max(280, screenWidth - 24));
        int panelHeight = Math.min(500, Math.max(160, screenHeight - 16));
        panelWidth = Math.min(panelWidth, Math.max(1, screenWidth - 8));
        panelHeight = Math.min(panelHeight, Math.max(1, screenHeight - 8));
        int left = (screenWidth - panelWidth) / 2, top = (screenHeight - panelHeight) / 2;
        var panel = new LevelUpLayout.Box(left, top, panelWidth, panelHeight);
        int footerHeight = panelHeight < 240 ? 24 : 30;
        var footer = new LevelUpLayout.Box(left + 10, panel.bottom() - footerHeight - 6,
                panelWidth - 20, footerHeight);
        var list = new LevelUpLayout.Box(left + 10, top + 38, panelWidth - 20,
                Math.max(54, footer.y() - top - 44));
        return new CharacterStatsLayout(panel, list, footer, top + 9);
    }
}
