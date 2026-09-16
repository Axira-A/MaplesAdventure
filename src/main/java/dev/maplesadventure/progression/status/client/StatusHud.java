package dev.maplesadventure.progression.status.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.maplesadventure.progression.status.StatusEffectType;
import net.minecraft.network.chat.Component;

public final class StatusHud {
    public static void render(GuiGraphics g,DeltaTracker delta) {
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||!mc.player.isAlive()) return;
        var layout=StatusHudLayout.forScreen(g.guiWidth(),g.guiHeight()); int index=0;
        for(var type:StatusEffectType.values()) {
            var display=ClientStatusState.displays().get(type); if(display==null) continue;
            float alpha=display.alpha(); if(alpha<=0) continue;
            int y=layout.bottom()-index++*layout.stride();
            var texture=StatusHudTextureCache.texture(type); if(texture==null) continue;
            g.pose().pushPose(); g.pose().translate(layout.x(),y,0); g.pose().scale(layout.scale(),layout.scale(),1);
            RenderSystem.enableBlend(); RenderSystem.setShaderColor(1,1,1,alpha);
            mc.getTextureManager().getTexture(StatusHudTextureCache.EMPTY).setFilter(false,false);
            g.blit(StatusHudTextureCache.EMPTY,0,0,0,0,128,32,128,32);
            int width=StatusHudMath.fillPixels(display.fill(),StatusHudTextureCache.FILL_WIDTH);
            if(width>0) g.blit(texture,StatusHudTextureCache.FILL_X,StatusHudTextureCache.FILL_Y,
                    StatusHudTextureCache.FILL_X,StatusHudTextureCache.FILL_Y,width,StatusHudTextureCache.FILL_HEIGHT,128,32);
            var icon=StatusHudTextureCache.visual(type).icon(); mc.getTextureManager().getTexture(icon).setFilter(false,false);
            // Actual supplied inner square is 9x8; a 16px icon is reduced by an exact factor of two.
            g.blit(icon,4,12,8,8,0,0,16,16,16,16);
            g.flush(); RenderSystem.setShaderColor(1,1,1,1); g.pose().popPose();
        }
        var proc=ClientStatusState.currentProc();
        if(proc!=null) {
            var text=Component.translatable("status.maplesadventure.proc."+proc.status().id());
            float alpha=ClientStatusState.procAlpha(); int width=mc.font.width(text)+24;
            int x=(g.guiWidth()-width)/2,y=Math.max(16,layout.bottom()-Math.max(1,index)*layout.stride()-24);
            g.fill(x,y,x+width,y+24,((int)(alpha*140)<<24)|0x4D0909);
            g.renderOutline(x,y,width,24,((int)(alpha*200)<<24)|0xA3302A);
            if(alpha>.02) g.drawCenteredString(mc.font,text,g.guiWidth()/2,y+8,((int)(alpha*255)<<24)|0xEEE8E0);
        }
    }
    private StatusHud() {}
}
