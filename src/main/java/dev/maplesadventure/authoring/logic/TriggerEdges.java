package dev.maplesadventure.authoring.logic;
import java.util.*;
/** Separate gameplay presence from authoring sessions. Clearing does not emit a synthetic exit. */
public final class TriggerEdges<T> {
    public record Change<T>(Set<T> entered,Set<T> exited){}
    private final Map<UUID,Set<T>> inside=new HashMap<>();
    public Change<T> update(UUID player,Set<T> current){var previous=inside.getOrDefault(player,Set.of());var entered=new LinkedHashSet<>(current);entered.removeAll(previous);
        var exited=new LinkedHashSet<>(previous);exited.removeAll(current);if(current.isEmpty())inside.remove(player);else inside.put(player,Set.copyOf(current));return new Change<>(entered,exited);}
    public void forget(UUID player){inside.remove(player);}
    public void retain(Set<T> keys){inside.replaceAll((id,old)->{var next=new HashSet<>(old);next.retainAll(keys);return Set.copyOf(next);});}
    public void clear(){inside.clear();}
}
