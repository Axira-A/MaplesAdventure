package dev.maplesadventure.multiplayer.phase.client;

import java.util.ArrayDeque;
import net.minecraft.world.entity.Entity;

/**
 * Preserves the source identity while an entity is being ticked. This lets the final
 * particle call be filtered without guessing its source from a coordinate or particle id.
 */
public final class PhaseSensorySourceContext {
    private static final ThreadLocal<ArrayDeque<Boolean>> SUPPRESSION_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    public static void push(Entity source) {
        SUPPRESSION_STACK.get().push(!PhaseSensoryPolicy.shouldExposeEntitySource(source));
    }

    public static void pop() {
        ArrayDeque<Boolean> stack = SUPPRESSION_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
        if (stack.isEmpty()) {
            SUPPRESSION_STACK.remove();
        }
    }

    public static boolean suppressesEntityParticles() {
        ArrayDeque<Boolean> stack = SUPPRESSION_STACK.get();
        return !stack.isEmpty() && stack.peek();
    }

    private PhaseSensorySourceContext() {
    }
}
