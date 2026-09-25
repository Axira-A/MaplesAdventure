package dev.maplesadventure.bonfire;

import dev.maplesadventure.client.bonfire.BonfireCameraMath;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BonfireCameraMathTest {
    @Test void everyStationHasTwoBlockRadiusTwentyDegreePitchAndOnlySideSectors() {
        for (int n = 0; n <= 1000; n++) {
            double u = n / 1000.0;
            double side = BonfireCameraMath.sideAngle(u);
            assertTrue(side >= 45 && side <= 135 || side >= 225 && side <= 315);
            for (double heading : new double[] {-Math.PI, -.7, 0, 1.8, Math.PI}) {
                Vec3 offset = BonfireCameraMath.stationOffset(heading, u);
                assertEquals(2, offset.horizontalDistance(), 1e-12);
                assertEquals(20, Math.toDegrees(Math.atan2(offset.y, offset.horizontalDistance())), 1e-12);
                Vec3 forward = offset.scale(-1).normalize();
                Vec3 focus = new Vec3(20, 60.65, -8);
                Vec3 station = focus.add(offset);
                for (double x : new double[] {-1, 1}) for (double y : new double[] {-1, 1}) {
                    Vec3 p = BonfireCameraMath.constrainOrbit(focus, station,
                            station.add(BonfireCameraMath.parallax(forward, x, y)), heading).subtract(focus);
                    assertEquals(2, p.horizontalDistance(), 1e-12);
                    double angle = (Math.toDegrees(Math.atan2(p.z, p.x) - heading) % 360 + 360) % 360;
                    assertTrue(angle >= 45 - 1e-9 && angle <= 135 + 1e-9
                            || angle >= 225 - 1e-9 && angle <= 315 + 1e-9);
                }
            }
        }
    }
    @Test void bothAxesMoveOppositeTheCursorInCameraPlane() {
        Vec3 forward = BonfireCameraMath.stationOffset(0, .25).scale(-1).normalize();
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 down = forward.cross(right).normalize();
        assertTrue(BonfireCameraMath.parallax(forward, 1, 0).dot(right) < 0);
        assertTrue(BonfireCameraMath.parallax(forward, 0, 1).dot(down) < 0);
        assertTrue(BonfireCameraMath.parallax(forward, -1, 0).dot(right) > 0);
        assertTrue(BonfireCameraMath.parallax(forward, 0, -1).dot(down) > 0);
    }
    @Test void shotAcquisitionIsRestrictedToFullBlackEvenAfterADroppedFrame() {
        for (int[] timing : new int[][] {{43, 14, 20}, {24, 10, 16}}) {
            for (double tick = 0; tick < 60; tick += .125) {
                if (BonfireCameraMath.mayAcquire(tick, timing[1], timing[2]))
                    assertEquals(1, BonfireTransitionMath.fadeAlpha(tick, timing[0], timing[1], timing[2]));
            }
            assertFalse(BonfireCameraMath.mayAcquire(timing[1] - .001, timing[1], timing[2]));
            assertFalse(BonfireCameraMath.mayAcquire(timing[2], timing[1], timing[2]));
            assertFalse(BonfireCameraMath.mayAcquire(100, timing[1], timing[2]));
        }
    }
    @Test void returnReachesTheUntouchedShoulderCameraBeforeUnlock() {
        for (double duration : new double[] {350, 650}) {
            assertEquals(1, BonfireCameraMath.returnWeight(0, duration));
            assertEquals(.5, BonfireCameraMath.returnWeight(duration / 2, duration));
            assertEquals(0, BonfireCameraMath.returnWeight(duration, duration));
            assertEquals(0, BonfireCameraMath.returnWeight(duration + 50, duration));
        }
    }
    @Test void cameraEntersAndReturnsWithBoundedSmoothBlend() {
        assertEquals(0, BonfireCameraMath.blend(-100));
        assertEquals(0, BonfireCameraMath.blend(0));
        assertEquals(.5, BonfireCameraMath.blend(325));
        assertEquals(1, BonfireCameraMath.blend(650));
        assertEquals(1, BonfireCameraMath.blend(5000));
    }
    @Test void cursorParallaxIsSmallAndNormalized() {
        assertEquals(-1, BonfireCameraMath.cursor(-200, 1920));
        assertEquals(0, BonfireCameraMath.cursor(960, 1920));
        assertEquals(1, BonfireCameraMath.cursor(3000, 1920));
        assertEquals(0, BonfireCameraMath.cursor(30, 0));
    }
    @Test void smoothingIsFrameRateIndependentAndNeverOvershoots() {
        double at30 = 0, at60 = 0;
        for (int i = 0; i < 30; i++) at30 = BonfireCameraMath.smooth(at30, 1, 1.0 / 30);
        for (int i = 0; i < 60; i++) at60 = BonfireCameraMath.smooth(at60, 1, 1.0 / 60);
        assertEquals(at30, at60, 1e-12);
        assertTrue(at30 < 1 && at30 > .99);
        assertEquals(.2, BonfireCameraMath.smooth(.2, 1, -1));
    }
}
