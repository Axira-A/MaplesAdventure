package dev.maplesadventure.multiplayer.echo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

final class EchoTrajectory {
    private final ArrayDeque<EchoFrame> frames = new ArrayDeque<>();
    private ResourceKey<Level> dimension;

    void add(ResourceKey<Level> currentDimension, EchoFrame frame, int capacity, double discontinuitySqr) {
        EchoFrame previous = frames.peekLast();
        if (dimension != currentDimension || previous != null && previous.position().distanceToSqr(frame.position()) > discontinuitySqr) {
            frames.clear();
        }
        dimension = currentDimension;
        frames.addLast(frame);
        while (frames.size() > capacity) frames.removeFirst();
    }

    List<EchoFrame> delayedFrames(int nowTick, int delayTicks, int historyTicks, int maximum) {
        int end = nowTick - delayTicks;
        int start = end - historyTicks;
        ArrayList<EchoFrame> result = new ArrayList<>();
        for (EchoFrame frame : frames) {
            if (frame.serverTick() >= start && frame.serverTick() <= end) result.add(frame);
        }
        if (result.size() > maximum) return List.copyOf(result.subList(result.size() - maximum, result.size()));
        return List.copyOf(result);
    }

    int size() { return frames.size(); }
    ResourceKey<Level> dimension() { return dimension; }
    void clear() { frames.clear(); }
}
