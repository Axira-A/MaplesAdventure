package dev.maplesadventure.client.flask;

import dev.maplesadventure.api.flask.*;
import dev.maplesadventure.flask.*;
import dev.maplesadventure.progression.ProgressionAttachments;
import dev.maplesadventure.client.bonfire.BonfireClient;
import dev.maplesadventure.integration.soulscombathud.SoulsFlaskClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

public final class FlaskClient {
    private static FlaskPayloads.Snapshot snapshot;
    public static FlaskSnapshot state() { return snapshot == null ? FlaskState.initial(false) : snapshot.state(); }
    public static boolean mana() { return snapshot != null && snapshot.mana(); }
    public static boolean hasSnapshot() { return snapshot != null; }
    public static void register(IEventBus bus) {
        NeoForge.EVENT_BUS.register(new FlaskClient());
        bus.addListener(FlaskVisuals::registerModels);
        bus.addListener(FlaskVisuals::registerShaders);
        bus.addListener((net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e) -> {
            var renderer = new FlaskIconRenderer();
            e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions() {
                @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() { return renderer; }
            }, FlaskItems.CRIMSON.get(), FlaskItems.ASHEN.get());
        });
        bus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
            SoulsFlaskClient.initialize();
            for(var item:java.util.List.of(FlaskItems.CRIMSON.get(),FlaskItems.ASHEN.get())) {
                ItemProperties.register(item,ResourceLocation.parse("maplesadventure:flask_fill"),(stack,level,entity,seed)->
                        entity != null && entity != Minecraft.getInstance().player ? 1
                                : FlaskRules.icon(state().remaining(item.kind()),state().allocated(item.kind())));
            }
            FlaskItem.displayName = kind -> Component.translatable("item.maplesadventure."+(kind==FlaskKind.CRIMSON?"crimson_flask":"ashen_flask"))
                    .append(state().potencyLevel()==0?"":" +"+state().potencyLevel());
        }));
        bus.addListener((RegisterGuiLayersEvent e)->e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                ResourceLocation.parse("maplesadventure:flask_hud"),FlaskClient::renderHud));
    }
    public static void receive(FlaskPayloads.Snapshot payload) {
        snapshot=payload;
        var p=Minecraft.getInstance().player;
        if(p==null)return;
        boolean was=FlaskApi.isUsingFlask(p);
        p.setData(ProgressionAttachments.FLASK,new FlaskState(payload.state()));
        p.setData(ProgressionAttachments.FLASK_USING,payload.usingKind()!=0);
        if(payload.usingKind()!=0 && !was) {
            var kind=FlaskKind.values()[payload.usingKind()-1];
            for(var hand:InteractionHand.values()) if(p.getItemInHand(hand).getItem() instanceof FlaskItem item && item.kind()==kind) {p.startUsingItem(hand);break;}
        } else if(payload.usingKind()==0 && was) p.stopUsingItem();
        SoulsFlaskClient.snapshot(payload.usingKind());
    }
    public static void menu(FlaskPayloads.Menu payload) {
        var mc=Minecraft.getInstance();
        if(!BonfireClient.resting() || mc.player==null) return;
        if (payload.replyTo() != null && (!(mc.screen instanceof FlaskScreen screen) || !screen.accepts(payload))) {
            FlaskScreen.closeSession(payload.nonce());
            return;
        }
        receive(new FlaskPayloads.Snapshot(payload.state(),payload.mana(),0));
        if(payload.replyTo() != null && mc.screen instanceof FlaskScreen screen) screen.accept(payload);
        else mc.setScreen(new FlaskScreen(payload));
    }
    @SubscribeEvent public void logout(ClientPlayerNetworkEvent.LoggingOut e) { snapshot=null; SoulsFlaskClient.reset(); }
    @SubscribeEvent public void input(InputEvent.InteractionKeyMappingTriggered e) {
        var p=Minecraft.getInstance().player;
        if(p!=null && FlaskApi.isUsingFlask(p)) {e.setCanceled(true);e.setSwingHand(false);}
    }
    @SubscribeEvent public void movement(MovementInputUpdateEvent e) {
        if(FlaskApi.isUsingFlask(e.getEntity())) e.getEntity().setSprinting(false);
    }
    @SubscribeEvent public void tooltip(ItemTooltipEvent e) {
        if(!(e.getItemStack().getItem() instanceof FlaskItem item))return;
        if(item.kind()==FlaskKind.ASHEN && !mana()) { e.getToolTip().add(Component.translatable("message.maplesadventure.flask.mana_unavailable"));return; }
        e.getToolTip().add(Component.translatable("tooltip.maplesadventure.flask.remaining",state().remaining(item.kind()),state().allocated(item.kind())));
        e.getToolTip().add(Component.translatable(item.kind()==FlaskKind.CRIMSON?"tooltip.maplesadventure.flask.health":"tooltip.maplesadventure.flask.mana",
                item.kind()==FlaskKind.CRIMSON?FlaskRules.health(state().potencyLevel()):FlaskRules.mana(state().potencyLevel())));
    }
    private static void renderHud(GuiGraphics g,DeltaTracker delta) {
        var mc=Minecraft.getInstance(); if(mc.player==null || mc.options.hideGui || snapshot==null)return;
        if(SoulsFlaskClient.available())return;
        boolean red=false,blue=false;
        for(int i=0;i<9;i++) { ItemStack stack=mc.player.getInventory().getItem(i); red|=stack.is(FlaskItems.CRIMSON.get());blue|=stack.is(FlaskItems.ASHEN.get()); }
        red|=mc.player.getOffhandItem().is(FlaskItems.CRIMSON.get());blue|=mc.player.getOffhandItem().is(FlaskItems.ASHEN.get());
        int x=8,y=g.guiHeight()-48;
        if(red) { g.renderItem(new ItemStack(FlaskItems.CRIMSON.get()),x,y);g.drawString(mc.font,state().crimsonRemaining()+" / "+state().crimsonAllocated(),x+20,y+4,0xFFFFD9A0);y-=20; }
        if(blue&&mana()) {g.renderItem(new ItemStack(FlaskItems.ASHEN.get()),x,y);g.drawString(mc.font,state().ashenRemaining()+" / "+state().ashenAllocated(),x+20,y+4,0xFFAFDFFF);}
    }
    private FlaskClient() {}
}
