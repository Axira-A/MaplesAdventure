package dev.maplesadventure.multiplayer.invasion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.maplesadventure.multiplayer.phase.PhaseId;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class InvasionDataTest {
    @Test void invaderRuneIsTransparentHorizontalAndBounded() throws Exception {
        String path = "/assets/maplesadventure/textures/invasion/rune_invader.png";
        try (InputStream stream = InvasionDataTest.class.getResourceAsStream(path)) {
            BufferedImage image = ImageIO.read(stream);
            assertEquals(768, image.getWidth());
            assertEquals(256, image.getHeight());
            assertTrue(image.getColorModel().hasAlpha());
        }
        try (InputStream stream = InvasionDataTest.class.getResourceAsStream(path)) {
            assertTrue(stream.readAllBytes().length < 300_000);
        }
    }

    @Test void sessionHasExplicitMaterializationAndActivationBoundary() {
        UUID host = UUID.randomUUID(), coop = UUID.randomUUID(), invader = UUID.randomUUID();
        InvasionSession session = new InvasionSession(UUID.randomUUID(), host, coop, invader, PhaseId.solo(host),
                HostileSessionType.INVASION, 1L);
        assertEquals(InvasionSessionState.PREPARING, session.state());
        session.materialize(100L, 30);
        assertEquals(InvasionSessionState.MATERIALIZING, session.state());
        assertEquals(130L, session.materializeUntilTick());
        session.activate();
        assertEquals(InvasionSessionState.ACTIVE, session.state());
    }

    @Test void duelUsesSameHostileRuntimeWithoutASecondCooperator() {
        UUID host = UUID.randomUUID(), duelist = UUID.randomUUID();
        InvasionSession session = new InvasionSession(UUID.randomUUID(), host, InvasionSession.NO_COOPERATOR,
                duelist, PhaseId.solo(host), HostileSessionType.DUEL, 1L);
        assertEquals(HostileSessionType.DUEL, session.type());
        assertTrue(!session.hasCooperator());
    }

}
