package dev.maplesadventure.progression.status;

import dev.maplesadventure.progression.Attribute;

public enum StatusResistanceType {
    IMMUNITY(Attribute.VIGOR), ROBUSTNESS(Attribute.ENDURANCE), FOCUS(Attribute.MIND), VITALITY(Attribute.ARCANE);
    private final Attribute attribute;
    StatusResistanceType(Attribute attribute) { this.attribute = attribute; }
    public Attribute attribute() { return attribute; }
}
