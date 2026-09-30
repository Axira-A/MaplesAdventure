package dev.maplesadventure.api.editor.logic;

import java.util.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;

/** Experimental, immutable event identity. No mutable authoring scene is exposed. */
public record LogicContext(MinecraftServer server, ServerLevel level, ResourceLocation sceneId,
                           UUID sourceObjectId, Optional<ServerPlayer> player, ResourceLocation eventType) {
    public LogicContext { Objects.requireNonNull(server); Objects.requireNonNull(level); Objects.requireNonNull(sceneId);
        Objects.requireNonNull(sourceObjectId); Objects.requireNonNull(player); Objects.requireNonNull(eventType); }
}
