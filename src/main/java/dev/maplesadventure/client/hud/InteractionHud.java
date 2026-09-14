package dev.maplesadventure.client.hud;

import dev.maplesadventure.client.input.AdventureKeyMappings;
import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.InteractionCandidate;
import dev.maplesadventure.interaction.InteractionTargetManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.network.chat.Component;

public final class InteractionHud implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        InteractionTargetManager manager = InteractionTargetManager.getInstance();
        InteractionCandidate candidate = manager.getCurrentCandidate();
        if (minecraft.options.hideGui || minecraft.screen != null || candidate == null) {
            return;
        }

        if (InteractionConfig.PROMPT_ENABLED.get()) {
            renderPrompt(graphics, minecraft.font, manager, candidate);
        }
        if (InteractionConfig.DEBUG.get()) {
            renderDebug(graphics, minecraft.font, manager, candidate);
        }
    }

    private static void renderPrompt(
            GuiGraphics graphics,
            Font font,
            InteractionTargetManager manager,
            InteractionCandidate candidate
    ) {
        Component interactKey = AdventureKeyMappings.INTERACT.getTranslatedKeyMessage();
        Component prompt = candidate.provider().getPrompt(
                interactKey, candidate.displayName(), InteractionConfig.SHOW_TARGET_NAME.get()
        );
        boolean showSwitch = InteractionConfig.SHOW_SWITCH_HINT.get() && manager.getCandidateCount() > 1;
        Component switchPrompt = Component.translatable(
                "hud.maplesadventure.switch_target",
                AdventureKeyMappings.SWITCH_TARGET.getTranslatedKeyMessage()
        );

        double scale = InteractionConfig.HUD_SCALE.get();
        int centerX = graphics.guiWidth() / 2;
        int baseY = graphics.guiHeight() - InteractionConfig.PROMPT_BOTTOM_OFFSET.get();
        int maxWidth = Math.max(font.width(prompt), showSwitch ? font.width(switchPrompt) : 0);
        int lines = showSwitch ? 2 : 1;
        int alpha = (int) Math.round(255.0D * InteractionConfig.HUD_OPACITY.get());
        int textColor = (alpha << 24) | 0x00FFFFFF;
        int background = ((int) Math.round(110.0D * InteractionConfig.HUD_OPACITY.get()) << 24);

        graphics.pose().pushPose();
        graphics.pose().translate(centerX, baseY, 0.0F);
        graphics.pose().scale((float) scale, (float) scale, 1.0F);
        int left = -maxWidth / 2 - 6;
        int top = -4;
        graphics.fill(left, top, left + maxWidth + 12, top + lines * 11 + 5, background);
        graphics.drawCenteredString(font, prompt, 0, 0, textColor);
        if (showSwitch) {
            graphics.drawCenteredString(font, switchPrompt, 0, 11, textColor);
        }
        graphics.pose().popPose();
    }

    private static void renderDebug(
            GuiGraphics graphics,
            Font font,
            InteractionTargetManager manager,
            InteractionCandidate candidate
    ) {
        Minecraft minecraft = Minecraft.getInstance();
        int color = 0xFFE8E8E8;
        int y = 8;
        graphics.drawString(font, "Interaction candidates: " + manager.getCandidateCount(), 8, y, color, true);
        y += 10;
        graphics.drawString(font, "Target: " + candidate.target().debugDescription(minecraft.level), 8, y, color, true);
        y += 10;
        graphics.drawString(font, String.format(java.util.Locale.ROOT, "distance=%.2f score=%.3f", candidate.distance(), candidate.score()), 8, y, color, true);
        y += 10;
        graphics.drawString(font, "provider=" + candidate.provider().id() + " LOS=" + candidate.lineOfSight(), 8, y, color, true);
        y += 10;
        graphics.drawString(font, "validation=" + manager.getLastValidation(), 8, y, color, true);
    }
}
