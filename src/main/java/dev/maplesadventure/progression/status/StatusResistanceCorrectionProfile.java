package dev.maplesadventure.progression.status;

import java.util.List;

/** Stage replaces the previous offset; offsets are never cumulatively added. */
public record StatusResistanceCorrectionProfile(List<Double> stages) {
    public static final StatusResistanceCorrectionProfile NONE = new StatusResistanceCorrectionProfile(List.of());
    public static final StatusResistanceCorrectionProfile STANDARD = new StatusResistanceCorrectionProfile(List.of(21.,49.,91.,189.,479.));
    public static final StatusResistanceCorrectionProfile RESISTANT = new StatusResistanceCorrectionProfile(List.of(28.,70.,168.,458.,915.));
    public StatusResistanceCorrectionProfile {
        stages=List.copyOf(stages);
        if(stages.size()>5) throw new IllegalArgumentException("At most five correction stages");
        double previous=0;
        for(double v:stages) { StatusResistance.bounded(v,previous,10000); previous=v; }
    }
    public double offset(int procCount) { return procCount<=0||stages.isEmpty()?0:stages.get(Math.min(procCount,stages.size())-1); }
}
