package dev.maplesadventure.client.bonfire;

import dev.maplesadventure.bonfire.BonfireSessionState;
import dev.maplesadventure.client.bonfire.BonfireCameraBridge.Pose;
import java.util.SplittableRandom;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Shared presentation only; optional adapters supply their public camera API, never their types. */
public final class BonfireCameraController {
    public record Rotation(float pitch, float yaw) {}
    private final BooleanSupplier shoulderActive;
    private final Supplier<Rotation> shoulderRotation;
    private UUID nonce;
    private ClientLevel level;
    private long leaving, lastFrame;
    private double mouseX, mouseY, returnMillis, playerAngle;
    private Rotation originalRotation;
    private Vec3 focus, station, forward;
    private Pose lastPose, returnFrom;
    private boolean optedIn, selected;

    public BonfireCameraController(BooleanSupplier shoulderActive, Supplier<Rotation> shoulderRotation) {
        this.shoulderActive = shoulderActive;
        this.shoulderRotation = shoulderRotation;
    }

    public Pose afterSetup(Camera camera) {
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
                    && shoulderActive.getAsBoolean();
            if (optedIn) {
                originalRotation = shoulderRotation.get();
                focus = Vec3.atCenterOf(view.bonfirePos()).add(0, .15, 0);
            }
        }
        if (!optedIn || !shoulderActive.getAsBoolean()) {
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

    /** Keep the underlying shoulder view stable until the shared return transition finishes. */
    public Rotation rotationOverride() {
        var view = BonfireClient.currentView();
        if (!optedIn || view == null || !view.nonce().equals(nonce)
                || !shoulderActive.getAsBoolean()) return null;
        return originalRotation;
    }

    private void reset() {
        nonce = null; level = null; focus = station = forward = null; originalRotation = null;
        lastPose = returnFrom = null;
        leaving = lastFrame = 0; mouseX = mouseY = returnMillis = 0;
        optedIn = selected = false;
    }
}
