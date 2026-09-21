package dev.maplesadventure.api.status;

import java.util.Objects;
import java.util.Optional;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Short-lived server source descriptor. Create at application time; do not persist or cache entities.
 * Environmental sources are for genuinely unowned hazards, never for laundering an entity attack.
 * Like every in-process mod API, this trusts integrations not to lie about their source.
 */
public final class StatusSource {
    private final Entity entity;
    private final ResourceLocation integration;
    private StatusSource(Entity entity, ResourceLocation integration) {
        this.entity = entity; this.integration = integration;
    }
    /**
     * @param entity actual responsible entity
     * @return source retaining its identity for validation */
    public static StatusSource fromEntity(Entity entity) { return new StatusSource(Objects.requireNonNull(entity), null); }
    /**
     * @param projectile actual projectile, whose owner is revalidated
     * @return projectile source */
    public static StatusSource fromProjectile(Projectile projectile) { return fromEntity(projectile); }
    /**
     * @return unowned environmental source; do not use for player or boss attacks */
    public static StatusSource environment() { return new StatusSource(null, null); }
    /**
     * @param id integration ID
     * @param source actual source entity
     * @return attributed integration source */
    public static StatusSource integration(ResourceLocation id, Entity source) {
        return new StatusSource(Objects.requireNonNull(source), Objects.requireNonNull(id));
    }
    /**
     * @return actual source, empty only for an unowned environment */
    public Optional<Entity> entity() { return Optional.ofNullable(entity); }
    /**
     * @return diagnostic integration ID, empty for ordinary sources */
    public Optional<ResourceLocation> integrationId() { return Optional.ofNullable(integration); }
}
