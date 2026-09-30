package dev.maplesadventure.api.editor.logic;
import dev.maplesadventure.api.editor.ComponentDescriptor;
import java.util.function.BiPredicate;
/** Evaluators must not mutate gameplay or authoring data. */
public record ConditionType<T>(ComponentDescriptor<T> schema, boolean requiresPlayer, BiPredicate<LogicContext,T> evaluator) {}
