package dev.maplesadventure.client.bonfire;

import net.minecraft.world.phys.Vec3;

/** Frame-rate-independent presentation timing; never feeds back into player rotation or gameplay. */
public final class BonfireCameraMath {
    public static final double RADIUS = 2.0;
    public static final double PITCH = 20.0;
    /** Two side sectors; excludes the 90-degree wedges toward and away from the player. */
    public static double sideAngle(double sample) {
        double u = Math.clamp(sample, 0, Math.nextDown(1.0));
        return u < .5 ? 45 + u * 180 : 225 + (u - .5) * 180;
    }
    public static double heightAboveFocus() { return RADIUS * Math.tan(Math.toRadians(PITCH)); }
    public static Vec3 stationOffset(double playerAngle, double sample) {
        double angle = playerAngle + Math.toRadians(sideAngle(sample));
        return new Vec3(Math.cos(angle) * RADIUS, heightAboveFocus(), Math.sin(angle) * RADIUS);
    }
    public static Vec3 parallax(Vec3 forward, double mouseX, double mouseY) {
        Vec3 right = forward.cross(new Vec3(0, 1, 0)).normalize();
        Vec3 down = forward.cross(right).normalize();
        return right.scale(reverseParallax(mouseX, .20)).add(down.scale(reverseParallax(mouseY, .12)));
    }
    /** Parallax must not move a boundary-angle shot into either forbidden front/back wedge. */
    public static Vec3 constrainOrbit(Vec3 focus, Vec3 station, Vec3 desired, double playerAngle) {
        double base = relativeAngle(station.subtract(focus), playerAngle);
        double angle = relativeAngle(desired.subtract(focus), playerAngle);
        angle = base < 180 ? Math.clamp(angle, 45, 135) : Math.clamp(angle, 225, 315);
        double radians = playerAngle + Math.toRadians(angle);
        return new Vec3(focus.x + RADIUS * Math.cos(radians), desired.y, focus.z + RADIUS * Math.sin(radians));
    }
    private static double relativeAngle(Vec3 radial, double playerAngle) {
        double angle = Math.toDegrees(Math.atan2(radial.z, radial.x) - playerAngle);
        return (angle % 360 + 360) % 360;
    }
    public static boolean mayAcquire(double elapsedTicks, int commitTick, int fadeInTick) {
        return elapsedTicks >= commitTick && elapsedTicks < fadeInTick;
    }
    public static double reverseParallax(double normalizedCursor, double amplitude) {
        return -Math.clamp(normalizedCursor, -1, 1) * amplitude;
    }
    public static double returnWeight(double elapsedMillis, double durationMillis) {
        return 1 - smoothStep(elapsedMillis / Math.max(1, durationMillis));
    }
    public static double blend(double milliseconds) {
        return smoothStep(milliseconds / 650.0);
    }
    private static double smoothStep(double fraction) {
        double t = Math.clamp(fraction, 0, 1);
        return t * t * (3 - 2 * t);
    }
    public static double cursor(double position, double extent) {
        return extent <= 0 ? 0 : Math.clamp(2 * position / extent - 1, -1, 1);
    }
    public static double smooth(double previous, double target, double elapsedSeconds) {
        return previous + (target - previous) * (1 - Math.exp(-Math.clamp(elapsedSeconds, 0, .1) * 10));
    }
    private BonfireCameraMath() {}
}
