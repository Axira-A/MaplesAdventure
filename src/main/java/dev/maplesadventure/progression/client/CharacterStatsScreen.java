package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.stats.*;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Reusable, read-only detailed character-stat surface; it is not tied to Bonfires or upgrading. */
public final class CharacterStatsScreen extends Screen {
    private static final int PANEL = 0xF0131210;
    private static final int INNER = 0xE8171613;
    private static final int GOLD = 0xFF9C7137;
    private static final int GOLD_BRIGHT = 0xFFE4B462;
    private static final int TEXT = 0xFFE0D9CA;
    private static final int MUTED = 0xFF9B9388;

    private final Screen parent;
    private final CharacterStatsSnapshot current;
    private final CharacterStatsSnapshot preview;
    private CharacterStatsLayout layout;
    private StatsList list;
    private SoulsButton close;
    private CharacterStat hovered;
    private dev.maplesadventure.progression.spell.SpellSchoolStat hoveredSchool;

    public CharacterStatsScreen(Screen parent, CharacterStatsSnapshot current,
                                CharacterStatsSnapshot preview) {
        super(Component.translatable("screen.maplesadventure.character_stats.title"));
        this.parent = parent;
        this.current = current;
        this.preview = preview;
    }

    @Override protected void init() {
        layout = CharacterStatsLayout.calculate(width, height);
        var listBox = layout.list();
        list = addRenderableWidget(new StatsList(minecraft, listBox.width(), listBox.height(), listBox.y(), 18));
        list.setX(listBox.x());
        list.populate();
        int buttonHeight = layout.footer().height() < 28 ? 18 : 22;
        close = addRenderableWidget(new SoulsButton(width / 2 - 65,
                layout.footer().y() + (layout.footer().height() - buttonHeight) / 2,
                130, buttonHeight, Component.translatable("gui.back"), SoulsButton.Style.SECONDARY, this::onClose));
        setInitialFocus(list);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, width, height, 0x69000000);
        var panel = layout.panel();
        graphics.fillGradient(panel.x(), panel.y(), panel.right(), panel.bottom(), PANEL, 0xF00F0E0D);
        graphics.renderOutline(panel.x(), panel.y(), panel.width(), panel.height(), GOLD);
        graphics.renderOutline(panel.x() + 4, panel.y() + 4, panel.width() - 8, panel.height() - 8, 0x806A502B);
        graphics.drawCenteredString(font, title, width / 2, layout.titleY(), 0xFFF0DDAD);
        Component levels = current.level() == preview.level()
                ? Component.translatable("screen.maplesadventure.character_stats.level", current.level())
                : Component.translatable("screen.maplesadventure.character_stats.level_preview",
                        current.level(), preview.level());
        graphics.drawString(font, levels, panel.x() + 12, panel.y() + 25, MUTED, false);
        Component basis = preview.equipLoad().ratio().available()
                ? Component.translatable("screen.maplesadventure.encumbrance.status",
                Component.translatable(preview.equipLoad().tier().translationKey()))
                : Component.translatable("screen.maplesadventure.character_stats.basis");
        graphics.drawString(font, basis, panel.right() - 12 - font.width(basis), panel.y() + 25,
                preview.equipLoad().ratio().available() ? GOLD_BRIGHT : MUTED, false);
        hovered = null;
        hoveredSchool = null;
        for (Renderable renderable : renderables) renderable.render(graphics, mouseX, mouseY, partialTick);
        if (hovered != null) renderTooltip(graphics, hovered, mouseX, mouseY);
        if (hoveredSchool != null) {
            var school = hoveredSchool;
            List<Component> lines = new ArrayList<>();
            lines.add(school.displayName());
            lines.add(Component.translatable(school.implementationState().translationKey()));
            lines.add(Component.translatable("screen.maplesadventure.spell.progression", schoolPercent(school.progressionBonus())));
            lines.add(Component.translatable("screen.maplesadventure.spell.weights",
                    schoolPercent(school.profile().intelligenceWeight()), schoolPercent(school.profile().faithWeight()),
                    schoolPercent(school.profile().arcaneWeight())));
            if (school.context().available()) {
                lines.add(Component.translatable("screen.maplesadventure.spell.school_power", String.format(java.util.Locale.ROOT, "%.3f", school.runtimeSchoolPower())));
                lines.add(Component.translatable("screen.maplesadventure.spell.effective_power", String.format(java.util.Locale.ROOT, "%.3f", school.runtimeSpellPower())));
            }
            List<FormattedCharSequence> wrapped = new ArrayList<>();
            lines.forEach(line -> wrapped.addAll(font.split(line, 230)));
            graphics.renderTooltip(font, wrapped, mouseX, mouseY);
        }
    }

    private void renderTooltip(GuiGraphics graphics, CharacterStat stat, int mouseX, int mouseY) {
        CharacterStatValue value = preview.value(stat);
        ArrayList<Component> lines = new ArrayList<>();
        lines.add(Component.translatable(stat.translationKey()));
        lines.add(Component.translatable(value.implementation().translationKey()));
        if(stat.section()==CharacterStatSection.RESISTANCE) lines.add(Component.translatable(stat.translationKey()+".description"));
        if (stat.section()==CharacterStatSection.DEFENSE || stat.section()==CharacterStatSection.ELEMENTAL)
            lines.add(Component.translatable("screen.maplesadventure.character_stats.build_defense_layer"));
        if (value.available()) {
            StatBreakdown b = value.breakdown();
            lines.add(Component.translatable("screen.maplesadventure.character_stats.breakdown.base",
                    CharacterStatFormatting.value(stat, CharacterStatValue.previewOnly(b.base()))));
            lines.add(Component.translatable("screen.maplesadventure.character_stats.breakdown.attribute",
                    signed(b.attribute())));
            lines.add(Component.translatable("screen.maplesadventure.character_stats.breakdown.equipment",
                    signed(b.equipment())));
            lines.add(Component.translatable("screen.maplesadventure.character_stats.breakdown.effect",
                    signed(b.effect())));
        }
        if (stat == CharacterStat.EQUIP_LOAD_RATIO && preview.equipLoad().ratio().available()) {
            var load = preview.equipLoad();
            var profile = load.profile();
            lines.add(Component.translatable(load.tier().translationKey()));
            lines.add(Component.translatable("screen.maplesadventure.encumbrance.effect.movement",
                    percentSigned(profile.movementMultiplier() - 1.0D)));
            lines.add(Component.translatable("screen.maplesadventure.encumbrance.effect.regen",
                    percentSigned(profile.staminaRegenMultiplier() - 1.0D)));
            lines.add(Component.translatable("screen.maplesadventure.encumbrance.effect.cost",
                    percentSigned(profile.staminaCostMultiplier() - 1.0D)));
            lines.add(Component.translatable("screen.maplesadventure.encumbrance.effect.dodge",
                    Component.translatable(profile.dodgeDescriptionKey())));
            if (!profile.canSprint()) lines.add(Component.translatable("screen.maplesadventure.encumbrance.no_sprint"));
        }
        List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (Component line : lines) wrapped.addAll(font.split(line, 230));
        graphics.renderTooltip(font, wrapped, mouseX, mouseY);
    }

    private static String signed(double value) {
        return String.format(java.util.Locale.ROOT, "%+.1f", value);
    }
    private static String percentSigned(double value) {
        return String.format(java.util.Locale.ROOT, "%+.0f%%", value * 100.0D);
    }

    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }

    private final class StatsList extends ObjectSelectionList<Row> {
        StatsList(Minecraft minecraft, int width, int height, int top, int itemHeight) {
            super(minecraft, width, height, top, itemHeight);
            centerListVertically = false;
        }
        void populate() {
            clearEntries();
            for (var weapon : preview.weapons()) {
                addEntry(new SectionRow(Component.translatable("screen.maplesadventure.weapon.combat")));
                for (var line : dev.maplesadventure.progression.weapon.client.WeaponRequirementText.lines(weapon))
                    for(var wrapped:font.split(line,getRowWidth()-20)) addEntry(new WeaponRow(line,wrapped));
            }
            for (CharacterStatSection section : CharacterStatSection.values()) {
                addEntry(new SectionRow(section));
                for (CharacterStat stat : current.section(section)) addEntry(new StatRow(stat));
            }
            if (!preview.spellSchools().schools().isEmpty()) {
                addEntry(new SectionRow(Component.translatable("screen.maplesadventure.spell.schools")));
                for (var school : preview.spellSchools().schools().values()) addEntry(new SchoolRow(school));
            }
            setScrollAmount(0);
        }
        @Override public int getRowWidth() { return Math.max(80, getWidth() - 18); }
        @Override protected void renderListBackground(GuiGraphics graphics) {
            graphics.fill(getX(), getY(), getRight(), getBottom(), INNER);
            graphics.renderOutline(getX(), getY(), getWidth(), getHeight(), 0x805B472D);
        }
    }

    private abstract static class Row extends ObjectSelectionList.Entry<Row> {}
    private final class WeaponRow extends Row {
        private final Component label;
        private final FormattedCharSequence visible;
        WeaponRow(Component label,FormattedCharSequence visible) { this.label=label; this.visible=visible; }
        @Override public Component getNarration() { return label; }
        @Override public void render(GuiGraphics g,int index,int top,int left,int width,int height,int mouseX,int mouseY,boolean hovered,float partialTick) {
            g.drawString(font,visible,left+10,top+5,TEXT,false);
        }
    }

    private final class SectionRow extends Row {
        private final Component label;
        private SectionRow(CharacterStatSection section) { this(Component.translatable(section.translationKey())); }
        private SectionRow(Component label) { this.label = label; }
        @Override public Component getNarration() { return label; }
        @Override public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                     int mouseX, int mouseY, boolean hovered, float partialTick) {
            graphics.fill(left + 2, top + height - 2, left + width - 2, top + height - 1, 0x806A502B);
            graphics.drawString(font, label, left + 7, top + 5,
                    GOLD_BRIGHT, false);
        }
    }

    static String schoolPercent(double value) { return String.format(java.util.Locale.ROOT, "%+.1f%%", 100 * value); }
    private final class SchoolRow extends Row {
        private final dev.maplesadventure.progression.spell.SpellSchoolStat after;
        private SchoolRow(dev.maplesadventure.progression.spell.SpellSchoolStat after) { this.after = after; }
        private String comparison() {
            var before = current.spellSchools().schools().getOrDefault(after.schoolId(), after);
            return Math.abs(before.progressionBonus() - after.progressionBonus()) < 1e-8 ? schoolPercent(after.progressionBonus())
                    : schoolPercent(before.progressionBonus()) + " → " + schoolPercent(after.progressionBonus());
        }
        @Override public Component getNarration() { return after.displayName().copy().append(" " + comparison()); }
        @Override public void render(GuiGraphics g, int index, int top, int left, int width, int height,
                int mouseX, int mouseY, boolean hover, float partialTick) {
            if (hover || isFocused()) { hoveredSchool = after; g.fill(left + 2, top, left + width - 2, top + height, 0x4D5B3E1B); }
            String value = comparison();
            int valueWidth = font.width(value);
            g.drawString(font, font.substrByWidth(after.displayName(), Math.max(20, width - valueWidth - 30)).getString(), left + 10, top + 5, TEXT, false);
            g.drawString(font, value, left + width - 10 - valueWidth, top + 5, GOLD_BRIGHT, false);
        }
    }

    private final class StatRow extends Row {
        private final CharacterStat stat;
        private StatRow(CharacterStat stat) { this.stat = stat; }
        @Override public Component getNarration() {
            return Component.translatable("screen.maplesadventure.character_stats.narration",
                    Component.translatable(stat.translationKey()),
                    CharacterStatFormatting.comparison(stat, current.value(stat), preview.value(stat)));
        }
        @Override public void render(GuiGraphics graphics, int index, int top, int left, int width, int height,
                                     int mouseX, int mouseY, boolean hovered, float partialTick) {
            if (hovered) {
                graphics.fill(left + 2, top, left + width - 2, top + height, 0x4D5B3E1B);
                CharacterStatsScreen.this.hovered = stat;
            }
            CharacterStatValue after = preview.value(stat);
            graphics.drawString(font, Component.translatable(stat.translationKey()), left + 10, top + 5,
                    after.available() ? TEXT : MUTED, false);
            Component value = CharacterStatFormatting.comparison(stat, current.value(stat), after);
            int right = left + width - 10;
            graphics.drawString(font, value, right - font.width(value), top + 5,
                    after.implementation() == StatImplementationState.UNAVAILABLE ? MUTED
                            : after.differsFrom(current.value(stat)) ? GOLD_BRIGHT : TEXT, false);
            if (after.implementation() == StatImplementationState.PREVIEW_ONLY) {
                Component marker = Component.translatable("screen.maplesadventure.character_stats.preview_marker");
                graphics.drawString(font, marker, right - font.width(value) - font.width(marker) - 8,
                        top + 5, 0xFFB38955, false);
            }
        }
    }
}
