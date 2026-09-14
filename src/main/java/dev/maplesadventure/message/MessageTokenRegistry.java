package dev.maplesadventure.message;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Closed semantic vocabulary shared by both logical sides. */
public final class MessageTokenRegistry {
    private static final Map<String, MessageToken> TOKENS = new LinkedHashMap<>();
    private static final Map<MessageTokenCategory, List<MessageToken>> BY_CATEGORY =
            new EnumMap<>(MessageTokenCategory.class);

    static {
        registerLocations("here", "ahead", "behind", "left", "right", "above", "below", "nearby",
                "far_away", "corner", "depths");
        registerObjects(MessageTokenCategory.OBJECT_ITEM, "treasure", "treasure_chest", "key", "weapon", "armor", "sorcery", "item");
        registerObjects(MessageTokenCategory.OBJECT_CREATURE, "enemy", "strong_enemy", "boss", "npc", "merchant", "dog");
        registerObjects(MessageTokenCategory.OBJECT_PLACE, "door", "road", "hidden_path", "cave", "bonfire", "mechanism",
                "trap", "bridge", "ladder", "entrance", "exit");
        registerObjects(MessageTokenCategory.OBJECT_ENVIRONMENT, "water", "lava", "cliff", "view", "sun");
        registerActions("jump", "attack", "defend", "dodge", "run", "sneak", "observe", "interact", "wait",
                "go_around", "ranged_attack", "magic", "turn_back", "go_forward");
        registerSimple("dont_give_up", "keep_going", "good_luck", "well_done", "take_a_rest", "beautiful_view",
                "suspicious", "safe", "dangerous", "nothing_here");
        registerModifiers(MessageModifierType.ADJECTIVE_SIZE, MessageTokenCategory.ADJECTIVE_SIZE, "giant", "tiny");
        registerModifiers(MessageModifierType.ADJECTIVE_QUALITY, MessageTokenCategory.ADJECTIVE_QUALITY,
                "powerful", "weak", "hidden", "suspicious_adjective", "precious", "useless", "strange", "beautiful");
        registerModifiers(MessageModifierType.ADJECTIVE_DANGER, MessageTokenCategory.ADJECTIVE_DANGER,
                "dangerous_adjective", "safe_adjective");
        for (MessageTokenCategory category : MessageTokenCategory.values()) {
            BY_CATEGORY.computeIfPresent(category, (ignored, values) -> Collections.unmodifiableList(values));
        }
    }

    public static Optional<MessageToken> find(String id) { return Optional.ofNullable(TOKENS.get(id)); }
    public static List<MessageToken> all() { return List.copyOf(TOKENS.values()); }
    public static List<MessageToken> tokens(MessageTokenCategory category) { return BY_CATEGORY.getOrDefault(category, List.of()); }
    public static List<MessageToken> tokensFor(MessageSlotType type) {
        return TOKENS.values().stream().filter(token -> !token.isModifier() && token.allowedIn(type)).toList();
    }
    public static List<MessageToken> modifiers(MessageModifierType type) {
        return TOKENS.values().stream().filter(token -> token.modifierType() == type).toList();
    }

    private static void registerLocations(String... ids) {
        for (String id : ids) register(id, MessageTokenCategory.LOCATION, "location", Set.of(MessageSlotType.LOCATION), null);
    }
    private static void registerObjects(MessageTokenCategory category, String... ids) {
        for (String id : ids) register(id, category, "object", Set.of(MessageSlotType.OBJECT, MessageSlotType.SUBJECT), null);
    }
    private static void registerActions(String... ids) {
        for (String id : ids) register(id, MessageTokenCategory.ACTION, "action", Set.of(MessageSlotType.ACTION), null);
    }
    private static void registerSimple(String... ids) {
        for (String id : ids) register(id, MessageTokenCategory.SIMPLE_PHRASE, "simple", Set.of(MessageSlotType.SIMPLE_PHRASE), null);
    }
    private static void registerModifiers(MessageModifierType type, MessageTokenCategory category, String... ids) {
        for (String id : ids) register(id, category, "adjective", Set.of(), type);
    }
    private static void register(String id, MessageTokenCategory category, String keyGroup,
                                 Set<MessageSlotType> slots, MessageModifierType modifierType) {
        MessageToken token = new MessageToken(id, category,
                "message.maplesadventure.token." + keyGroup + "." + id, slots, modifierType, Set.of());
        if (TOKENS.putIfAbsent(id, token) != null) throw new IllegalStateException("Duplicate message token: " + id);
        BY_CATEGORY.computeIfAbsent(category, ignored -> new ArrayList<>()).add(token);
    }
    private MessageTokenRegistry() {}
}
