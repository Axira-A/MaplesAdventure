package dev.maplesadventure.client.input;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;

public final class AdventureKeyMappings {
    public static final String CATEGORY = "key.categories.maplesadventure";

    public static final KeyMapping INTERACT = new KeyMapping(
            "key.maplesadventure.interact",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_F,
            CATEGORY
    );

    public static final KeyMapping SWITCH_TARGET = new KeyMapping(
            "key.maplesadventure.switch_interaction_target",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_Y,
            CATEGORY
    );

    public static final KeyMapping OPEN_MULTIPLAYER_MENU = new KeyMapping(
            "key.maplesadventure.open_multiplayer_menu",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_L,
            CATEGORY
    );

    private AdventureKeyMappings() {
    }
}
