package dev.maplesadventure.multiplayer.encounter.boss;

import java.util.Optional;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.neoforge.entity.PartEntity;

/** Resolves only explicit ownership. It intentionally never guesses lineage from distance or type. */
public final class BossLineageResolver {
    public static Optional<Entity> parentOf(Entity entity) {
        if (entity instanceof Projectile projectile && projectile.getOwner() != null
                && projectile.getOwner() != entity) return Optional.of(projectile.getOwner());
        if (entity instanceof AreaEffectCloud cloud && cloud.getOwner() != null
                && cloud.getOwner() != entity) return Optional.of(cloud.getOwner());
        if (entity instanceof PartEntity<?> part && part.getParent() != entity)
            return Optional.of(part.getParent());
        return BossIntegrationRegistry.classifyParent(entity);
    }

    public static BossSpawnClassification classification(Entity entity) {
        if (entity instanceof Projectile) return BossSpawnClassification.projectile();
        if (entity instanceof AreaEffectCloud) return BossSpawnClassification.effect();
        if (entity instanceof PartEntity<?>) return BossSpawnClassification.part();
        return BossIntegrationRegistry.classify(entity).orElse(BossSpawnClassification.child(false));
    }

    private BossLineageResolver() {}
}
