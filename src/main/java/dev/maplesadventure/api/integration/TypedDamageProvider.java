package dev.maplesadventure.api.integration;

import java.util.Optional;
import net.minecraft.world.damagesource.DamageSource;
import dev.maplesadventure.api.damage.TypedDamage;

/**
 * Pure descriptive callback on the server thread. Never call hurt, change HP, apply ailments,
 * post damage events, or mutate the world here. Return empty for sources you do not own.
 */
@FunctionalInterface
public interface TypedDamageProvider {
    /**
     * @param source existing hit's source
     * @return pressure description, or empty to let other providers try */
    Optional<TypedDamage> describe(DamageSource source);
}
