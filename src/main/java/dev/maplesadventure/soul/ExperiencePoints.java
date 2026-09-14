package dev.maplesadventure.soul;

import net.minecraft.server.level.ServerPlayer;

/** Keeps level, progress, and totalExperience mutually consistent without converting points to levels. */
public final class ExperiencePoints {
    public static int capture(ServerPlayer player) {
        return Math.max(0, player.totalExperience);
    }

    public static void setExact(ServerPlayer player, int totalPoints) {
        int points = Math.max(0, totalPoints);
        int level = levelForTotal(points);
        int pointsIntoLevel = points - totalToReachLevel(level);

        player.totalExperience = points;
        player.setExperienceLevels(level);
        player.setExperiencePoints(pointsIntoLevel);
    }

    static int levelForTotal(int points) {
        int low = 0;
        int high = 1;
        while (totalToReachLevelLong(high) <= points && high < 1_000_000) {
            high *= 2;
        }
        while (low + 1 < high) {
            int middle = low + (high - low) / 2;
            if (totalToReachLevelLong(middle) <= points) {
                low = middle;
            } else {
                high = middle;
            }
        }
        return low;
    }

    static int totalToReachLevel(int level) {
        long value = totalToReachLevelLong(level);
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private static long totalToReachLevelLong(int level) {
        long value;
        if (level <= 16) {
            value = (long) level * level + 6L * level;
        } else if (level <= 31) {
            value = (5L * level * level - 81L * level + 720L) / 2L;
        } else {
            value = (9L * level * level - 325L * level + 4440L) / 2L;
        }
        return value;
    }

    private ExperiencePoints() {
    }
}
