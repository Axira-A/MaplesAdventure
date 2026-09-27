package dev.maplesadventure.flask;

import dev.maplesadventure.api.flask.FlaskKind;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

/** A reusable input tool, never food, durability, charge storage or a consumed stack. */
public final class FlaskItem extends Item {
    /** Client installs a names-only presentation provider; server behavior never reads it. */
    public static java.util.function.Function<FlaskKind,net.minecraft.network.chat.Component> displayName = kind ->
            net.minecraft.network.chat.Component.translatable("item.maplesadventure."+(kind==FlaskKind.CRIMSON?"crimson_flask":"ashen_flask"));
    private final FlaskKind kind;
    public FlaskItem(FlaskKind kind) { super(new Properties().stacksTo(1)); this.kind = kind; }
    public FlaskKind kind() { return kind; }
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack) { return displayName.apply(kind); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer server) {
            if (!FlaskUseController.start(server, kind, hand)) {
                FlaskService.sync(server); // Explicit authoritative rejection for pending quick-use clients.
                server.displayClientMessage(net.minecraft.network.chat.Component.translatable("message.maplesadventure.flask.unavailable"), true);
                return InteractionResultHolder.fail(stack);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return FlaskRules.USE_TICKS + 10; }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) { return stack; }
}
