package dev.maplesadventure.network;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NetworkProtocolContractTest {
    @Test void protocol25IsRequiredAndNotOptional() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dev/maplesadventure/network/MessageNetwork.java"));
        assertTrue(source.contains("PROTOCOL_VERSION = \"25\""));
        assertFalse(source.contains(".optional()"));
    }
}
