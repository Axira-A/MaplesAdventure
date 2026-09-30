package dev.maplesadventure.client.editor;
import java.util.function.BooleanSupplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
/** Original code-drawn workbench control, no third-party icon/texture assets. */
final class EditorButton extends Button {
    private final BooleanSupplier selected;
    EditorButton(int x,int y,int w,int h,Component label,Runnable action,BooleanSupplier selected){super(x,y,w,h,label,b->action.run(),DEFAULT_NARRATION);this.selected=selected;}
    @Override protected void renderWidget(GuiGraphics g,int mouseX,int mouseY,float delta){
        boolean chosen=selected.getAsBoolean();int color=chosen?0xEF244A68:isHoveredOrFocused()?0xEE303F50:0xDC151F2B;
        g.fill(getX(),getY(),getX()+width,getY()+height,color);g.renderOutline(getX(),getY(),width,height,chosen?0xFF519AD1:0xFF374656);
        var font=Minecraft.getInstance().font;String text=font.plainSubstrByWidth(getMessage().getString(),Math.max(0,width-8));
        g.drawString(font,text,getX()+4,getY()+(height-8)/2,active?(chosen?0xFF9DD3FF:0xFFCDD5DF):0xFF697685,false);
    }
}
