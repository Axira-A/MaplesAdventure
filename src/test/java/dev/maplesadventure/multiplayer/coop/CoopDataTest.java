package dev.maplesadventure.multiplayer.coop;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class CoopDataTest {
    @Test
    void helperRuneIsTransparentHorizontalAndBounded() throws Exception {
        String path = "/assets/maplesadventure/textures/summon/rune_helper.png";
        try (InputStream stream = CoopDataTest.class.getResourceAsStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            org.junit.jupiter.api.Assertions.assertEquals(768, image.getWidth());
            org.junit.jupiter.api.Assertions.assertEquals(256, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
        }
        try (InputStream stream = CoopDataTest.class.getResourceAsStream(path)) {
            assertTrue(stream.readAllBytes().length < 300_000);
        }
    }
}
