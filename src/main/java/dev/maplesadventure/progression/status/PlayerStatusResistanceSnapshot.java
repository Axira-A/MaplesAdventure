package dev.maplesadventure.progression.status;

import java.util.Map;

public record PlayerStatusResistanceSnapshot(Map<StatusResistanceType, Breakdown> values) {
    public record Breakdown(double base, double level, double attribute, double equipment, double effect) {
        public Breakdown {
            for (double v : new double[]{base, level, attribute, equipment, effect}) StatusResistance.bounded(v, 0, 10000);
        }
        public double total() { return base + level + attribute + equipment + effect; }
    }
    public PlayerStatusResistanceSnapshot { values = Map.copyOf(values); }
    public double value(StatusResistanceType type) { return values.get(type).total(); }
    public double immunity() { return value(StatusResistanceType.IMMUNITY); }
    public double robustness() { return value(StatusResistanceType.ROBUSTNESS); }
    public double focus() { return value(StatusResistanceType.FOCUS); }
    public double vitality() { return value(StatusResistanceType.VITALITY); }
}
