package dev.maplesadventure.documentation;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Checks maintained prose pairs and their reciprocal navigation, not translation quality. */
class DocumentationLanguageParityTest {
    private static final Path ROOT = Path.of("").toAbsolutePath();
    private static final List<String> ENGLISH = List.of(
            "AGENTS.md", "HUMANS.md", "ASSET_LICENSE.md", "THIRD_PARTY_NOTICES.md",
            "docs/bonfire.md", "docs/integration/README.md", "docs/integration/armor-api.md",
            "docs/integration/datapack-api.md", "docs/integration/defense-api.md",
            "docs/integration/status-api.md", "docs/integration/typed-damage-api.md",
            "docs/integration/weapon-api.md",
            "src/main/resources/assets/maplesadventure/ASSET_NOTICE.md");

    @Test void maintainedPairsHaveReciprocalLanguageLinks() throws IOException {
        pair("README.en.md", "README.md");
        for (String english : ENGLISH) {
            String chinese = english.substring(0, english.length() - 3) + ".zh-CN.md";
            pair(english, chinese);
        }
    }

    @Test void originalMitLicenseRemainsAuthoritative() throws IOException {
        Path original = ROOT.resolve("LICENSE");
        Path reference = ROOT.resolve("LICENSE.zh-CN.md");
        assertTrue(Files.isRegularFile(original));
        assertTrue(Files.isRegularFile(reference));
        String translated = Files.readString(reference);
        assertTrue(translated.contains("[English](LICENSE)"));
        assertTrue(translated.contains("以英文 `LICENSE` 原文为准"));
        assertTrue(Files.readString(original).startsWith("MIT License"));
    }

    private static void pair(String english, String chinese) throws IOException {
        Path en = ROOT.resolve(english), zh = ROOT.resolve(chinese);
        assertTrue(Files.isRegularFile(en), "Missing: " + english);
        assertTrue(Files.isRegularFile(zh), "Missing: " + chinese);
        assertTrue(Files.readString(en).contains("[简体中文](" + zh.getFileName() + ")"), english);
        assertTrue(Files.readString(zh).contains("[English](" + en.getFileName() + ")"), chinese);
    }
}
