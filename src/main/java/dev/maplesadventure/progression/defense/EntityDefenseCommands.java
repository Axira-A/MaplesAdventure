package dev.maplesadventure.progression.defense;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import dev.maplesadventure.multiplayer.phase.PhaseRelations;
import dev.maplesadventure.progression.weapon.WeaponDamageChannel;
import java.util.Locale;

public final class EntityDefenseCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new EntityDefenseCommands()); }
    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        for (String root : new String[]{"ma", "maplesadventure"}) event.getDispatcher().register(Commands.literal(root)
            .then(Commands.literal("defense").requires(s -> s.hasPermission(2))
                .then(Commands.literal("info").executes(c -> info(c.getSource())))
                .then(Commands.literal("setprofile").then(Commands.argument("id", ResourceLocationArgument.id())
                    .suggests((c,b) -> SharedSuggestionProvider.suggestResource(EntityDefenseRegistry.profiles().keySet(), b))
                    .executes(c -> {
                        try { EntityDefenseService.assign(target(c.getSource()), ResourceLocationArgument.getId(c, "id")); }
                        catch (IllegalArgumentException error) { throw failure(error.getMessage()); }
                        return info(c.getSource());
                    })))
                .then(Commands.literal("clearprofile").executes(c -> {
                    try { EntityDefenseService.clearOverride(target(c.getSource())); }
                    catch (IllegalArgumentException error) { throw failure(error.getMessage()); }
                    return info(c.getSource());
                }))
                .then(Commands.literal("simulate").then(Commands.argument("channel", StringArgumentType.word())
                    .suggests((c,b) -> SharedSuggestionProvider.suggest(java.util.Arrays.stream(WeaponDamageChannel.values()).map(WeaponDamageChannel::id), b))
                    .then(Commands.argument("attackRating", DoubleArgumentType.doubleArg(0, 60000))
                        .then(Commands.argument("incomingDamage", DoubleArgumentType.doubleArg(0, 1000000))
                            .executes(c -> simulate(c.getSource(), StringArgumentType.getString(c, "channel"),
                                    DoubleArgumentType.getDouble(c, "attackRating"), DoubleArgumentType.getDouble(c, "incomingDamage")))))))
                .then(Commands.literal("lasthit").executes(c -> lastHit(c.getSource())))));
    }
    private static LivingEntity target(CommandSourceStack source) throws CommandSyntaxException {
        var player = source.getPlayerOrException(); var from = player.getEyePosition();
        var intended = from.add(player.getLookAngle().scale(32));
        var end = player.level().clip(new ClipContext(from, intended, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        var hit = ProjectileUtil.getEntityHitResult(player, from, end,
                player.getBoundingBox().expandTowards(end.subtract(from)).inflate(1),
                e -> e instanceof LivingEntity && !e.isSpectator() && e.isPickable() && PhaseRelations.canSee(player, e), from.distanceToSqr(end));
        if (hit == null || !(hit.getEntity() instanceof LivingEntity living)) throw failure("Look at a visible living entity within 32 blocks (clear line of sight).");
        return living;
    }
    private static int info(CommandSourceStack source) throws CommandSyntaxException {
        var entity = target(source); var resolved = TargetDefenseResolver.resolve(entity);
        say(source, entity.getName().getString() + " " + entity.getUUID() + " type=" + BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())
                + " source=" + resolved.source() + " pressure=" + resolved.pressure());
        for (var channel : WeaponDamageChannel.values()) {
            var defense = resolved.view().channel(channel);
            if (!defense.equals(ChannelDefense.NONE)) say(source, format("%s Defense=%.3f Absorption=%.1f%%", channel.id(), defense.defense(), defense.absorption()*100));
        }
        return 1;
    }
    private static int simulate(CommandSourceStack source, String id, double ar, double damage) throws CommandSyntaxException {
        try {
            var channel = WeaponDamageChannel.parse(id); var entity=target(source); var resolved=TargetDefenseResolver.resolve(entity);
            var defense = resolved.view().channel(channel);
            double pressure = resolved.pressure();
            say(source,"Target="+BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType())+" Source="+resolved.source()+" Pressure="+pressure+" AttackPower="+ar);
            double multiplier = DefenseMitigationCurve.finalChannelMultiplier(ar, defense, pressure);
            say(source, format("%s Defense=%.3f Absorption=%.1f%% Penetration=%.6f Multiplier=%.6f Final=%.6f (simulation only)",
                    channel.id(), defense.defense(), defense.absorption()*100, DefenseMitigationCurve.penetration(ar,defense.defense(),pressure), multiplier, damage*multiplier));
            return 1;
        } catch (IllegalArgumentException error) { throw failure(error.getMessage()); }
    }
    private static int lastHit(CommandSourceStack source) throws CommandSyntaxException {
        var hit = LastWeaponDamageResolution.get(source.getPlayerOrException().getUUID()).orElseThrow(() -> failure("No typed hit recorded in the last 60 seconds."));
        var resolution = hit.resolution();
        say(source,"TypedSource="+hit.typedSource()+" Pressure="+hit.pressure()+" (RAW is post-Vanilla-armor Pre input, not unmitigated damage)");
        say(source, format("target=%s profile=%s RAW=%.6f Nominal=%.6f", hit.target(), hit.profile(), resolution.originalDamage(), resolution.nominalDamage()));
        for (var channel : resolution.channelResults()) say(source, format("%s AttackPower=%.3f Share=%.3f%% Incoming=%.6f Defense=%.3f Absorb=%.1f%% Multiplier=%.6f Final=%.6f",
                channel.channel().id(),channel.attackRating(),channel.share()*100,channel.incomingDamage(),channel.defense(),channel.absorption()*100,channel.defenseMultiplier(),channel.finalDamage()));
        say(source,format("Qualified=%.6f Requirement=%.3f%% Frost=%.3f FinalMaples=%.6f",resolution.qualifiedDamage(),resolution.requirementMultiplier()*100,hit.frostMultiplier(),hit.finalMaplesDamage())); return 1;
    }
    private static String format(String pattern, Object... args) { return String.format(Locale.ROOT, pattern, args); }
    private static void say(CommandSourceStack source, String text) { source.sendSuccess(() -> Component.literal(text), false); }
    private static CommandSyntaxException failure(String text) { return new SimpleCommandExceptionType(Component.literal(text)).create(); }
}
