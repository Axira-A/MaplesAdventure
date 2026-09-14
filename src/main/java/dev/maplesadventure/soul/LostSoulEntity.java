package dev.maplesadventure.soul;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import dev.maplesadventure.client.soul.LostSoulVisualEffects;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/** A persistent, non-living, non-combat entity. All visual elements are rendered from this one entity. */
public final class LostSoulEntity extends Entity {
    private static final EntityDataAccessor<Optional<UUID>> OWNER_UUID = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<Optional<UUID>> SOUL_ID = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.OPTIONAL_UUID
    );
    private static final EntityDataAccessor<String> OWNER_NAME = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<Long> GENERATION = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.LONG
    );
    private static final EntityDataAccessor<Float> RECOVERY_DISTANCE = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.FLOAT
    );
    private static final EntityDataAccessor<String> TEXTURE_VALUE = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.STRING
    );
    private static final EntityDataAccessor<String> TEXTURE_SIGNATURE = SynchedEntityData.defineId(
            LostSoulEntity.class, EntityDataSerializers.STRING
    );

    private int storedExperience;
    private long creationTime;
    private @Nullable GameProfile cachedProfile;

    public LostSoulEntity(EntityType<? extends LostSoulEntity> type, Level level) {
        super(type, level);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public void initialize(LostSoulRecord record, GameProfile ownerProfile, float recoveryDistance) {
        setUUID(record.soulId());
        entityData.set(OWNER_UUID, Optional.of(record.ownerUuid()));
        entityData.set(SOUL_ID, Optional.of(record.soulId()));
        entityData.set(OWNER_NAME, record.ownerName());
        entityData.set(GENERATION, record.generation());
        entityData.set(RECOVERY_DISTANCE, recoveryDistance);
        storedExperience = record.storedExperience();
        creationTime = record.creationTime();
        copyTexture(ownerProfile);
        updateLocalizedName();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(OWNER_UUID, Optional.empty());
        builder.define(SOUL_ID, Optional.empty());
        builder.define(OWNER_NAME, "Player");
        builder.define(GENERATION, 0L);
        builder.define(RECOVERY_DISTANCE, 1.25F);
        builder.define(TEXTURE_VALUE, "");
        builder.define(TEXTURE_SIGNATURE, "");
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        clearFire();
        if (level().isClientSide()) {
            LostSoulVisualEffects.tick(this);
            return;
        }
        if (tickCount == 1 || tickCount % 20 == 0) {
            UUID owner = getOwnerUuid();
            UUID soulId = getSoulId();
            if (owner == null || soulId == null
                    || !LostSoulSavedData.get(level().getServer()).isActive(owner, soulId, getGeneration())) {
                discard();
            }
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        UUID owner = getOwnerUuid();
        if (owner == null || !owner.equals(player.getUUID())) {
            return InteractionResult.PASS;
        }
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return player instanceof ServerPlayer serverPlayer
                && LostSoulRecoveryService.recover(serverPlayer, this)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        entityData.set(OWNER_UUID, tag.hasUUID("Owner") ? Optional.of(tag.getUUID("Owner")) : Optional.empty());
        entityData.set(SOUL_ID, tag.hasUUID("SoulId") ? Optional.of(tag.getUUID("SoulId")) : Optional.empty());
        entityData.set(OWNER_NAME, tag.getString("OwnerName"));
        entityData.set(GENERATION, Math.max(0L, tag.getLong("Generation")));
        entityData.set(RECOVERY_DISTANCE, Math.max(0.25F, tag.getFloat("RecoveryDistance")));
        entityData.set(TEXTURE_VALUE, tag.getString("TextureValue"));
        entityData.set(TEXTURE_SIGNATURE, tag.getString("TextureSignature"));
        storedExperience = Math.max(0, tag.getInt("StoredExperience"));
        creationTime = tag.getLong("CreationTime");
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
        updateLocalizedName();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        UUID owner = getOwnerUuid();
        UUID soulId = getSoulId();
        if (owner != null) {
            tag.putUUID("Owner", owner);
        }
        if (soulId != null) {
            tag.putUUID("SoulId", soulId);
        }
        tag.putString("OwnerName", getOwnerName());
        tag.putLong("Generation", getGeneration());
        tag.putFloat("RecoveryDistance", getRecoveryDistance());
        tag.putString("TextureValue", entityData.get(TEXTURE_VALUE));
        tag.putString("TextureSignature", entityData.get(TEXTURE_SIGNATURE));
        tag.putInt("StoredExperience", storedExperience);
        tag.putLong("CreationTime", creationTime);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        super.onSyncedDataUpdated(key);
        if (key.equals(OWNER_UUID) || key.equals(OWNER_NAME)
                || key.equals(TEXTURE_VALUE) || key.equals(TEXTURE_SIGNATURE)) {
            cachedProfile = null;
        }
    }

    public @Nullable UUID getOwnerUuid() {
        return entityData.get(OWNER_UUID).orElse(null);
    }

    public @Nullable UUID getSoulId() {
        return entityData.get(SOUL_ID).orElse(null);
    }

    public String getOwnerName() {
        String name = entityData.get(OWNER_NAME);
        return name.isBlank() ? "Player" : name;
    }

    public long getGeneration() {
        return entityData.get(GENERATION);
    }

    public float getRecoveryDistance() {
        return entityData.get(RECOVERY_DISTANCE);
    }

    public @Nullable GameProfile getOwnerProfile() {
        UUID owner = getOwnerUuid();
        if (owner == null) {
            return null;
        }
        if (cachedProfile == null) {
            GameProfile profile = new GameProfile(owner, getOwnerName());
            String textureValue = entityData.get(TEXTURE_VALUE);
            String textureSignature = entityData.get(TEXTURE_SIGNATURE);
            if (!textureValue.isBlank()) {
                Property property = textureSignature.isBlank()
                        ? new Property("textures", textureValue)
                        : new Property("textures", textureValue, textureSignature);
                profile.getProperties().put("textures", property);
            }
            cachedProfile = profile;
        }
        return cachedProfile;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean skipAttackInteraction(Entity entity) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double x, double y, double z) {
        // Logical marker entities never accept physical impulses.
    }

    private void copyTexture(GameProfile profile) {
        Collection<Property> textures = profile.getProperties().get("textures");
        Property property = textures.stream().findFirst().orElse(null);
        if (property == null) {
            return;
        }
        entityData.set(TEXTURE_VALUE, property.value());
        entityData.set(TEXTURE_SIGNATURE, property.signature() == null ? "" : property.signature());
        cachedProfile = null;
    }

    private void updateLocalizedName() {
        setCustomName(Component.translatable("entity.maplesadventure.lost_soul.name", getOwnerName()));
        setCustomNameVisible(true);
    }
}
