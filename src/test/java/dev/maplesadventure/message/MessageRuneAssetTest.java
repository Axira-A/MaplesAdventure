package dev.maplesadventure.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class MessageRuneAssetTest {
    @Test
    void optimizedRuneIsTransparentHorizontalAndBounded() throws Exception {
        String path = "/assets/maplesadventure/textures/message/message_rune.png";
        try (InputStream stream = MessageRuneAssetTest.class.getResourceAsStream(path)) {
            assertNotNull(stream);
            BufferedImage image = ImageIO.read(stream);
            assertNotNull(image);
            assertEquals(768, image.getWidth());
            assertEquals(256, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
        }
        try (InputStream stream = MessageRuneAssetTest.class.getResourceAsStream(path)) {
            assertNotNull(stream);
            assertTrue(stream.readAllBytes().length < 300_000);
        }
    }
}
