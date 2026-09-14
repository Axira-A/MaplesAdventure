package dev.maplesadventure.message;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MessageRegistryTest {
    @Test
    void validatesExactTemplateCategoriesAndRejectsArbitraryInput() {
        assertTrue(MessageTemplateRegistry.isValid(new MessagePhrase(
                "location_has_object", List.of("ahead", "treasure")
        )));
        assertFalse(MessageTemplateRegistry.isValid(new MessagePhrase(
                "location_has_object", List.of("jump", "treasure")
        )));
        assertFalse(MessageTemplateRegistry.isValid(new MessagePhrase("unknown", List.of("ahead"))));
        assertFalse(MessageTemplateRegistry.isValid(new MessagePhrase(
                "beware_of", List.of("enemy", "enemy")
        )));
    }

    @Test
    void namedSlotsAndOptionalModifiersRemainServerValidated() {
        MessagePhrase phrase = new MessagePhrase("location_has_object",
                Map.of("location", "ahead", "object", "treasure_chest"),
                Map.of("object", List.of("giant")));
        assertTrue(phrase.isComplete());
        assertTrue(MessageTemplateRegistry.isValid(phrase));
        assertFalse(MessageTemplateRegistry.isValid(new MessagePhrase("location_has_object",
                Map.of("location", "ahead", "object", "treasure_chest"),
                Map.of("object", List.of("jump")))));
    }

    @Test
    void connectorRegistryIsClosedAndPhraseCountProtocolStaysBounded() {
        assertTrue(MessageConnectorRegistry.find("therefore").isPresent());
        assertTrue(MessageConnectorRegistry.find("free_text_connector").isEmpty());
        assertEquals(6, MessageConnectorRegistry.all().size());
    }

    @Test
    void switchingRatingAdjustsBothCountsExactlyOnce() {
        MessageRatingMath.Counts first = MessageRatingMath.apply(
                0, 0, MessageRating.NONE, MessageRating.POSITIVE
        );
        assertEquals(1, first.positive());
        assertEquals(0, first.negative());
        MessageRatingMath.Counts switched = MessageRatingMath.apply(
                first.positive(), first.negative(), MessageRating.POSITIVE, MessageRating.NEGATIVE
        );
        assertEquals(0, switched.positive());
        assertEquals(1, switched.negative());
    }
}
