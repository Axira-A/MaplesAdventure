package examples;

import com.mojang.serialization.Codec;
import dev.maplesadventure.api.editor.*;
import dev.maplesadventure.api.editor.logic.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Compile-checked experimental API example. Register during common setup, not each world load. */
public final class LogicAuthoringExample {
    private static final ResourceLocation EVENT = ResourceLocation.parse("example:checkpoint_visited");
    public record Flag(ResourceLocation id) {}
    private static ComponentDescriptor<Flag> schema(String name) {
        return new ComponentDescriptor<>(ResourceLocation.fromNamespaceAndPath("example", name), 1,
                "editor.example." + name, () -> new Flag(ResourceLocation.parse("example:checkpoint")),
                ResourceLocation.CODEC.xmap(Flag::new, Flag::id).fieldOf("flag").codec(),
                List.of(new ComponentDescriptor.Field<>(new InspectorField("flag", "editor.example.flag",
                        EditorValue.Kind.RESOURCE_LOCATION, 0, 0, 0, 256, false, false, List.of(), null),
                        v -> new EditorValue(EditorValue.Kind.RESOURCE_LOCATION, v.id().toString()),
                        (v, field) -> new Flag(ResourceLocation.parse(field.text())))), v -> List.of());
    }

    public static void register() {
        MaplesAuthoringApi.registerEventType(new EventType<>(new ComponentDescriptor<>(EVENT, 1,
                "editor.example.checkpoint_visited", () -> Boolean.TRUE,
                Codec.BOOL.fieldOf("event").codec(), List.of(), v -> List.of()), true, false));
        MaplesAuthoringApi.registerConditionType(new ConditionType<>(schema("flag_not_set"), false,
                (ctx, data) -> !MaplesAuthoringApi.worldFlag(ctx, data.id())));
        MaplesAuthoringApi.registerActionType(new ActionType<>(schema("remember_checkpoint"), false,
                (ctx, data) -> MaplesAuthoringApi.worldFlag(ctx, data.id(), true)));
    }

    /** Call from a trusted server gameplay hook; never expose this as an unchecked client request. */
    public static void checkpointVisited(ServerPlayer player, ResourceLocation scene, UUID object) {
        MaplesAuthoringApi.emit(player.serverLevel(), scene, object, Optional.of(player), EVENT);
    }
    private LogicAuthoringExample() {}
}
