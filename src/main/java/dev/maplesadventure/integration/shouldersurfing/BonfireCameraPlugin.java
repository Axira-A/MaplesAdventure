package dev.maplesadventure.integration.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.event.SetupCameraRotationEvent;
import com.github.exopandora.shouldersurfing.api.client.event.handler.SetupCameraRotationEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.math.Vec2f;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge.Pose;
import dev.maplesadventure.client.bonfire.BonfireCameraMath;
import dev.maplesadventure.client.bonfire.BonfireClient;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Loaded only by Shoulder Surfing's plugin loader. Never changes settings, perspective or player pose. */
public final class BonfireCameraPlugin implements IShoulderSurfingPlugin {
    private UUID nonce;
    private ClientLevel level;
    private long leaving, lastFrame;
    private double mouseX, mouseY, returnMillis, playerAngle;
    private Vec2f originalRotation;
    private Vec3 focus, station, forward;
    private Pose lastPose, returnFrom;
    private boolean optedIn, selected;

    @Override public void register(IEventBus bus) {
        bus.register((SetupCameraRotationEventHandler) this::rotation);
        BonfireCameraBridge.register(this::afterSetup);
    }

    private Pose afterSetup(Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        if (camera != mc.gameRenderer.getMainCamera()) return null;
        var view = BonfireClient.currentView();
        if (view == null || mc.player == null || mc.level == null || mc.getCameraEntity() != mc.player) {
            reset();
            return null;
        }
        long now = System.nanoTime();
        if (!view.nonce().equals(nonce) || level != mc.level) {
            reset();
            nonce = view.nonce(); level = mc.level; lastFrame = now;
            optedIn = view.state() == BonfireSessionState.SITTING_DOWN
                    && IShoulderSurfing.getInstance().isShoulderSurfing();
            if (optedIn) {
                var shoulderCamera = IShoulderSurfing.getInstance().getCamera();
                originalRotation = new Vec2f(shoulderCamera.getXRot(), shoulderCamera.getYRot());
                focus = Vec3.atCenterOf(view.bonfirePos()).add(0, .15, 0);
            }
        }
        if (!optedIn || !IShoulderSurfing.getInstance().isShoulderSurfing()) {
            optedIn = false;
            return null;
        }
        double elapsed = BonfireClient.transitionElapsedTicks();
        if (!selected) {
            // No turn or translation during either visible fade. A skipped blackout never causes a late cut.
            if (view.state() != BonfireSessionState.SITTING_DOWN
                    || !BonfireCameraMath.mayAcquire(elapsed, view.commitTick(), view.fadeInTick())) return null;
            selected = true;
            station = chooseStation(mc);
            if (station == null) { optedIn = false; return null; }
            forward = focus.subtract(station).normalize();
        }
        if (view.state() == BonfireSessionState.STANDING_UP) {
            if (leaving == 0) {
                leaving = now; returnFrom = lastPose;
                // Finish before the server releases the pose, including the shorter no-Epic-Fight fallback.
                returnMillis = Math.min(650, Math.max(50, view.transitionTicks() * 50.0 - 50));
            }
            if (returnFrom == null) return null;
            double weight = BonfireCameraMath.returnWeight((now - leaving) / 1e6, returnMillis);
            if (weight <= 0) return null;
            Vec3 position = camera.getPosition().lerp(returnFrom.position(), weight);
            Vec3 anchor = mc.player.getEyePosition(camera.getPartialTickTime()).lerp(focus, weight);
            position = clipCamera(mc, anchor, position);
            return new Pose(position, Mth.rotLerp((float) weight, camera.getYRot(), returnFrom.yaw()),
                    Mth.lerp((float) weight, camera.getXRot(), returnFrom.pitch()),
                    Mth.lerp((float) weight, camera.getRoll(), returnFrom.roll()));
        }
        double dt = (now - lastFrame) / 1e9;
        lastFrame = now;
        if (view.state() == BonfireSessionState.RESTING && mc.screen != null) {
            mouseX = BonfireCameraMath.smooth(mouseX,
                    BonfireCameraMath.cursor(mc.mouseHandler.xpos(), mc.getWindow().getScreenWidth()), dt);
            mouseY = BonfireCameraMath.smooth(mouseY,
                    BonfireCameraMath.cursor(mc.mouseHandler.ypos(), mc.getWindow().getScreenHeight()), dt);
        }
        Vec3 desired = BonfireCameraMath.constrainOrbit(focus, station,
                station.add(BonfireCameraMath.parallax(forward, mouseX, mouseY)), playerAngle);
        Vec3 position = clipCamera(mc, focus, desired);
        // Translate the viewing plane; do not cancel parallax with an opposite aim correction.
        float yaw = (float) Math.toDegrees(Math.atan2(-forward.x, forward.z));
        lastPose = new Pose(position, yaw, (float) BonfireCameraMath.PITCH, 0);
        return lastPose;
    }

    private Vec3 chooseStation(Minecraft mc) {
        playerAngle = Math.atan2(mc.player.getZ() - focus.z, mc.player.getX() - focus.x);
        SplittableRandom random = new SplittableRandom(nonce.getMostSignificantBits() ^ nonce.getLeastSignificantBits());
        // Once per rest, bounded search; no chunk loads.
        for (int i = 0; i < 32; i++) {
            Vec3 candidate = focus.add(BonfireCameraMath.stationOffset(playerAngle, random.nextDouble()));
            if (!mc.level.hasChunk(Mth.floor(candidate.x) >> 4, Mth.floor(candidate.z) >> 4)) continue;
            if (!mc.level.noCollision(new AABB(candidate, candidate).inflate(.15))) continue;
            if (clipCamera(mc, focus, candidate).distanceToSqr(candidate) < 1e-8) return candidate;
        }
        return null; // No safe side shot: keep the shoulder camera, never cut through a wall.
    }

    private static Vec3 clipCamera(Minecraft mc, Vec3 origin, Vec3 desired) {
        Vec3 delta = desired.subtract(origin);
        double length = delta.length();
        if (length < 1e-6) return desired;
        double fraction = 1;
        for (int i = 0; i < 8; i++) {
            Vec3 padding = new Vec3((i & 1) == 0 ? -.1 : .1, (i & 2) == 0 ? -.1 : .1, (i & 4) == 0 ? -.1 : .1);
            Vec3 from = origin.add(padding);
            var hit = mc.level.clip(new ClipContext(from, desired.add(padding),
                    ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, mc.player));
            if (hit.getType() != HitResult.Type.MISS)
                fraction = Math.min(fraction, Math.max(0, (from.distanceTo(hit.getLocation()) - .05) / length));
        }
        return origin.add(delta.scale(fraction));
    }

    private void rotation(SetupCameraRotationEvent event) {
        var view = BonfireClient.currentView();
        if (!optedIn || view == null || !view.nonce().equals(nonce)
                || !IShoulderSurfing.getInstance().isShoulderSurfing()) return;
        event.setResult(originalRotation);
        event.cancel();
    }

    private void reset() {
        nonce = null; level = null; focus = station = forward = null; originalRotation = null;
        lastPose = returnFrom = null;
        leaving = lastFrame = 0; mouseX = mouseY = returnMillis = 0;
        optedIn = selected = false;
    }
}
