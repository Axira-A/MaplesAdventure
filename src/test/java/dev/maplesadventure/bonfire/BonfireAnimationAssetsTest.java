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

    @Test void threeClipsUseTheSameRigAndJoinWithoutPoseJumps() {
        Map<String, Track> down = read("sit_down", 1.8);
        Map<String, Track> idle = read("sit_idle", 4.0);
        Map<String, Track> up = read("stand_up", 1.5);
        assertEquals(down.keySet(), idle.keySet());
        assertEquals(down.keySet(), up.keySet());
        for (String bone : down.keySet()) {
            samePose(down.get(bone).last(), idle.get(bone).first(), bone);
            samePose(idle.get(bone).first(), idle.get(bone).last(), bone);
            samePose(idle.get(bone).last(), up.get(bone).first(), bone);
            samePose(down.get(bone).first(), up.get(bone).last(), bone);
        }
    }

    @Test void rootHasNoHorizontalDisplacement() {
        for (String name : new String[] {"sit_down", "sit_idle", "stand_up"}) {
            Track root = read(name, name.equals("sit_down") ? 1.8 : name.equals("sit_idle") ? 4.0 : 1.5).get("Root");
            assertNotNull(root);
            double startX = root.first().get(3).getAsDouble();
            // Epic Fight's exported biped rig is Z-up here; Z is the visual sit-height axis.
            double startY = root.first().get(7).getAsDouble();
            for (var transform : root.transforms()) {
                JsonArray matrix = transform.getAsJsonArray();
                assertEquals(startX, matrix.get(3).getAsDouble(), 1e-5);
                assertEquals(startY, matrix.get(7).getAsDouble(), 1e-5);
            }
        }
    }
}
