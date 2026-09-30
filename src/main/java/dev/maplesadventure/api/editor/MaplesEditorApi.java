package dev.maplesadventure.api.editor;

import dev.maplesadventure.editor.EditorFoundation;
import dev.maplesadventure.editor.EditorPermissions;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Experimental Editor Foundation API, separate from the existing stable gameplay API v1. */
public final class MaplesEditorApi {
    /** Initialize during common setup. Duplicate IDs and late registrations are rejected. */
    public static <T> void registerComponent(ComponentDescriptor<T> descriptor) { EditorFoundation.COMPONENTS.register(descriptor); }
    /** Add a trusted server permission grant (e.g. an external map-author role); no client authority. */
    public static void registerPermission(ResourceLocation id, Predicate<ServerPlayer> permission) { EditorPermissions.register(id,permission); }
    public static boolean isEditorAuthorized(ServerPlayer player) { return EditorPermissions.authorized(player); }
    /** Runtime gameplay triggers must exclude authors, independently of vanilla spectator policy. */
    public static boolean isEditing(ServerPlayer player) { return dev.maplesadventure.editor.EditorSessionService.active(player); }
    private MaplesEditorApi() {}
}
