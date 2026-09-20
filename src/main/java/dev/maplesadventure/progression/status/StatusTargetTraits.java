package dev.maplesadventure.progression.status;

/** Explicit entity semantics supplied by the existing defense profile resolver. */
public record StatusTargetTraits(boolean tarnishedLike, boolean madnessImmune, boolean deathBlightImmune, boolean allowDeathBlight) {
    public static final StatusTargetTraits DEFAULT=new StatusTargetTraits(false,false,false,false);
}
