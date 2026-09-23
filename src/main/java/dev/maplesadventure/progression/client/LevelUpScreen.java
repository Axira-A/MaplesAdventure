package dev.maplesadventure.progression.client;

import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.stats.*;
import dev.maplesadventure.progression.network.UpgradePayloads;
import dev.maplesadventure.progression.upgrade.UpgradeView;
import java.util.EnumMap;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/** Access-neutral Souls-style level-up editor. It knows no Bonfires or future NPC classes. */
public final class LevelUpScreen extends Screen {
    private static final int PANEL = 0xF0131210;
    private static final int PANEL_INNER = 0xE81A1916;
    private static final int GOLD = 0xFF9C7137;
    private static final int GOLD_BRIGHT = 0xFFE4B462;
    private static final int TEXT = 0xFFE0D9CA;
    private static final int MUTED = 0xFF9B9388;

    private UpgradeView baseline;
    private final LevelUpDraft draft = new LevelUpDraft();
    private final EnumMap<Attribute, SoulsButton> plus = new EnumMap<>(Attribute.class);
    private final EnumMap<Attribute, SoulsButton> minus = new EnumMap<>(Attribute.class);
    private LevelUpPreviewCalculator.Preview preview;
    private CharacterStatsSnapshot baselineStats;
    private CharacterStatsSnapshot previewStats;
    private java.util.List<StatPreviewPriority.Comparison> compactStats = java.util.List.of();
    private LevelUpLayout layout;
    private SoulsButton confirm, cancel;
    private Attribute selected = Attribute.VIGOR;
    private boolean pending, invalid;
    private boolean navigatingToStats;
    private Component feedback = Component.empty();

    public LevelUpScreen(UpgradeView baseline) {
        super(Component.translatable("screen.maplesadventure.level_up"));
        this.baseline = baseline;
    }
    public UUID nonce() { return baseline.nonce(); }

    @Override protected void init() {
        layout = LevelUpLayout.calculate(width, height);
        plus.clear(); minus.clear();
        var table = layout.attributes();
        int header = attributeHeaderHeight();
        int buttonSize = Math.clamp(layout.rowHeight() - 2, 10, 18);
        int rowTop = table.y() + header;
        for (Attribute attribute : Attribute.values()) {
            int y = rowTop + attribute.ordinal() * layout.rowHeight()
                    + Math.max(0, (layout.rowHeight() - buttonSize) / 2);
            int plusX = table.right() - buttonSize - 7;
            int minusX = plusX - buttonSize - (layout.compact() ? 21 : 31);
            SoulsButton sub = addRenderableWidget(new SoulsButton(minusX, y, buttonSize, buttonSize,
                    Component.literal("−"), SoulsButton.Style.ADJUST, () -> adjust(attribute, -1)));
            SoulsButton add = addRenderableWidget(new SoulsButton(plusX, y, buttonSize, buttonSize,
                    Component.literal("+"), SoulsButton.Style.ADJUST, () -> adjust(attribute, 1)));
            sub.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.level_up.retract", attribute.displayName())));
            add.setTooltip(Tooltip.create(Component.translatable("screen.maplesadventure.level_up.add", attribute.displayName())));
            minus.put(attribute, sub); plus.put(attribute, add);
        }
        int footerY = layout.footer().y() + Math.max(1, (layout.footer().height() - (layout.dense() ? 18 : 24)) / 2);
        int gap = 10;
        int buttonWidth = Math.min(220, (layout.footer().width() - gap) / 2);
        int buttonHeight = layout.dense() ? 18 : 24;
        int start = layout.footer().x() + (layout.footer().width() - buttonWidth * 2 - gap) / 2;
        confirm = addRenderableWidget(new SoulsButton(start, footerY, buttonWidth, buttonHeight, text("confirm"),
                SoulsButton.Style.PRIMARY, this::submit));
        cancel = addRenderableWidget(new SoulsButton(start + buttonWidth + gap, footerY, buttonWidth, buttonHeight,
                text("cancel"), SoulsButton.Style.SECONDARY, this::onClose));
        recalculate();
        setInitialFocus(plus.get(selected));
    }

    private void adjust(Attribute attribute, int amount) {
        if (pending || invalid) return;
        selected = attribute;
        draft.adjust(attribute, amount, baseline.attributes().state(), baseline.hardCap());
        feedback = Component.empty();
        recalculate();
    }
    private void recalculate() {
        preview = LevelUpPreviewCalculator.calculate(baseline.attributes(), draft.deltas(), baseline.experience(),
                baseline.hardCap(), baseline.costMultiplier());
        var attributes = baseline.attributes();
        baselineStats = attributes.characterStats();
        previewStats = preview.characterStats();
        compactStats = StatPreviewPriority.select(baselineStats, previewStats, 7);
        for (Attribute attribute : Attribute.values()) {
            if (!plus.containsKey(attribute)) continue;
            plus.get(attribute).active = !pending && !invalid && preview.state().get(attribute) < baseline.hardCap();
            minus.get(attribute).active = !pending && !invalid && draft.get(attribute) > 0;
            if (preview.state().get(attribute) >= baseline.hardCap())
                plus.get(attribute).setTooltip(Tooltip.create(text("at_cap")));
        }
        if (confirm != null) {
            confirm.active = !pending && !invalid && preview.points() > 0 && preview.remainingXp() >= 0
                    && preview.totalCost() <= Integer.MAX_VALUE;
            confirm.setMessage(text(pending ? "pending" : "confirm"));
            confirm.setTooltip(Tooltip.create(invalid ? text("result.invalid_session")
                    : preview.points() == 0 ? text("no_points")
                    : preview.remainingXp() < 0 ? text("result.insufficient_experience") : text("confirm_hint")));
        }
        if (cancel != null) cancel.active = !pending;
    }
    private void submit() {
        if (confirm == null || !confirm.active) return;
        pending = true;
        recalculate();
        PacketDistributor.sendToServer(new UpgradePayloads.Submit(nonce(), baseline.revision(), draft.deltas()));
    }
    public void accept(UpgradeView view, BatchUpgradeStatus status) {
        if (view.revision() <= baseline.revision()) return;
        baseline = view;
        draft.clear(); pending = false;
        feedback = Component.translatable(status.translationKey());
        recalculate();
    }
    public void invalidate(BatchUpgradeStatus reason) {
        invalid = true; pending = false; draft.clear();
        feedback = Component.translatable(reason.translationKey());
        recalculate();
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && layout != null && layout.derived().contains(mouseX, mouseY)) {
            navigatingToStats = true;
            minecraft.setScreen(new CharacterStatsScreen(this, baselineStats, previewStats));
            return true;
        }
        selectRow(mouseX, mouseY);
        return super.mouseClicked(mouseX, mouseY, button);
    }
    private void selectRow(double mouseX, double mouseY) {
        if (layout == null || !layout.attributes().contains(mouseX, mouseY)) return;
        int top = layout.attributes().y() + attributeHeaderHeight();
        int row = (int) ((mouseY - top) / layout.rowHeight());
        if (row >= 0 && row < Attribute.values().length) selected = Attribute.values()[row];
    }
    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN) {
            int direction = keyCode == GLFW.GLFW_KEY_UP ? -1 : 1;
            selected = Attribute.values()[Math.floorMod(selected.ordinal() + direction, Attribute.values().length)];
            setFocused(plus.get(selected));
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_LEFT) { adjust(selected, -1); return true; }
        if (keyCode == GLFW.GLFW_KEY_RIGHT) { adjust(selected, 1); return true; }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (confirm.active) submit();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
    @Override public void tick() {
        if (minecraft.player == null || minecraft.level == null
                || !minecraft.level.dimension().location().equals(baseline.access().sourceDimension())) onClose();
    }
    @Override public void onClose() {
        if (pending && minecraft.player != null && minecraft.level != null
                && minecraft.level.dimension().location().equals(baseline.access().sourceDimension())) return;
        if (baseline.access().sourceKey().equals(dev.maplesadventure.bonfire.BonfireSessionService.UPGRADE_SOURCE)
                && dev.maplesadventure.client.bonfire.BonfireClient.resume()) return;
        super.onClose();
    }
    @Override public void removed() {
        if (navigatingToStats) {
            navigatingToStats = false;
            return;
        }
        draft.clear();
        UpgradeClient.close(nonce());
    }
    @Override public boolean isPauseScreen() { return false; }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(0, 0, width, height, 0x65000000);
        drawPanel(g, layout.panel(), PANEL, GOLD);
        var panel = layout.panel();
        g.renderOutline(panel.x() + 4, panel.y() + 4, panel.width() - 8, panel.height() - 8, 0x806A502B);
        drawTitle(g);
        drawSummary(g);
        drawAttributes(g, mouseX, mouseY);
        drawCompactStats(g, mouseX, mouseY);
        if (!layout.compact()) drawDescription(g);
        drawFeedback(g);
        for (Renderable renderable : renderables) renderable.render(g, mouseX, mouseY, partialTick);
        if (layout.compact() && layout.attributes().contains(mouseX, mouseY))
            g.renderTooltip(font, font.split(description(selected), Math.min(220, width - 20)), mouseX, mouseY);
    }

    private void drawTitle(GuiGraphics g) {
        var panel = layout.panel();
        g.pose().pushPose();
        float scale = layout.dense() ? 1.0F : 1.35F;
        g.pose().scale(scale, scale, 1F);
        g.drawCenteredString(font, title, (int) (width / (2F * scale)),
                (int) (layout.titleY() / scale), 0xFFF0DDAD);
        g.pose().popPose();
        int y = layout.summary().y() - 4;
        g.fill(panel.x() + panel.width() / 5, y, panel.x() + panel.width() * 2 / 5, y + 1, 0x807B582C);
        g.fill(panel.x() + panel.width() * 3 / 5, y, panel.x() + panel.width() * 4 / 5, y + 1, 0x807B582C);
        g.fill(width / 2 - 2, y - 2, width / 2 + 2, y + 3, GOLD);
    }
    private void drawSummary(GuiGraphics g) {
        var box = layout.summary();
        drawPanel(g, box, PANEL_INNER, 0xFF67502F);
        String[] keys = {"level", "held_experience", "required_experience", "remaining_experience"};
        String[] values = {baseline.attributes().level() + "  →  " + preview.level(),
                Integer.toString(baseline.experience()), Long.toString(preview.totalCost()),
                Long.toString(preview.remainingXp())};
        int cell = box.width() / 4;
        for (int index = 0; index < 4; index++) {
            int x = box.x() + index * cell;
            if (index > 0) g.fill(x, box.y() + 4, x + 1, box.bottom() - 4, 0x605D4C36);
            g.drawCenteredString(font, text(keys[index]), x + cell / 2, box.y() + 5, MUTED);
            int valueColor = index == 0 || index == 3 ? GOLD_BRIGHT : TEXT;
            g.drawCenteredString(font, values[index], x + cell / 2,
                    box.y() + (layout.dense() ? 17 : 24), valueColor);
        }
    }
    private void drawAttributes(GuiGraphics g, int mouseX, int mouseY) {
        var box = layout.attributes();
        drawPanel(g, box, PANEL_INNER, 0xFF5D4A2F);
        int header = attributeHeaderHeight();
        if (!layout.compact()) {
            g.drawString(font, text("attribute"), box.x() + 10, box.y() + 5, MUTED);
            g.drawCenteredString(font, text("current_value"), box.x() + box.width() * 45 / 100, box.y() + 5, MUTED);
            g.drawCenteredString(font, text("preview_value"), box.x() + box.width() * 62 / 100, box.y() + 5, MUTED);
            g.drawCenteredString(font, text("adjustment"), box.right() - 38, box.y() + 5, MUTED);
            g.fill(box.x() + 3, box.y() + 16, box.right() - 3, box.y() + 17, 0x80664B29);
        }
        int rowTop = box.y() + header;
        for (Attribute attribute : Attribute.values()) {
            int y = rowTop + attribute.ordinal() * layout.rowHeight();
            boolean hover = mouseX >= box.x() && mouseX < box.right() && mouseY >= y
                    && mouseY < y + layout.rowHeight();
            if (hover) selected = attribute;
            boolean chosen = selected == attribute;
            if (chosen) {
                g.fillGradient(box.x() + 2, y, box.right() - 2, y + layout.rowHeight(),
                        0x855B3E1B, 0x302A2118);
                g.fill(box.x() + 2, y, box.x() + 4, y + layout.rowHeight(), GOLD_BRIGHT);
            }
            if (attribute.ordinal() > 0) g.fill(box.x() + 5, y, box.right() - 5, y + 1, 0x383D362D);
            int textY = y + Math.max(1, (layout.rowHeight() - font.lineHeight) / 2);
            g.drawString(font, attribute.displayName(), box.x() + (layout.compact() ? 6 : 12), textY,
                    chosen ? 0xFFF1DEB7 : TEXT);
            int currentX = box.x() + box.width() * (layout.compact() ? 43 : 45) / 100;
            int previewX = box.x() + box.width() * (layout.compact() ? 59 : 62) / 100;
            g.drawCenteredString(font, Integer.toString(baseline.attributes().state().get(attribute)), currentX, textY, TEXT);
            g.drawCenteredString(font, "→", (currentX + previewX) / 2, textY, 0xFF8A7963);
            g.drawCenteredString(font, Integer.toString(preview.state().get(attribute)), previewX, textY,
                    draft.get(attribute) > 0 ? GOLD_BRIGHT : TEXT);
            int deltaX = (minus.get(attribute).getRight() + plus.get(attribute).getX()) / 2;
            g.drawCenteredString(font, Integer.toString(draft.get(attribute)), deltaX, textY,
                    draft.get(attribute) > 0 ? GOLD_BRIGHT : MUTED);
        }
    }
    private void drawCompactStats(GuiGraphics g, int mouseX, int mouseY) {
        var box = layout.derived();
        boolean hovered = box.contains(mouseX, mouseY);
        drawPanel(g, box, PANEL_INNER, hovered ? GOLD_BRIGHT : 0xFF685130);
        if (layout.compact()) {
            g.drawCenteredString(font, text("details_hint"), box.x() + box.width() / 2, box.y() + 3,
                    hovered ? GOLD_BRIGHT : TEXT);
            return;
        }
        g.drawCenteredString(font, text("derived_preview"), box.x() + box.width() / 2, box.y() + 7,
                hovered ? 0xFFF1D28F : GOLD_BRIGHT);
        g.fill(box.x() + 12, box.y() + 20, box.right() - 12, box.y() + 21, 0x806E512D);
        if (drawWeaponRequirementPreview(g, box)) return;
        var changedSchools = previewStats.spellSchools().changedFrom(baselineStats.spellSchools());
        if (!changedSchools.isEmpty()) {
            drawSchoolPreview(g, box, changedSchools);
            g.drawCenteredString(font, text("details_hint"), box.x() + box.width() / 2, box.bottom() - 11, MUTED);
            return;
        }
        if (baselineStats.equipLoad().ratio().available() && previewStats.equipLoad().ratio().available()) {
            drawEncumbrancePreview(g, box);
            g.drawCenteredString(font, text("details_hint"), box.x() + box.width() / 2,
                    box.bottom() - 11, hovered ? GOLD_BRIGHT : MUTED);
            return;
        }
        int maximumRows = Math.max(1, Math.min(compactStats.size(), (box.height() - 37) / 11));
        for (int i = 0; i < maximumRows; i++) {
            var row = compactStats.get(i);
            int y = box.y() + 25 + i * 11;
            CharacterStatValue after = row.preview();
            Component label = Component.translatable(row.stat().translationKey());
            if (after.implementation() == StatImplementationState.PREVIEW_ONLY)
                label = Component.translatable("screen.maplesadventure.character_stats.preview_label", label);
            g.drawString(font, label, box.x() + 7, y, row.changed() ? 0xFFE8D1A2 : MUTED, false);
            Component value = CharacterStatFormatting.comparison(row.stat(), row.current(), after);
            g.drawString(font, value, box.right() - 7 - font.width(value), y,
                    row.changed() ? GOLD_BRIGHT : TEXT, false);
        }
        g.drawCenteredString(font, text("details_hint"), box.x() + box.width() / 2,
                box.bottom() - 11, hovered ? GOLD_BRIGHT : MUTED);
    }

    private void drawEncumbrancePreview(GuiGraphics g, LevelUpLayout.Box box) {
        var before = baselineStats.equipLoad();
        var after = previewStats.equipLoad();
        String[] labels = {
                text("encumbrance.tier").getString(), text("encumbrance.movement").getString(),
                text("encumbrance.regen").getString(), text("encumbrance.cost").getString(),
                text("encumbrance.dodge").getString()
        };
        String[] oldValues = {
                Component.translatable(before.tier().translationKey()).getString(),
                percentage(before.profile().movementMultiplier()), percentage(before.profile().staminaRegenMultiplier()),
                percentage(before.profile().staminaCostMultiplier()),
                Component.translatable(before.profile().dodgeDescriptionKey()).getString()
        };
        String[] newValues = {
                Component.translatable(after.tier().translationKey()).getString(),
                percentage(after.profile().movementMultiplier()), percentage(after.profile().staminaRegenMultiplier()),
                percentage(after.profile().staminaCostMultiplier()),
                Component.translatable(after.profile().dodgeDescriptionKey()).getString()
        };
        int maxRows = Math.min(5, Math.max(1, (box.height() - 37) / 11));
        int extraLines = 0;
        for (int i = 0; i < maxRows; i++) {
            int y = box.y() + 23 + (i + extraLines) * 11;
            boolean changed = !oldValues[i].equals(newValues[i]);
            String value = changed ? oldValues[i] + " → " + newValues[i] : newValues[i];
            // Narrow windows get two lines per row. Never paint the comparison over its label.
            if (box.width() < 210 && (i == 0 || i == 4)) {
                if (y + 18 > box.bottom() - 13) break;
                g.drawString(font, labels[i], box.x() + 7, y, MUTED, false);
                drawFitted(g, value, box.x() + 7, y + 9, box.width() - 14, changed ? GOLD_BRIGHT : TEXT);
                extraLines++;
            } else {
                int valueWidth = Math.min(font.width(value), box.width() * 2 / 3);
                String label = font.plainSubstrByWidth(labels[i], box.width() - valueWidth - 21);
                g.drawString(font, label, box.x() + 7, y, changed ? 0xFFE8D1A2 : MUTED, false);
                drawFitted(g, value, box.right() - 7 - valueWidth, y, valueWidth, changed ? GOLD_BRIGHT : TEXT);
            }
        }
    }

    private boolean drawWeaponRequirementPreview(GuiGraphics g, LevelUpLayout.Box box) {
        for (var after : previewStats.weapons()) {
            if (after.held().handState() != dev.maplesadventure.progression.weapon.WeaponLoadoutSnapshot.HandState.WEAPON) continue;
            var before=baselineStats.weapons().stream().filter(v->v.offhand()==after.offhand() && v.held().item().equals(after.held().item())).findFirst().orElse(after);
            boolean requirementsChanged=!before.result().missingAttributes().equals(after.result().missingAttributes());
            boolean attackChanged=after.held().weapon() && Math.abs(before.attack().attackRating()-after.attack().attackRating())>1e-7;
            boolean statusChanged=!before.statuses().equals(after.statuses());
            if (!requirementsChanged && !attackChanged && !statusChanged) continue;
            var lines=new java.util.ArrayList<Component>();
            lines.add(after.held().name());
            after.statuses().amounts().forEach((type,value)->{
                double old=before.statuses().amounts().getOrDefault(type,0.0);
                if(Math.abs(old-value)>1e-7) lines.add(Component.translatable("status.maplesadventure.preview",Component.translatable("status.maplesadventure."+type.id()),
                        dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(old),dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(value)));
            });
            if(requirementsChanged) lines.add(dev.maplesadventure.progression.weapon.client.WeaponRequirementText.status(before.result().satisfied()).copy().append(" → ")
                    .append(dev.maplesadventure.progression.weapon.client.WeaponRequirementText.status(after.result().satisfied())));
            if(after.held().weapon()) {
                  var old=before.attack(); var next=after.attack();
                  next.bundle().channels().entrySet().stream()
                          .max(java.util.Comparator.comparingDouble(e->Math.abs(e.getValue().attackRating()-old.bundle().channels().get(e.getKey()).attackRating())))
                          .filter(e->Math.abs(e.getValue().attackRating()-old.bundle().channels().get(e.getKey()).attackRating())>1e-7)
                          .ifPresent(e->lines.add(Component.translatable("screen.maplesadventure.weapon.channel_preview",e.getKey().displayName(),
                                  dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(old.bundle().channels().get(e.getKey()).attackRating()),
                                  dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(e.getValue().attackRating()))));
                lines.add(Component.translatable("screen.maplesadventure.weapon.ar_preview",
                        dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(old.attackRating()),
                        dev.maplesadventure.progression.weapon.client.WeaponAttackText.number(next.attackRating())));
            }
            lines.add(Component.translatable("screen.maplesadventure.weapon.efficiency_preview",
                    Math.round(before.result().damageMultiplier()*100),Math.round(after.result().damageMultiplier()*100)));
            lines.add(dev.maplesadventure.progression.weapon.client.WeaponRequirementText.skill(before.result().weaponSkillAllowed()).copy().append(" → ")
                    .append(dev.maplesadventure.progression.weapon.client.WeaponRequirementText.skill(after.result().weaponSkillAllowed())));
            for (var a:dev.maplesadventure.progression.weapon.WeaponRequirementProfile.ATTRIBUTES) if(after.held().profile().get(a)>0 && before.attributes().get(a)!=after.attributes().get(a))
                lines.add(Component.translatable("screen.maplesadventure.weapon.draft_requirement",a.displayName(),before.attributes().get(a),after.attributes().get(a),after.held().profile().get(a)));
            int y=box.y()+25;
            outer: for(var line:lines) {
                for(var wrapped:font.split(line,box.width()-14)) {
                    if(y+9>box.bottom()-16) break outer;
                    g.drawString(font,wrapped,box.x()+7,y,GOLD_BRIGHT,false); y+=10;
                }
                y+=2;
            }
            g.drawCenteredString(font,text("details_hint"),box.x()+box.width()/2,box.bottom()-11,MUTED);
            return true;
        }
        return false;
    }

    private void drawSchoolPreview(GuiGraphics g, LevelUpLayout.Box box,
            java.util.List<dev.maplesadventure.progression.spell.SpellSchoolStat> schools) {
        java.util.List<Component> labels = new java.util.ArrayList<>();
        java.util.List<String> values = new java.util.ArrayList<>();
        for (var stat : java.util.List.of(CharacterStat.INTELLIGENCE_SCALING, CharacterStat.FAITH_SCALING, CharacterStat.ARCANE_SCALING)) {
            if (!previewStats.value(stat).differsFrom(baselineStats.value(stat))) continue;
            labels.add(Component.translatable(stat.translationKey()));
            values.add(CharacterStatFormatting.comparison(stat, baselineStats.value(stat), previewStats.value(stat)).getString());
        }
        for (var school : schools) {
            labels.add(school.displayName());
            var before = baselineStats.spellSchools().schools().get(school.schoolId());
            values.add(CharacterStatsScreen.schoolPercent(before.progressionBonus()) + " → " + CharacterStatsScreen.schoolPercent(school.progressionBonus()));
        }
        int rowHeight = box.width() < 210 ? 21 : 13;
        int rows = Math.min(labels.size(), Math.max(1, (box.height() - 37) / rowHeight));
        for (int i = 0; i < rows; i++) {
            int y = box.y() + 24 + i * rowHeight;
            if (box.width() < 210) {
                g.drawString(font, font.substrByWidth(labels.get(i), box.width() - 14).getString(), box.x() + 7, y, TEXT, false);
                drawFitted(g, values.get(i), box.x() + 7, y + 10, box.width() - 14, GOLD_BRIGHT);
            } else {
                int valueWidth = Math.min(font.width(values.get(i)), box.width() * 2 / 3);
                g.drawString(font, font.substrByWidth(labels.get(i), box.width() - valueWidth - 21).getString(), box.x() + 7, y, TEXT, false);
                drawFitted(g, values.get(i), box.right() - valueWidth - 7, y, valueWidth, GOLD_BRIGHT);
            }
        }
    }

    private void drawFitted(GuiGraphics g, String value, int x, int y, int maximumWidth, int color) {
        float scale = Math.min(1.0F, maximumWidth / (float) Math.max(1, font.width(value)));
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(font, value, 0, 0, color, false);
        g.pose().popPose();
    }

    private static String percentage(double multiplier) {
        return String.format(java.util.Locale.ROOT, "%.0f%%", multiplier * 100.0D);
    }
    private void drawDescription(GuiGraphics g) {
        var box = layout.description();
        if (box.height() < 25) return;
        drawPanel(g, box, PANEL_INNER, 0xFF685130);
        g.drawString(font, selected.displayName(), box.x() + 10, box.y() + 9, GOLD_BRIGHT);
        int y = box.y() + 25;
        for (var line : font.split(description(selected), box.width() - 20)) {
            if (y + font.lineHeight > box.bottom() - 6) break;
            g.drawString(font, line, box.x() + 10, y, MUTED);
            y += font.lineHeight + 2;
        }
    }
    private Component description(Attribute attribute) {
        Component base = Component.translatable(attribute.translationKey() + ".description");
        if (attribute == Attribute.VIGOR || attribute == Attribute.MIND || attribute == Attribute.ENDURANCE)
            return base.copy().append("\n").append(text("diminishing_returns"));
        if (attribute == Attribute.INTELLIGENCE || attribute == Attribute.FAITH || attribute == Attribute.ARCANE)
            return base.copy().append("\n").append(Component.translatable("screen.maplesadventure.spell.rating_hint"));
        return base;
    }
    private void drawFeedback(GuiGraphics g) {
        Component status = pending ? text("pending") : feedback;
        if (!pending && !invalid && preview.remainingXp() < 0) status = text("result.insufficient_experience");
        if (status.getString().isEmpty()) return;
        int y = layout.footer().y() - font.lineHeight - 2;
        g.drawCenteredString(font, font.split(status, layout.panel().width() - 30).getFirst(), width / 2, y,
                preview.remainingXp() < 0 || invalid ? 0xFFE08A71 : GOLD_BRIGHT);
    }
    private static void drawPanel(GuiGraphics g, LevelUpLayout.Box box, int fill, int border) {
        if (box.width() <= 0 || box.height() <= 0) return;
        g.fillGradient(box.x(), box.y(), box.right(), box.bottom(), fill, fill & 0xFFF7F7F7);
        g.renderOutline(box.x(), box.y(), box.width(), box.height(), border);
    }
    private int attributeHeaderHeight() {
        return layout.compact() ? 0 : layout.dense() ? 14 : 18;
    }
    private static Component text(String suffix) { return Component.translatable("screen.maplesadventure.level_up." + suffix); }
}
