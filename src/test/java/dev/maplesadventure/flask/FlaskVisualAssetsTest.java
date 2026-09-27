package dev.maplesadventure.flask;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlaskVisualAssetsTest {
    private static final Path ROOT = Path.of("src/main/resources/assets/maplesadventure");
    private JsonObject json(String path) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(path))).getAsJsonObject();
    }

    @Test void sevenIconsAndThreeDimensionalModelsResolve() throws Exception {
        for (String kind : new String[]{"crimson", "ashen"}) {
            String prefix = kind.equals("crimson") ? "hp" : "mp";
            var overrides = json("models/item/" + kind + "_flask.json").getAsJsonArray("overrides");
            assertEquals(7, overrides.size());
            double previousLiquidVolume = Double.MAX_VALUE;
            for (int frame = 1; frame <= 7; frame++) {
                var icon = ImageIO.read(ROOT.resolve("textures/gui/flask/icons/" + prefix + "flask_" + frame + ".png").toFile());
                assertNotNull(icon);
                assertTrue(icon.getHeight() > icon.getWidth());
                var wrapper = json("models/item/flask/" + kind + "_" + frame + ".json");
                assertEquals("neoforge:separate_transforms", wrapper.get("loader").getAsString());
                assertEquals("minecraft:builtin/entity",
                        wrapper.getAsJsonObject("perspectives").getAsJsonObject("gui").get("parent").getAsString());
                var model = json("models/item/flask/" + kind + "_body_" + frame + ".json");
                assertEquals("minecraft:block/white_concrete", model.getAsJsonObject("textures").get("palette").getAsString());
                var elements = model.getAsJsonArray("elements");
                assertTrue(elements.size() > 50);
                double liquidVolume = 0;
                for (var entry : elements) {
                    var e = entry.getAsJsonObject();
                    var from = e.getAsJsonArray("from"); var to = e.getAsJsonArray("to");
                    double volume = 1;
                    for (int axis = 0; axis < 3; axis++) {
                        double a = from.get(axis).getAsDouble(), b = to.get(axis).getAsDouble();
                        assertTrue(Double.isFinite(a) && Double.isFinite(b) && a >= -16 && b <= 32 && b > a);
                        volume *= b - a;
                    }
                    assertEquals(6, e.getAsJsonObject("faces").size());
                    if (e.get("name").getAsString().startsWith("Liquid")) {
                        liquidVolume += volume;
                        for (var face : e.getAsJsonObject("faces").entrySet()) {
                            String rgba = face.getValue().getAsJsonObject().getAsJsonObject("neoforge_data").get("color").getAsString();
                            assertEquals(120, Integer.parseInt(rgba.substring(0, 2), 16));
                        }
                    }
                }
                assertTrue(liquidVolume < previousLiquidVolume, kind + " frame " + frame);
                previousLiquidVolume = liquidVolume;
                if (frame == 7) assertEquals(0, liquidVolume);
                for (String context : new String[]{"gui", "firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand"})
                    assertTrue(model.getAsJsonObject("display").has(context));
            }
        }
    }

    @Test void glowFramesContainRealTransparency() throws Exception {
        for (int frame = 1; frame <= 8; frame++) {
            var image = ImageIO.read(ROOT.resolve("textures/gui/flask/glow_" + frame + ".png").toFile());
            assertEquals(384, image.getWidth()); assertEquals(408, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
            assertEquals(0, image.getRGB(0, 0) >>> 24);
        }
    }

    @Test void silhouetteAndMaskShaderResolveWithoutChangingItemIcons() throws Exception {
        var mask = ImageIO.read(ROOT.resolve("textures/gui/flask/silhouette.png").toFile());
        assertEquals(1254, mask.getWidth()); assertEquals(1254, mask.getHeight());
        assertTrue(mask.getColorModel().hasAlpha()); assertEquals(0, mask.getRGB(0, 0) >>> 24);
        assertEquals("maplesadventure:flask_alpha_mask", json("shaders/core/flask_alpha_mask.json").get("fragment").getAsString());
        for (String language : new String[]{"en_us", "zh_cn"})
            assertEquals("", json("lang/" + language + ".json").get("message.maplesadventure.flask.ok").getAsString());
    }
}
