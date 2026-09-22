package dev.maplesadventure.progression.client;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.maplesadventure.progression.*;
import dev.maplesadventure.progression.stats.*;
import dev.maplesadventure.progression.weapon.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class CharacterStatsPresentationTest {
    private static final String EMPTY = "screen.maplesadventure.weapon.empty_hand";
    private static final String NON_WEAPON = "screen.maplesadventure.weapon.no_weapon";
    private static final PlayerAttributeState ATTRIBUTES = PlayerAttributeState.defaultsState();

    private static WeaponLoadoutSnapshot.Held held(String item, boolean weapon) {
        return new WeaponLoadoutSnapshot.Held(ResourceLocation.withDefaultNamespace(item), WeaponRequirementProfile.NONE,
                WeaponScalingProfile.automatic(WeaponRequirementArchetype.SWORD), weapon ? 7 : 0, weapon);
    }
    private static CharacterStatsSnapshot snapshot(WeaponLoadoutSnapshot loadout) {
        return CharacterStatCalculator.calculate(ATTRIBUTES).withWeapons(loadout, ATTRIBUTES);
    }
    private static String key(Component component) {
        return assertInstanceOf(TranslatableContents.class, component.getContents()).getKey();
    }
    private static JsonObject language(String locale) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of("src/main/resources/assets/maplesadventure/lang/" + locale + ".json"))).getAsJsonObject();
    }

    @Test void mainHandAirIsEmpty() {
        var stats = snapshot(WeaponLoadoutSnapshot.empty());
        assertEquals(WeaponLoadoutSnapshot.HandState.EMPTY, stats.handState(CharacterStat.MAIN_HAND_ATTACK).orElseThrow());
        assertEquals(EMPTY, key(CharacterStatFormatting.comparison(CharacterStat.MAIN_HAND_ATTACK, stats, stats)));
    }
    @Test void offHandAirIsEmpty() {
        var stats = snapshot(WeaponLoadoutSnapshot.empty());
        assertEquals(WeaponLoadoutSnapshot.HandState.EMPTY, stats.handState(CharacterStat.OFF_HAND_ATTACK).orElseThrow());
        assertEquals(EMPTY, key(CharacterStatFormatting.comparison(CharacterStat.OFF_HAND_ATTACK, stats, stats)));
    }
    @Test void torchIsNotAWeaponInEitherHand() {
        var torch = held("torch", false);
        var stats = snapshot(new WeaponLoadoutSnapshot(torch, torch, .35));
        for (var stat : new CharacterStat[]{CharacterStat.MAIN_HAND_ATTACK, CharacterStat.OFF_HAND_ATTACK}) {
            assertEquals(WeaponLoadoutSnapshot.HandState.NON_WEAPON, stats.handState(stat).orElseThrow());
            assertEquals(NON_WEAPON, key(CharacterStatFormatting.comparison(stat, stats, stats)));
        }
        assertTrue(stats.weapons().isEmpty());
        assertFalse(torch.weapon());
    }
    @Test void swordStillUsesCanonicalAttackRating() {
        var loadout = new WeaponLoadoutSnapshot(held("iron_sword", true), held("air", false), .35);
        var stats = snapshot(loadout);
        assertEquals(WeaponLoadoutSnapshot.HandState.WEAPON, stats.handState(CharacterStat.MAIN_HAND_ATTACK).orElseThrow());
        assertTrue(CharacterStatFormatting.handDescription(stats, CharacterStat.MAIN_HAND_ATTACK).isEmpty());
        assertEquals(loadout.evaluate(ATTRIBUTES).getFirst().attack().attackRating(), stats.value(CharacterStat.MAIN_HAND_ATTACK).value(), 0);
        assertEquals(StatImplementationState.ACTIVE, stats.value(CharacterStat.MAIN_HAND_ATTACK).implementation());
    }
    @Test void absentSnapshotRemainsTrulyUnavailableNotEmpty() {
        var stats = CharacterStatCalculator.calculate(ATTRIBUTES);
        assertTrue(stats.handState(CharacterStat.MAIN_HAND_ATTACK).isEmpty());
        assertEquals(StatImplementationState.UNAVAILABLE.translationKey(),
                key(CharacterStatFormatting.comparison(CharacterStat.MAIN_HAND_ATTACK, stats, stats)));
    }
    @Test void activeHasNoImplementationTooltip() {
        assertTrue(CharacterStatFormatting.implementationHint(StatImplementationState.ACTIVE).isEmpty());
    }
    @Test void previewOnlyRetainsNecessaryHint() {
        assertEquals(StatImplementationState.PREVIEW_ONLY.translationKey(),
                key(CharacterStatFormatting.implementationHint(StatImplementationState.PREVIEW_ONLY).orElseThrow()));
        assertEquals(StatImplementationState.PREVIEW_ONLY,
                CharacterStatCalculator.calculate(ATTRIBUTES).value(CharacterStat.MAX_MANA).implementation());
    }
    @Test void unavailableHasNeutralText() throws Exception {
        var hint = CharacterStatFormatting.implementationHint(StatImplementationState.UNAVAILABLE).orElseThrow();
        assertEquals("Currently unavailable", language("en_us").get(key(hint)).getAsString());
        assertEquals("当前无法获取该数据", language("zh_cn").get(key(hint)).getAsString());
    }
    @Test void chinesePresentationKeysAreComplete() throws Exception { assertLanguage("zh_cn"); }
    @Test void englishPresentationKeysAreComplete() throws Exception { assertLanguage("en_us"); }
    private static void assertLanguage(String locale) throws Exception {
        var lang = language(locale);
        for (String id : new String[]{EMPTY, NON_WEAPON, StatImplementationState.PREVIEW_ONLY.translationKey(),
                StatImplementationState.UNAVAILABLE.translationKey()}) assertFalse(lang.get(id).getAsString().isBlank());
        assertFalse(lang.has("screen.maplesadventure.level_up.design_only"));
        for (var attribute : Attribute.values()) {
            var text = lang.get(attribute.translationKey() + ".description").getAsString();
            assertFalse(text.isBlank());
            assertFalse(text.contains("未来"));
            assertFalse(text.contains("Future:"));
        }
    }
    @Test void draftRetainsBothHandContextsWithoutChangingNumericStats() {
        var base = new AttributeSnapshot(ATTRIBUTES, 5, 20, 100, 20, 118,
                dev.maplesadventure.progression.runtime.RuntimeResourceSnapshot.progressionOnly(ATTRIBUTES),
                dev.maplesadventure.progression.encumbrance.EquipLoadRuntimeSnapshot.unavailable());
        var loadout = new WeaponLoadoutSnapshot(held("torch", false), held("air", false), .35);
        var baseline = new AttributeSnapshot(base.state(),base.level(),base.maxHealth(),base.mana(),base.stamina(),
                base.nextLevelCost(),base.runtimeResources(),base.equipLoad(),base.spellSchools(),loadout);
        var draft = LevelUpPreviewCalculator.calculate(baseline, Map.of(Attribute.STRENGTH, 1), 10000, 99, 1);
        assertEquals(WeaponLoadoutSnapshot.HandState.NON_WEAPON, baseline.characterStats().handState(CharacterStat.MAIN_HAND_ATTACK).orElseThrow());
        assertEquals(WeaponLoadoutSnapshot.HandState.NON_WEAPON, draft.characterStats().handState(CharacterStat.MAIN_HAND_ATTACK).orElseThrow());
        assertEquals(WeaponLoadoutSnapshot.HandState.EMPTY, draft.characterStats().handState(CharacterStat.OFF_HAND_ATTACK).orElseThrow());
        var withoutEquipment = CharacterStatCalculator.calculate(draft.state());
        for (var stat : CharacterStat.values()) assertEquals(withoutEquipment.value(stat), draft.characterStats().value(stat));
        assertEquals(5, baseline.state().get(Attribute.STRENGTH));
    }
}
