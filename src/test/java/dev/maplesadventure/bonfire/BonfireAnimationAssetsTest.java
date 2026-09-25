package dev.maplesadventure.bonfire;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Checks Epic Fight's biped animation resources without requiring the optional mod at test runtime. */
class BonfireAnimationAssetsTest {
    private record Track(JsonArray times, JsonArray transforms) {
        JsonArray first() { return transforms.get(0).getAsJsonArray(); }
        JsonArray last() { return transforms.get(transforms.size() - 1).getAsJsonArray(); }
    }

    private static Map<String, Track> read(String name, double expectedDuration) {
        String path = "/assets/maplesadventure/animmodels/animations/bonfire_" + name + ".json";
        var stream = BonfireAnimationAssetsTest.class.getResourceAsStream(path);
        assertNotNull(stream, path);
        JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        JsonArray animation = root.getAsJsonArray("animation");
        assertEquals(20, animation.size(), "Epic Fight biped should have exactly 20 keyed bones");
        Map<String, Track> tracks = new HashMap<>();
        for (var entry : animation) {
            JsonObject bone = entry.getAsJsonObject();
            String boneName = bone.get("name").getAsString();
            JsonArray times = bone.getAsJsonArray("time");
            JsonArray transforms = bone.getAsJsonArray("transform");
            assertNull(tracks.put(boneName, new Track(times, transforms)), "duplicate bone " + boneName);
            assertEquals(times.size(), transforms.size(), boneName);
            assertTrue(times.size() >= 2, boneName);
            assertEquals(0.0, times.get(0).getAsDouble(), 1e-9, boneName);
            assertEquals(expectedDuration, times.get(times.size() - 1).getAsDouble(), 1e-9, boneName);
            double previous = -1;
            for (int i = 0; i < times.size(); i++) {
                double time = times.get(i).getAsDouble();
                assertTrue(Double.isFinite(time) && time > previous, boneName + " time " + i);
                if (i > 0) assertTrue(time - previous <= 1.0 / 60 + 1e-6, "60 Hz bake: " + boneName);
                previous = time;
                JsonArray matrix = transforms.get(i).getAsJsonArray();
                assertEquals(16, matrix.size(), boneName + " matrix " + i);
                for (var coefficient : matrix) assertTrue(Double.isFinite(coefficient.getAsDouble()));
                assertEquals(1.0, matrix.get(15).getAsDouble(), 1e-9, boneName);
            }
        }
        return tracks;
    }

    private static void samePose(JsonArray first, JsonArray second, String bone) {
        for (int i = 0; i < 16; i++)
            assertEquals(first.get(i).getAsDouble(), second.get(i).getAsDouble(), 1e-5, bone + " matrix entry " + i);
    }

    private static double[] armatureMatrix(Map<String, Track> tracks, String... chain) {
        double[] result = {1,0,0,0, 0,1,0,0, 0,0,1,0, 0,0,0,1};
        for (String name : chain) {
            JsonArray local = tracks.get(name).first();
            double[] next = new double[16];
            for (int row = 0; row < 4; row++)
                for (int column = 0; column < 4; column++)
                    for (int k = 0; k < 4; k++)
                        next[row * 4 + column] += result[row * 4 + k] * local.get(k * 4 + column).getAsDouble();
            result = next;
        }
        return result;
    }

    @Test void seatedKneesAndForearmsExtendTowardRuntimeForwardNotBehindThePlayer() {
        var idle = read("sit_idle", 4.0);
        for (String side : new String[] {"L", "R"}) {
            double[] knee = armatureMatrix(idle, "Root", "Thigh_" + side, "Leg_" + side);
            double[] elbow = armatureMatrix(idle, "Root", "Torso", "Chest", "Shoulder_" + side,
                    "Arm_" + side, "Hand_" + side);
            assertTrue(knee[7] > 0.25, side + " knee must extend forward after Blender/runtime correction");
            assertTrue(elbow[7] > 0.05, side + " forearm must remain in front of torso");
        }
    }

    @Test void fourClipsUseTheSameRigAndJoinWithoutPoseJumps() {
        Map<String, Track> activate = read("activate", 1.8);
        Map<String, Track> down = read("sit_down", 2.0);
        Map<String, Track> idle = read("sit_idle", 4.0);
        Map<String, Track> up = read("stand_up", 1.5);
        assertEquals(activate.keySet(), down.keySet());
        assertEquals(down.keySet(), idle.keySet());
        assertEquals(down.keySet(), up.keySet());
        for (String bone : down.keySet()) {
            samePose(activate.get(bone).first(), down.get(bone).first(), bone);
            samePose(activate.get(bone).last(), down.get(bone).first(), bone);
            samePose(down.get(bone).last(), idle.get(bone).first(), bone);
            samePose(idle.get(bone).first(), idle.get(bone).last(), bone);
            samePose(idle.get(bone).last(), up.get(bone).first(), bone);
            samePose(down.get(bone).first(), up.get(bone).last(), bone);
        }
    }

    @Test void rootHasNoHorizontalDisplacement() {
        for (String name : new String[] {"activate", "sit_down", "sit_idle", "stand_up"}) {
            Track root = read(name, switch (name) {
                case "activate" -> 1.8;
                case "sit_down" -> 2.0;
                case "sit_idle" -> 4.0;
                default -> 1.5;
            }).get("Root");
            assertNotNull(root);
            double startX = root.first().get(3).getAsDouble();
            // Verified against the user's Epic Fight Blender rig and Epic Fight's shipped idle animation.
            double startY = root.first().get(7).getAsDouble();
            for (var transform : root.transforms()) {
                JsonArray matrix = transform.getAsJsonArray();
                assertEquals(startX, matrix.get(3).getAsDouble(), 1e-5);
                assertEquals(startY, matrix.get(7).getAsDouble(), 1e-5);
            }
        }
    }

    @Test void pelvisActuallyReachesTheGroundAndReturns() {
        Track down = read("sit_down", 2.0).get("Root");
        Track idle = read("sit_idle", 4.0).get("Root");
        Track up = read("stand_up", 1.5).get("Root");
        double standing = down.first().get(11).getAsDouble();
        double seated = down.last().get(11).getAsDouble();
        assertTrue(standing - seated > 0.5, "pelvis must lower substantially, not just raise both legs");
        assertTrue(seated > 0.16 && seated < 0.28, "pelvis should rest near the ground");
        assertEquals(seated, idle.first().get(11).getAsDouble(), 1e-5);
        assertEquals(standing, up.last().get(11).getAsDouble(), 1e-5);
    }

    @Test void bakedIkDoesNotFlipOrStretchBetweenSamples() {
        for (var clip : Map.of("activate", 1.8, "sit_down", 2.0, "sit_idle", 4.0, "stand_up", 1.5).entrySet()) {
            for (var entry : read(clip.getKey(), clip.getValue()).entrySet()) {
                Track track = entry.getValue();
                String label = clip.getKey() + "/" + entry.getKey();
                for (int sample = 0; sample < track.transforms().size(); sample++) {
                    JsonArray matrix = track.transforms().get(sample).getAsJsonArray();
                    // IK controls are authoring-only. The shipped biped must retain unit scale.
                    for (int row = 0; row < 3; row++) {
                        for (int other = 0; other < 3; other++) {
                            double dot = 0;
                            for (int column = 0; column < 3; column++)
                                dot += matrix.get(row * 4 + column).getAsDouble()
                                        * matrix.get(other * 4 + column).getAsDouble();
                            assertEquals(row == other ? 1.0 : 0.0, dot, 1e-4, label + " scale/skew");
                        }
                    }
                    if (sample == 0) continue;
                    JsonArray previous = track.transforms().get(sample - 1).getAsJsonArray();
                    double trace = 0;
                    for (int row = 0; row < 3; row++)
                        for (int column = 0; column < 3; column++)
                            trace += previous.get(row * 4 + column).getAsDouble()
                                    * matrix.get(row * 4 + column).getAsDouble();
                    double angle = Math.acos(Math.clamp((trace - 1.0) / 2.0, -1.0, 1.0));
                    double interval = track.times().get(sample).getAsDouble()
                            - track.times().get(sample - 1).getAsDouble();
                    assertTrue(interval <= 0.10001, label + " sparse samples");
                    assertTrue(angle < Math.PI / 4, label + " IK branch flip at sample " + sample);
                }
            }
        }
    }
}
