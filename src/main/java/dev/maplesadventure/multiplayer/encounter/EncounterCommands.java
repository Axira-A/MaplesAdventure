package dev.maplesadventure.multiplayer.encounter;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.multiplayer.phase.PhaseId;
import dev.maplesadventure.multiplayer.phase.PhaseManager;
import java.util.Comparator;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.UuidArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.maplesadventure.multiplayer.phase.loot.PhaseLootService;
import dev.maplesadventure.multiplayer.encounter.boss.BossEncounterRuntime;
import dev.maplesadventure.multiplayer.encounter.boss.PhaseBossBarService;

public final class EncounterCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new EncounterCommands()); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure").then(buildEncounterRoot()));
        event.getDispatcher().register(Commands.literal("ma").then(buildEncounterRoot()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildEncounterRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("encounter").requires(source -> source.hasPermission(2));
        root.then(Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.literal("common").executes(context -> create(context.getSource(),
                                id(context.getSource(), StringArgumentType.getString(context, "id")), EncounterType.COMMON)))
                        .then(Commands.literal("boss").executes(context -> create(context.getSource(),
                                id(context.getSource(), StringArgumentType.getString(context, "id")), EncounterType.BOSS)))));
        root.then(Commands.literal("addspawn")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                .executes(context -> addSpawn(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        ResourceLocationArgument.getId(context, "entity_type"))))));
        root.then(Commands.literal("setspawnrole")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("spawn_point", UuidArgument.uuid())
                                .then(Commands.literal("primary").executes(context -> setSpawnRole(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        UuidArgument.getUuid(context, "spawn_point"), EncounterSpawnRole.BOSS_PRIMARY)))
                                .then(Commands.literal("add").executes(context -> setSpawnRole(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        UuidArgument.getUuid(context, "spawn_point"), EncounterSpawnRole.BOSS_ADD)))
                                .then(Commands.literal("normal").executes(context -> setSpawnRole(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        UuidArgument.getUuid(context, "spawn_point"), EncounterSpawnRole.NORMAL))))));
        root.then(Commands.literal("info").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> info(context.getSource(), id(context.getSource(), StringArgumentType.getString(context, "id"))))));
        root.then(Commands.literal("list").executes(context -> list(context.getSource())));
        root.then(Commands.literal("remove").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> remove(context.getSource(), id(context.getSource(), StringArgumentType.getString(context, "id"))))));
        root.then(Commands.literal("setradius").then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("radius", DoubleArgumentType.doubleArg(1.0D, 256.0D))
                        .executes(context -> radius(context.getSource(),
                                id(context.getSource(), StringArgumentType.getString(context, "id")),
                                DoubleArgumentType.getDouble(context, "radius"))))));
        root.then(Commands.literal("reset").then(Commands.argument("id", StringArgumentType.word())
                .executes(context -> reset(context.getSource(),
                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                        context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player()).executes(context -> reset(context.getSource(),
                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                        EntityArgument.getPlayer(context, "player"))))));
        root.then(Commands.literal("resetphase").then(Commands.argument("player", EntityArgument.player())
                .executes(context -> resetPhase(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("bossstage").then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("stage", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> bossStage(
                                context.getSource(), id(context.getSource(), StringArgumentType.getString(context, "id")),
                                EntityArgument.getPlayer(context, "player"),
                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "stage")))))));
        var replaceBossCommand = Commands.literal("replaceboss")
                .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                .then(Commands.argument("stage", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 16))
                                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> replaceBoss(
                                                context.getSource(),
                                                id(context.getSource(), StringArgumentType.getString(context, "id")),
                                                ResourceLocationArgument.getId(context, "entity_type"),
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "stage"),
                                                EntityArgument.getPlayer(context, "player")))))));
        root.then(Commands.literal("debug")
                .executes(context -> debug(context.getSource().getPlayerOrException()))
                .then(replaceBossCommand)
                .then(Commands.literal("bosschild")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("entity_type", ResourceLocationArgument.id())
                                        .executes(context -> bossChild(context.getSource(),
                                                id(context.getSource(), StringArgumentType.getString(context, "id")),
                                                ResourceLocationArgument.getId(context, "entity_type"),
                                                context.getSource().getPlayerOrException()))
                                        .then(Commands.argument("player", EntityArgument.player()).executes(context -> bossChild(
                                                context.getSource(),
                                                id(context.getSource(), StringArgumentType.getString(context, "id")),
                                                ResourceLocationArgument.getId(context, "entity_type"),
                                                EntityArgument.getPlayer(context, "player")))))))
                .then(Commands.literal("bossprojectile")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> bossProjectile(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> bossProjectile(
                                        context.getSource(), id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        EntityArgument.getPlayer(context, "player"))))))
                .then(Commands.literal("bosseffect")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(context -> bossEffect(context.getSource(),
                                        id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        context.getSource().getPlayerOrException()))
                                .then(Commands.argument("player", EntityArgument.player()).executes(context -> bossEffect(
                                        context.getSource(), id(context.getSource(), StringArgumentType.getString(context, "id")),
                                        EntityArgument.getPlayer(context, "player")))))));
        return root;
    }

    private static ResourceLocation id(CommandSourceStack source, String value) {
        ResourceLocation parsed = ResourceLocation.tryParse(value.contains(":") ? value : MaplesAdventure.MOD_ID + ":" + value);
        if (parsed == null) throw new IllegalArgumentException("Invalid encounter id: " + value);
        return parsed;
    }

    private static int create(CommandSourceStack source, ResourceLocation id, EncounterType type) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        if (data.definition(id).isPresent()) { source.sendFailure(Component.literal("Encounter already exists: " + id)); return 0; }
        data.putDefinition(new EncounterDefinition(id, player.level().dimension(), type, player.position(), 24.0D, java.util.List.of()));
        source.sendSuccess(() -> Component.literal("Created " + type + " encounter " + id + " at " + player.position()), true);
        return 1;
    }

    private static int addSpawn(CommandSourceStack source, ResourceLocation id, ResourceLocation typeId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition definition = data.definition(id).orElse(null);
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        if (definition == null) { source.sendFailure(Component.literal("Unknown encounter: " + id)); return 0; }
        if (type == null || !(type.create(player.serverLevel()) instanceof Mob)) {
            source.sendFailure(Component.literal("Entity type is not a Mob: " + typeId)); return 0;
        }
        EncounterSpawnRole role = definition.type() == EncounterType.COMMON ? EncounterSpawnRole.NORMAL
                : definition.primaryCount() == 0L ? EncounterSpawnRole.BOSS_PRIMARY : EncounterSpawnRole.BOSS_ADD;
        EncounterSpawnPoint point = new EncounterSpawnPoint(UUID.randomUUID(), player.position(), player.getYRot(), typeId, role);
        data.putDefinition(definition.withSpawn(point));
        source.sendSuccess(() -> Component.literal("Added spawn " + point.spawnPointId() + " type=" + typeId
                + " role=" + role + " to " + id), true);
        return 1;
    }

    private static int setSpawnRole(CommandSourceStack source, ResourceLocation id, UUID spawnPoint,
                                    EncounterSpawnRole role) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition definition = data.definition(id).orElse(null);
        if (definition == null) { source.sendFailure(Component.literal("Unknown encounter: " + id)); return 0; }
        EncounterDefinition updated = definition.withSpawnRole(spawnPoint, role).orElse(null);
        if (updated == null) { source.sendFailure(Component.literal("Invalid spawn point/role for " + id)); return 0; }
        data.putDefinition(updated);
        source.sendSuccess(() -> Component.literal("Set spawn " + spawnPoint + " role=" + role), true);
        return 1;
    }

    private static int info(CommandSourceStack source, ResourceLocation id) {
        EncounterDefinition definition = EncounterSavedData.get(source.getServer()).definition(id).orElse(null);
        if (definition == null) { source.sendFailure(Component.literal("Unknown encounter: " + id)); return 0; }
        StringBuilder text = new StringBuilder("id=").append(id).append(" type=").append(definition.type())
                .append(" dimension=").append(definition.dimension().location()).append(" anchor=").append(definition.anchor())
                .append(" radius=").append(definition.activationRadius()).append(" spawns=").append(definition.spawnPoints().size());
        text.append(" primary=").append(definition.primaryCount()).append(" spawnRoles=")
                .append(definition.spawnPoints().stream().map(point -> point.spawnPointId() + ":" + point.role()).toList());
        EncounterSavedData.get(source.getServer()).fogGate(id).ifPresent(gate -> text.append(" fogGate=")
                .append(gate.gateId()).append(" blocks=").append(gate.fogBlocks().length)
                .append(" inside=").append(gate.insideDirection()));
        if (source.getEntity() instanceof ServerPlayer player) {
            PhaseId phase = PhaseManager.state(player).phaseId();
            PhaseEncounterState state = EncounterSavedData.get(source.getServer()).state(phase, definition);
            text.append(" phase=").append(phase).append(" status=").append(state.status())
                    .append(" generation=").append(state.generation()).append(" stage=").append(state.bossStage())
                    .append(" living=").append(state.livingEntities().size());
            if (state.bossAttempt() != null) {
                text.append(" attempt=").append(state.bossAttempt().attemptId())
                        .append(" primaryBoss=").append(state.bossAttempt().primaryBossUuid())
                        .append(" partySize=").append(state.bossAttempt().partySize())
                        .append(" healthMultiplier=").append(state.bossAttempt().healthMultiplier());
                var level = source.getServer().getLevel(definition.dimension());
                Entity primary = level == null ? null : level.getEntity(state.bossAttempt().primaryBossUuid());
                if (primary instanceof LivingEntity living) {
                    text.append(" primaryLoaded=true primaryType=")
                            .append(BuiltInRegistries.ENTITY_TYPE.getKey(primary.getType()))
                            .append(" primaryHealth=").append(living.getHealth()).append('/').append(living.getMaxHealth())
                            .append(" primaryPos=").append(primary.position());
                } else {
                    text.append(" primaryLoaded=false");
                }
            }
        }
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        return 1;
    }

    private static int list(CommandSourceStack source) {
        var definitions = EncounterSavedData.get(source.getServer()).definitions().stream()
                .sorted(Comparator.comparing(definition -> definition.encounterId().toString())).toList();
        source.sendSuccess(() -> Component.literal("Encounters (" + definitions.size() + "): "
                + definitions.stream().map(definition -> definition.encounterId().toString()).toList()), false);
        return definitions.size();
    }

    private static int remove(CommandSourceStack source, ResourceLocation id) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        var affectedPhases = data.runtimeSnapshot().keySet().stream().filter(key -> key.encounterId().equals(id))
                .map(PhaseEncounterKey::phaseId).distinct().toList();
        EncounterDefinition definition = data.removeDefinition(id).orElse(null);
        if (definition == null) { source.sendFailure(Component.literal("Unknown encounter: " + id)); return 0; }
        for (PhaseId phase : affectedPhases) {
            PhaseBossBarService.remove(phase, id);
            EncounterManager.discardLoaded(source.getServer(), phase, id);
            PhaseLootService.discardLoadedCommon(source.getServer(), phase, id);
        }
        source.sendSuccess(() -> Component.literal("Removed encounter " + id), true);
        return 1;
    }

    private static int radius(CommandSourceStack source, ResourceLocation id, double radius) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition definition = data.definition(id).orElse(null);
        if (definition == null) { source.sendFailure(Component.literal("Unknown encounter: " + id)); return 0; }
        data.putDefinition(definition.withRadius(radius));
        source.sendSuccess(() -> Component.literal("Set " + id + " activation radius to " + radius), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ResourceLocation id, ServerPlayer player) {
        boolean reset = EncounterResetService.resetEncounter(source.getServer(), PhaseManager.state(player).phaseId(), id, true);
        if (!reset) { source.sendFailure(Component.literal("Could not reset encounter " + id)); return 0; }
        source.sendSuccess(() -> Component.literal("Reset " + id + " for phase of " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int resetPhase(CommandSourceStack source, ServerPlayer player) {
        EncounterResetService.resetPhase(source.getServer(), PhaseManager.state(player).phaseId(), EncounterResetReason.ADMIN);
        source.sendSuccess(() -> Component.literal("Reset encounter phase for " + player.getGameProfile().getName()), true);
        return 1;
    }

    private static int bossStage(CommandSourceStack source, ResourceLocation id, ServerPlayer player, int stage) {
        boolean changed = EncounterManager.setBossStage(source.getServer(), PhaseManager.state(player).phaseId(), id, stage);
        if (!changed) { source.sendFailure(Component.literal("Boss encounter is not ACTIVE: " + id)); return 0; }
        source.sendSuccess(() -> Component.literal("Set debug boss stage=" + stage + " for " + id), true);
        return 1;
    }

    private static int replaceBoss(CommandSourceStack source, ResourceLocation id, ResourceLocation typeId,
                                   int stage, ServerPlayer phasePlayer) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition definition = data.definition(id).orElse(null);
        if (definition == null || definition.type() != EncounterType.BOSS) {
            source.sendFailure(Component.literal("Unknown BOSS encounter: " + id)); return 0;
        }
        PhaseEncounterState state = data.existingState(PhaseManager.state(phasePlayer).phaseId(), id).orElse(null);
        var attempt = state == null ? null : state.bossAttempt();
        var level = source.getServer().getLevel(definition.dimension());
        var oldEntity = attempt == null || level == null ? null : level.getEntity(attempt.primaryBossUuid());
        var entityType = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        var created = entityType == null || level == null ? null : entityType.create(level);
        if (!(oldEntity instanceof Mob oldBoss) || !(created instanceof Mob newBoss)) {
            if (created != null) created.discard();
            source.sendFailure(Component.literal("Primary is not loaded or replacement is not a Mob")); return 0;
        }
        newBoss.moveTo(oldBoss.getX(), oldBoss.getY(), oldBoss.getZ(), oldBoss.getYRot(), oldBoss.getXRot());
        if (!BossEncounterRuntime.replacePrimary(oldBoss, newBoss, stage)) {
            newBoss.discard();
            source.sendFailure(Component.literal("Boss replacement failed validation")); return 0;
        }
        source.sendSuccess(() -> Component.literal("Replaced primary for " + id + " with " + typeId
                + " stage=" + stage), true);
        return 1;
    }

    private static int bossChild(CommandSourceStack source, ResourceLocation id, ResourceLocation typeId,
                                 ServerPlayer phasePlayer) {
        Mob primary = loadedPrimary(source, id, phasePlayer);
        var type = BuiltInRegistries.ENTITY_TYPE.getOptional(typeId).orElse(null);
        Entity created = primary == null || type == null ? null : type.create(primary.level());
        if (!(created instanceof Mob child)) {
            if (created != null) created.discard();
            source.sendFailure(Component.literal("Loaded primary or child Mob type unavailable"));
            return 0;
        }
        child.moveTo(primary.getX() + 1.0D, primary.getY(), primary.getZ(), primary.getYRot(), 0.0F);
        if (!BossEncounterRuntime.attachChild(primary, child, false)) {
            child.discard();
            source.sendFailure(Component.literal("Could not attach boss child"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Attached CHILD " + typeId + " to " + id), true);
        return 1;
    }

    private static int bossProjectile(CommandSourceStack source, ResourceLocation id, ServerPlayer phasePlayer) {
        Mob primary = loadedPrimary(source, id, phasePlayer);
        Entity created = primary == null ? null : EntityType.ARROW.create(primary.level());
        if (!(created instanceof Projectile projectile)) {
            source.sendFailure(Component.literal("Loaded primary unavailable"));
            return 0;
        }
        projectile.setOwner(primary);
        projectile.moveTo(primary.getX(), primary.getEyeY(), primary.getZ(), primary.getYRot(), 0.0F);
        if (!BossEncounterRuntime.attachProjectile(primary, projectile)) {
            projectile.discard();
            source.sendFailure(Component.literal("Could not attach boss projectile"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Attached PROJECTILE to " + id), true);
        return 1;
    }

    private static int bossEffect(CommandSourceStack source, ResourceLocation id, ServerPlayer phasePlayer) {
        Mob primary = loadedPrimary(source, id, phasePlayer);
        Entity created = primary == null ? null : EntityType.AREA_EFFECT_CLOUD.create(primary.level());
        if (!(created instanceof AreaEffectCloud effect)) {
            source.sendFailure(Component.literal("Loaded primary unavailable"));
            return 0;
        }
        effect.setOwner(primary);
        effect.setDuration(200);
        effect.moveTo(primary.getX(), primary.getY(), primary.getZ(), 0.0F, 0.0F);
        if (!BossEncounterRuntime.attachEffect(primary, effect)) {
            effect.discard();
            source.sendFailure(Component.literal("Could not attach boss effect"));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Attached EFFECT to " + id), true);
        return 1;
    }

    private static Mob loadedPrimary(CommandSourceStack source, ResourceLocation id, ServerPlayer phasePlayer) {
        EncounterSavedData data = EncounterSavedData.get(source.getServer());
        EncounterDefinition definition = data.definition(id).orElse(null);
        if (definition == null || definition.type() != EncounterType.BOSS) return null;
        PhaseEncounterState state = data.existingState(PhaseManager.state(phasePlayer).phaseId(), id).orElse(null);
        var attempt = state == null ? null : state.bossAttempt();
        var level = source.getServer().getLevel(definition.dimension());
        Entity primary = attempt == null || level == null ? null : level.getEntity(attempt.primaryBossUuid());
        return primary instanceof Mob mob ? mob : null;
    }

    private static int debug(ServerPlayer player) {
        boolean enabled = EncounterDebugService.toggle(player);
        player.sendSystemMessage(Component.literal("Encounter debug " + (enabled ? "enabled" : "disabled")));
        return enabled ? 1 : 0;
    }

    private EncounterCommands() {}
}
