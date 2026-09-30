package dev.maplesadventure.api.editor.logic;
import dev.maplesadventure.api.editor.ComponentDescriptor;
import java.util.function.BiConsumer;
/** Synchronous, ordered server action. Never receives editable SavedData. */
public record ActionType<T>(ComponentDescriptor<T> schema, boolean requiresPlayer, BiConsumer<LogicContext,T> executor) {}
