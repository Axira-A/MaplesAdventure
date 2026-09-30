package dev.maplesadventure.regression;

import dev.maplesadventure.MaplesAdventure;
import dev.maplesadventure.client.editor.EditorClient;
import dev.maplesadventure.client.editor.EditorScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import org.lwjgl.glfw.GLFW;

/** F9, only in the opt-in regression client and an already authorized Editor Screen. */
@EventBusSubscriber(modid="weaponregression",value=Dist.CLIENT)
public final class EditorInputRegression {
    private static EditorScreen screen;
    private static Vec3 origin,stopped;
    private static int tick,key;
    @SubscribeEvent public static void key(InputEvent.Key event){
        var mc=Minecraft.getInstance();
        if(event.getKey()!=GLFW.GLFW_KEY_F9||event.getAction()!=GLFW.GLFW_PRESS||screen!=null
                ||!EditorClient.active()||!(mc.screen instanceof EditorScreen editor)||mc.player==null)return;
        screen=editor;origin=mc.player.position();tick=0;key=mc.options.keyUp.getKey().getValue();
        screen.setFocused(null);screen.keyPressed(key,0,0);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event){
        if(screen==null)return;var mc=Minecraft.getInstance();
        if(mc.screen!=screen||mc.player==null||!EditorClient.active()){cancel();return;}
        try{
            tick++;
            if(tick==6){check(mc.player.position().distanceTo(origin)>1,"Movement without RMB uses actual player travel");
                screen.keyReleased(key,0,0);stopped=mc.player.position();}
            if(tick==12){check(mc.player.position().distanceTo(stopped)<.00001,"Release stops immediately with no spectator inertia");
                var input=new EditBox(mc.font,0,0,100,20,Component.literal("fixture"));screen.setFocused(input);screen.keyPressed(key,0,0);}
            if(tick==18){check(mc.player.position().distanceTo(stopped)<.00001,"Focused text field blocks navigation");
                screen.setFocused(null);cancel();mc.player.displayClientMessage(Component.literal("Editor input regression PASS"),true);}
        }catch(RuntimeException failure){MaplesAdventure.LOGGER.error("Editor input regression FAILED",failure);cancel();}
    }
    private static void check(boolean value,String label){if(!value)throw new IllegalStateException(label);MaplesAdventure.LOGGER.info("[Editor input regression] PASS {}",label);}
    private static void cancel(){if(screen!=null)screen.keyReleased(key,0,0);screen=null;}
    private EditorInputRegression(){}
}
