package dev.maplesadventure.authoring.logic;
import java.util.*;
public record LogicBinding(UUID id, boolean enabled, LogicDefinition event, ConditionExpression conditions,
                           List<LogicDefinition> actions) {
    public LogicBinding {Objects.requireNonNull(id);Objects.requireNonNull(event);Objects.requireNonNull(conditions);
        actions=List.copyOf(actions);if(actions.size()>LogicLimits.ACTIONS)throw new IllegalArgumentException("Action limit");}
}
