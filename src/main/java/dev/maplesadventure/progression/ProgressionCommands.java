package dev.maplesadventure.progression;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

public final class ProgressionCommands {
    public static void register() { NeoForge.EVENT_BUS.register(new ProgressionCommands()); }

    @SubscribeEvent
    public void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("maplesadventure").then(root()));
        event.getDispatcher().register(Commands.literal("ma").then(root()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("attribute")
                .requires(source -> source.hasPermission(2));
        root.then(Commands.literal("info")
                .executes(context -> info(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> info(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("spell")
                .executes(context -> spell(context.getSource(), context.getSource().getPlayerOrException()))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> spell(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(attributeArgument().then(Commands.argument("value", IntegerArgumentType.integer(5, 99))
                                .executes(context -> set(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        parse(context.getSource(), StringArgumentType.getString(context, "attribute")),
                                        IntegerArgumentType.getInteger(context, "value")))))));
        root.then(Commands.literal("add")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(attributeArgument().then(Commands.argument("amount", IntegerArgumentType.integer(-1000, 1000))
                                .executes(context -> add(context.getSource(), EntityArgument.getPlayer(context, "player"),
                                        parse(context.getSource(), StringArgumentType.getString(context, "attribute")),
                                        IntegerArgumentType.getInteger(context, "amount")))))));
        root.then(Commands.literal("reset").then(Commands.argument("player", EntityArgument.player())
                .executes(context -> reset(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("refresh").then(Commands.argument("player", EntityArgument.player())
                .executes(context -> refresh(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        root.then(Commands.literal("upgrade")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(attributeArgument().executes(context -> upgrade(context.getSource(),
                                EntityArgument.getPlayer(context, "player"),
                                parse(context.getSource(), StringArgumentType.getString(context, "attribute")))))));
        root.then(Commands.literal("screen")
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(context -> openScreen(context.getSource(), EntityArgument.getPlayer(context, "player")))));
        return root;
    }

    private static com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> attributeArgument() {
        return Commands.argument("attribute", StringArgumentType.word()).suggests((context, builder) ->
                SharedSuggestionProvider.suggest(Arrays.stream(Attribute.values())
                        .map(Attribute::serializedName), builder));
    }

    private static Attribute parse(CommandSourceStack source, String text) {
        Attribute parsed = Attribute.parse(text).orElse(null);
        if (parsed == null) source.sendFailure(Component.literal("Unknown attribute: " + text));
        return parsed;
    }

    private static int info(CommandSourceStack source, ServerPlayer player) {
        PlayerAttributeState state = PlayerAttributeService.state(player);
        var stats = dev.maplesadventure.progression.stats.CharacterStatsService.snapshot(player);
        StringBuilder text = new StringBuilder(player.getGameProfile().getName())
                .append(" Level=").append(AttributeProgression.level(state))
                .append(" XP=").append(dev.maplesadventure.soul.ExperiencePoints.capture(player))
                .append(" NextCost=").append(PlayerAttributeService.nextLevelCost(player));
        for (Attribute attribute : Attribute.values())
            text.append(' ').append(attribute.abbreviation().toUpperCase(java.util.Locale.ROOT))
                    .append('=').append(state.get(attribute));
        text.append(" Derived[HP=").append(format(stats.value(
                        dev.maplesadventure.progression.stats.CharacterStat.MAX_HEALTH).value()))
                .append(" Mana=").append(format(stats.value(
                        dev.maplesadventure.progression.stats.CharacterStat.MAX_MANA).value()))
                .append(" Stamina=").append(format(stats.value(
                        dev.maplesadventure.progression.stats.CharacterStat.MAX_STAMINA).value())).append(']');
        source.sendSuccess(() -> Component.literal(text.toString()), false);
        var load = dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.snapshot(player);
        var profile = load.policy().profile(load.tier());
        source.sendSuccess(() -> Component.literal("EquipmentLoad=" + format(load.currentLoad())
                + " tier=" + load.tier() + " dodge=" + load.currentDodgeMode()
                + " movement=" + profile.movementMultiplier() + " regen=" + profile.staminaRegenMultiplier()
                + " cost=" + profile.staminaCostMultiplier() + " distance=" + profile.dodgeDistanceMultiplier()), false);
        var runtime = dev.maplesadventure.progression.runtime.DerivedStatRuntimeService.snapshot(player);
        for (var resource : dev.maplesadventure.progression.runtime.DerivedRuntimeResource.values()) {
            var value = runtime.value(resource);
            source.sendSuccess(() -> Component.literal(resource + ": formula=" + format(value.formulaValue())
                    + " runtime=" + format(value.runtimeValue())
                    + " progressionModifier=" + format(value.formulaValue() - resource.progressionBaseline())
                    + " status=" + value.implementation()), false);
        }
        return 1;
    }

    private static int spell(CommandSourceStack source, ServerPlayer player) {
        var state = PlayerAttributeService.state(player);
        source.sendSuccess(() -> Component.literal("Iron's school adapter="
                + dev.maplesadventure.progression.spell.SpellScalingRuntimeService.available()), false);
        for (Attribute attribute : java.util.List.of(Attribute.INTELLIGENCE, Attribute.FAITH, Attribute.ARCANE))
            source.sendSuccess(() -> Component.literal(attribute + "=" + state.get(attribute) + " rating="
                    + format(100 * OffensiveScalingCurve.evaluate(state.get(attribute)))), false);
        for (var s : dev.maplesadventure.progression.spell.SpellScalingRuntimeService.snapshot(player).schools().values())
            source.sendSuccess(() -> Component.literal(s.schoolId() + " weights[INT=" + s.profile().intelligenceWeight()
                    + ",FTH=" + s.profile().faithWeight() + ",ARC=" + s.profile().arcaneWeight()
                    + "] progression=" + format(100 * s.progressionBonus()) + "%"
                    + (s.context().available() ? " school=" + format(s.runtimeSchoolPower())
                        + " global=" + format(s.context().globalPower()) + " effective=" + format(s.runtimeSpellPower())
                        + " observed=" + format(s.context().observedSchoolPower()) : " runtime=unavailable")
                    + " modifier=" + s.modifierPresent()
                    + " status=" + s.implementationState()), false);
        return 1;
    }

    private static int set(CommandSourceStack source, ServerPlayer player, Attribute attribute, int value) {
        if (attribute == null) return 0;
        PlayerAttributeState state = PlayerAttributeService.set(player, attribute, value, UpgradeContext.ADMIN);
        source.sendSuccess(() -> Component.literal("Set " + player.getGameProfile().getName() + ' '
                + attribute.abbreviation().toUpperCase(java.util.Locale.ROOT) + '=' + state.get(attribute)), true);
        return 1;
    }

    private static int add(CommandSourceStack source, ServerPlayer player, Attribute attribute, int amount) {
        if (attribute == null) return 0;
        PlayerAttributeState state = PlayerAttributeService.add(player, attribute, amount, UpgradeContext.ADMIN);
        source.sendSuccess(() -> Component.literal("Changed " + player.getGameProfile().getName() + ' '
                + attribute.abbreviation().toUpperCase(java.util.Locale.ROOT) + '=' + state.get(attribute)), true);
        return 1;
    }

    private static int reset(CommandSourceStack source, ServerPlayer player) {
        PlayerAttributeService.reset(player, UpgradeContext.ADMIN);
        source.sendSuccess(() -> Component.literal("Reset attributes for " + player.getGameProfile().getName()
                + " without refunding XP"), true);
        return 1;
    }

    private static int refresh(CommandSourceStack source, ServerPlayer player) {
        var runtime = dev.maplesadventure.progression.runtime.DerivedStatRuntimeService.refresh(player,
                dev.maplesadventure.progression.runtime.DerivedStatRefreshReason.ADMIN);
        dev.maplesadventure.progression.encumbrance.EncumbranceRuntimeService.refresh(player);
        AttributeSyncService.sync(player);
        source.sendSuccess(() -> Component.literal("Refreshed derived runtime attributes for "
                + player.getGameProfile().getName() + " [health=" + runtime.health().implementation()
                + ", mana=" + runtime.mana().implementation()
                + ", stamina=" + runtime.stamina().implementation() + ']'), true);
        return 1;
    }

    private static int upgrade(CommandSourceStack source, ServerPlayer player, Attribute attribute) {
        if (attribute == null) return 0;
        UpgradeResult result = PlayerAttributeService.upgradeOne(player, attribute, UpgradeContext.DEBUG);
        if (!result.success()) {
            source.sendFailure(Component.literal("Upgrade failed: " + result.status()
                    + " cost=" + result.cost() + " xp=" + result.experienceBefore()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal("Upgraded " + attribute.abbreviation().toUpperCase(java.util.Locale.ROOT)
                + " to " + result.state().get(attribute) + " cost=" + result.cost()
                + " remainingXP=" + result.experienceAfter()), true);
        return 1;
    }

    private static int openScreen(CommandSourceStack source, ServerPlayer player) {
        dev.maplesadventure.progression.upgrade.UpgradeAccessService.authorizeAdmin(player);
        source.sendSuccess(() -> Component.literal("Opened generic level-up access for "
                + player.getGameProfile().getName()), false);
        return 1;
    }

    private static String format(double value) { return String.format(java.util.Locale.ROOT, "%.2f", value); }
    private ProgressionCommands() {}
}
