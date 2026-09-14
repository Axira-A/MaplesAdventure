package dev.maplesadventure.multiplayer.echo.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.echo.EchoAppearanceSnapshot;
import dev.maplesadventure.multiplayer.echo.EchoFrame;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.PlayerSkin;
import net.neoforged.fml.ModList;

/** Optional renderer boundary. Epic Fight classes are never resolved unless its mod is installed. */
public final class EpicFightEchoRenderBridge {
    private static final String RENDERER =
            "dev.maplesadventure.integration.epicfight.echo.EpicFightEchoRenderer";
    private static Renderer renderer;
    private static boolean initialized;
    private static boolean warned;

    public static boolean render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, EchoFrame frame,
                                 EchoAppearanceSnapshot appearance, PlayerSkin skin, float alpha) {
        if (!frame.epicFight().active()) return false;
        Renderer active = renderer();
        if (active == null) return false;
        try {
            return active.render(poseStack, buffers, frame, appearance, skin, alpha);
        } catch (RuntimeException | LinkageError exception) {
            if (!warned) {
                warned = true;
                MaplesAdventure.LOGGER.warn("Epic Fight historical echo render failed; using vanilla echo pose", exception);
            }
            return false;
        }
    }

    private static Renderer renderer() {
        if (initialized) return renderer;
        initialized = true;
        if (!ModList.get().isLoaded("epicfight")) return null;
        try {
            renderer = (Renderer) Class.forName(RENDERER).getConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                 | IllegalAccessException | InvocationTargetException | LinkageError exception) {
            if (!warned) {
                warned = true;
                MaplesAdventure.LOGGER.warn("Epic Fight historical echo renderer is unavailable", exception);
            }
        }
        return renderer;
    }

    public interface Renderer {
        boolean render(PoseStack poseStack, MultiBufferSource.BufferSource buffers, EchoFrame frame,
                       EchoAppearanceSnapshot appearance, PlayerSkin skin, float alpha);
    }
    private EpicFightEchoRenderBridge() {}
}
