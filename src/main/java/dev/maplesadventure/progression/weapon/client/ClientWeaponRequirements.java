package dev.maplesadventure.progression.weapon.client;
import dev.maplesadventure.progression.weapon.*;
import dev.maplesadventure.progression.client.ClientAttributeState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import java.util.*;
public final class ClientWeaponRequirements {
    private static Map<ResourceLocation,WeaponRequirementNetwork.Entry> profiles=Map.of();
    private static final Map<ResourceLocation,WeaponRequirementNetwork.Entry> pending=new HashMap<>();
    private static List<WeaponInfusionDefinition> pendingInfusions=List.of();
    private static UUID revision;
    private static int next,total;
    private static double penalty=1;
    public static void register() { NeoForge.EVENT_BUS.register(new ClientWeaponRequirements()); }
    public static void accept(WeaponRequirementNetwork.Batch b) {
        if(b.index()==0) { pending.clear(); pendingInfusions=b.infusions(); revision=b.revision(); next=0; total=b.batches(); }
        if(!b.revision().equals(revision)||b.index()!=next||b.batches()!=total) { pending.clear(); pendingInfusions=List.of(); revision=null; return; }
        for(var entry:b.entries()) {
            if(pending.put(entry.item(),entry)!=null || pending.size()>WeaponRequirementService.MAX_ITEMS) { pending.clear(); revision=null; return; }
        }
        if(++next==total) { profiles=Map.copyOf(pending); penalty=b.penalty(); ClientWeaponInfusions.replace(pendingInfusions); pending.clear(); pendingInfusions=List.of(); }
    }
    @SubscribeEvent public void logout(ClientPlayerNetworkEvent.LoggingOut event) { profiles=Map.of(); pending.clear(); pendingInfusions=List.of(); ClientWeaponInfusions.clear(); revision=null; }
    @SubscribeEvent public void tooltip(ItemTooltipEvent event) {
        if(event.getEntity()==null || !event.getEntity().level().isClientSide()) return;
        var entry=profiles.get(BuiltInRegistries.ITEM.getKey(event.getItemStack().getItem()));
        if(entry==null) return;
        var resolved=ClientWeaponInfusions.resolve(event.getItemStack(),entry); var p=resolved.requirements();
        var state=ClientAttributeState.snapshot().state();
        if(entry.weapon()) {
            var requirement=WeaponRequirementService.evaluate(state,p,penalty);
            event.getToolTip().add(infusionLine(resolved.infusion()));
            var rating=WeaponAttackRatingCalculator.calculate(WeaponClassifier.baseAttack(event.getItemStack()),resolved.scaling(),resolved.damage(),state,requirement.damageMultiplier());
            event.getToolTip().addAll(WeaponAttackText.lines(WeaponAttackSnapshot.of(entry.item(),rating,resolved.scaling(),resolved.damage(),requirement)));
            if(net.minecraft.client.gui.screens.Screen.hasShiftDown())
                for(var c:resolved.damage().components()) {
                    event.getToolTip().add(c.channel().displayName());
                    event.getToolTip().addAll(WeaponAttackText.contributions(state,c.scaling(resolved.scaling())));
                }
        }
        event.getToolTip().addAll(dev.maplesadventure.progression.status.client.StatusText.lines(resolved.statuses().evaluate(state.get(dev.maplesadventure.progression.Attribute.ARCANE))));
        if(!p.enabled()) return;
        event.getToolTip().add(Component.translatable("screen.maplesadventure.weapon.requirements").withStyle(ChatFormatting.GOLD));
        for(var a:WeaponRequirementProfile.ATTRIBUTES) if(p.get(a)>0)
            event.getToolTip().add(Component.translatable("screen.maplesadventure.weapon.requirement",Component.translatable(a.translationKey()),p.get(a))
                    .withStyle(state.get(a)<p.get(a)?ChatFormatting.RED:ChatFormatting.GRAY));
        if(!WeaponRequirementService.evaluate(state,p,penalty).satisfied()) {
            event.getToolTip().add(Component.translatable("screen.maplesadventure.weapon.unqualified").withStyle(ChatFormatting.RED));
            event.getToolTip().add(Component.translatable("screen.maplesadventure.weapon.penalty",Math.round(penalty*100)).withStyle(ChatFormatting.GRAY));
        }
    }
    private static Component infusionLine(WeaponInfusionView infusion) {
        return Component.translatable("screen.maplesadventure.weapon.infusion",infusion.displayName()).withStyle(
                infusion.status()==WeaponInfusionView.Status.UNKNOWN||infusion.status()==WeaponInfusionView.Status.INELIGIBLE?ChatFormatting.RED:ChatFormatting.GOLD);
    }
}
