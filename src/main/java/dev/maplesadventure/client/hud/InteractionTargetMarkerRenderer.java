package dev.maplesadventure.client.hud;

import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.InteractionCandidate;
import dev.maplesadventure.interaction.InteractionRegistry;
import dev.maplesadventure.interaction.InteractionTargetManager;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public final class InteractionTargetMarkerRenderer implements LayeredDraw.Layer {
    @Override
    public void render(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        InteractionCandidate candidate = InteractionTargetManager.getInstance().getCurrentCandidate();
        if (!InteractionConfig.MARKER_ENABLED.get() || minecraft.options.hideGui || minecraft.screen != null
                || minecraft.level == null || candidate == null || !candidate.lineOfSight()) {
            return;
        }

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(false);
        Vec3 markerPosition;
        try {
            markerPosition = candidate.provider().getMarkerPosition(minecraft.level, candidate.target(), partialTick);
        } catch (RuntimeException exception) {
            InteractionRegistry.getInstance().reportProviderError(candidate.provider(), candidate.target(), minecraft.level, exception);
            return;
        } catch (LinkageError error) {
            InteractionRegistry.getInstance().reportProviderError(candidate.provider(), candidate.target(), minecraft.level, error);
            return;
        }
        ScreenPoint point = project(markerPosition, graphics.guiWidth(), graphics.guiHeight());
        if (point == null) {
            return;
        }

        int alpha = (int) Math.round(190.0D * InteractionConfig.HUD_OPACITY.get());
        int color = (alpha << 24) | 0x00F4E8B8;
        int half = Math.max(4, (int) Math.round(7.0D * InteractionConfig.HUD_SCALE.get()));
        drawDiamond(graphics, point.x(), point.y(), half, color);
    }

    private static void drawDiamond(GuiGraphics graphics, int centerX, int centerY, int half, int color) {
        for (int offset = 0; offset <= half; offset++) {
            int horizontal = half - offset;
            graphics.fill(centerX - horizontal, centerY - offset, centerX - horizontal + 1, centerY - offset + 1, color);
            graphics.fill(centerX + horizontal, centerY - offset, centerX + horizontal + 1, centerY - offset + 1, color);
            graphics.fill(centerX - horizontal, centerY + offset, centerX - horizontal + 1, centerY + offset + 1, color);
            graphics.fill(centerX + horizontal, centerY + offset, centerX + horizontal + 1, centerY + offset + 1, color);
        }
    }

    private static ScreenPoint project(Vec3 worldPosition, int width, int height) {
        Minecraft minecraft = Minecraft.getInstance();
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 relative = worldPosition.subtract(camera.getPosition());
        Vector3f look = camera.getLookVector();
        Vector3f up = camera.getUpVector();
        Vector3f left = camera.getLeftVector();
        double depth = dot(relative, look);
        if (depth <= 0.05D) {
            return null;
        }

        double verticalFov = Math.toRadians(minecraft.options.fov().get());
        double tanHalfFov = Math.tan(verticalFov * 0.5D);
        double aspect = (double) width / Math.max(1, height);
        double normalizedX = dot(relative, left) / (depth * tanHalfFov * aspect);
        double normalizedY = dot(relative, up) / (depth * tanHalfFov);
        if (Math.abs(normalizedX) > 1.05D || Math.abs(normalizedY) > 1.05D) {
            return null;
        }

        int screenX = (int) Math.round(width * 0.5D * (1.0D - normalizedX));
        int screenY = (int) Math.round(height * 0.5D * (1.0D - normalizedY));
        return new ScreenPoint(screenX, screenY);
    }

    private static double dot(Vec3 vector, Vector3f basis) {
        return vector.x * basis.x() + vector.y * basis.y() + vector.z * basis.z();
    }

    private record ScreenPoint(int x, int y) {
    }
}
