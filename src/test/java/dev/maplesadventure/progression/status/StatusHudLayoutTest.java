package dev.maplesadventure.progression.status;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.maplesadventure.progression.status.client.StatusHudLayout;
class StatusHudLayoutTest {
    @Test void sevenNativeBarsFitGuiScalesOneThroughFour() {
        for(int scale:new int[]{1,2,3,4}) {
            int width=1920/scale,height=1080/scale;
            var layout=new StatusHudLayout((width-128)/2,height-110,32,1);
            for(int index=0;index<7;index++) {
                var position=layout.bar(index,7,width,height);
                assertTrue(position.x()>=0&&position.x()+128<=width);
                assertTrue(position.y()>=0&&position.y()+32<=height);
            }
        }
    }
    @Test void procAnchorDoesNotDependOnBarCountOrGuiScale() {
        for(int scale:new int[]{1,2,3,4}) {int height=1080/scale;assertEquals(Math.round(height*.35),StatusHudLayout.procY(height,0));}
        assertEquals(0,StatusHudLayout.procY(240,-999));assertEquals(216,StatusHudLayout.procY(240,999));
    }
}
