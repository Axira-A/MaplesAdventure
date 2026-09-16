package dev.maplesadventure.progression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.progression.runtime.PlayerResourceCheckpoint;
import dev.maplesadventure.progression.encumbrance.CombatSkillInitializationState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ProgressionAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MaplesAdventure.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerAttributeState>> PLAYER_ATTRIBUTES =
            TYPES.register("player_attributes", () -> AttachmentType.serializable(PlayerAttributeState::new)
                    .copyOnDeath()
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<PlayerResourceCheckpoint>> RESOURCE_CHECKPOINT =
            TYPES.register("resource_checkpoint", () -> AttachmentType.serializable(PlayerResourceCheckpoint::new)
                    .build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CombatSkillInitializationState>> COMBAT_SKILL_INITIALIZATION =
            TYPES.register("combat_skill_initialization", () -> AttachmentType.serializable(CombatSkillInitializationState::new)
                    .copyOnDeath()
                    .build());

    public static void register(IEventBus modBus) { TYPES.register(modBus); }
    public static final DeferredHolder<AttachmentType<?>,AttachmentType<dev.maplesadventure.progression.status.StatusRuntimeState>> STATUS_RUNTIME =
            TYPES.register("status_runtime",()->AttachmentType.serializable(()->new dev.maplesadventure.progression.status.StatusRuntimeState()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<dev.maplesadventure.progression.defense.EntityDefenseProfileRef>> ENTITY_DEFENSE_PROFILE =
            TYPES.register("entity_defense_profile", () -> AttachmentType.serializable(
                    () -> new dev.maplesadventure.progression.defense.EntityDefenseProfileRef()).build());
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<dev.maplesadventure.progression.weapon.ProjectileRequirementPenalty>> PROJECTILE_REQUIREMENT =
            TYPES.register("projectile_weapon_requirement", () -> AttachmentType.serializable(
                    () -> new dev.maplesadventure.progression.weapon.ProjectileRequirementPenalty()).build());
    private ProgressionAttachments() {}
}
