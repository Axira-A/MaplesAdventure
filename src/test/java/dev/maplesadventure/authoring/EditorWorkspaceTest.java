package dev.maplesadventure.authoring;
import dev.maplesadventure.client.editor.EditorDockLayout;
import dev.maplesadventure.client.editor.EditorNavigation;
import dev.maplesadventure.editor.EditorRecoveryState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EditorWorkspaceTest {
    @Test void authoringMotionIsImmediateAndDoesNotAccumulateMomentum(){
        var initial=EditorNavigation.motion(0,0,1,0);assertEquals(EditorNavigation.SPEED,initial.z,1e-10);
        for(int i=0;i<100;i++)assertEquals(initial,EditorNavigation.motion(0,0,1,0));
        assertEquals(Vec3.ZERO,EditorNavigation.motion(0,0,0,0));
        assertEquals(-EditorNavigation.SPEED,EditorNavigation.motion(90,0,1,0).x,1e-10);
        assertEquals(EditorNavigation.SPEED,EditorNavigation.motion(0,1,1,1).length(),1e-10);
        assertEquals(-EditorNavigation.SPEED,EditorNavigation.motion(0,0,0,-1).y,1e-10);
    }
    @Test void movementDoesNotRequireRightMouseButTextAndFocusAlwaysBlockIt(){
        assertTrue(EditorNavigation.acceptsMovement(true,true,false,false,false));
        assertFalse(EditorNavigation.acceptsMovement(false,true,false,false,false));
        assertFalse(EditorNavigation.acceptsMovement(true,false,false,false,false));
        assertFalse(EditorNavigation.acceptsMovement(true,true,true,false,false));
        assertFalse(EditorNavigation.acceptsMovement(true,true,false,true,false));
        assertFalse(EditorNavigation.acceptsMovement(true,true,false,false,true));
    }
    @Test void defaultProportionsAndPreferenceRoundtrip(){var a=EditorDockLayout.resolve(1200,.19,.25);assertEquals(228,a.left());assertEquals(900,a.right());assertEquals(a,EditorDockLayout.resolve(1200,a.leftRatio(),a.rightRatio()));}
    @Test void draggingClampsAndPreservesViewport(){var a=EditorDockLayout.resolve(1200,.19,.25);var left=a.dragLeft(10000);assertTrue(left.left()<=480);assertTrue(left.right()-left.left()>=left.minimumViewport());
        var right=left.dragRight(-1000);assertTrue(right.right()-right.left()>=right.minimumViewport());assertTrue(a.dragLeft(-1).left()>=180);assertTrue(a.dragRight(9999).width()-a.dragRight(9999).right()>=220);}
    @Test void guiScalesAndAspectRatiosRetainVisiblePanels(){for(int width:new int[]{320,480,640,854,960,1280,1720,2560})for(double left:new double[]{.1,.19,.4})for(double right:new double[]{.1,.25,.45}){
        var a=EditorDockLayout.resolve(width,left,right);assertTrue(a.left()>0);assertTrue(a.right()<width);assertTrue(a.right()-a.left()>=a.minimumViewport());
    }}
    @Test void invalidPreferencesUseDefaults(){assertEquals(EditorDockLayout.resolve(1000,.19,.25),EditorDockLayout.resolve(1000,Double.NaN,Double.POSITIVE_INFINITY));}
    @Test void recoverySavesOnlyOriginAndModeAndClears(){var state=new EditorRecoveryState();var point=new EditorRecoveryState.ReturnPoint(GameType.SURVIVAL,ResourceLocation.parse("minecraft:overworld"),new EditorTransform(new Vec3(1,70,-4),120,30));state.begin(point);
        var tag=state.serializeNBT(null);assertFalse(tag.contains("Inventory"));assertFalse(tag.contains("XpTotal"));var restored=new EditorRecoveryState();restored.deserializeNBT(null,tag);assertEquals(point,restored.point().orElseThrow());
        assertThrows(IllegalStateException.class,()->restored.begin(point));restored.clear();assertFalse(restored.active());}
    @Test void malformedRecoveryRetainsActiveMarkerForSafeFallback(){var state=new EditorRecoveryState();var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("Active",true);tag.putInt("Version",99);state.deserializeNBT(null,tag);assertTrue(state.active());assertTrue(state.malformed());assertTrue(state.point().isEmpty());}
}
