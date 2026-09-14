package dev.maplesadventure.multiplayer.encounter.boss;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Server-runtime index only. Persistent authority remains entity attachments plus BossAttemptState. */
public final class BossAttemptEntityIndex {
    private static final Map<UUID, Members> ATTEMPTS = new HashMap<>();

    public static void track(UUID entityId, BossEntityLinkState link) {
        if (!link.valid()) return;
        Members members = ATTEMPTS.computeIfAbsent(link.attemptId(), ignored -> new Members());
        members.remove(entityId);
        switch (link.role()) {
            case PRIMARY -> members.primary = entityId;
            case ADD -> members.adds.add(entityId);
            case CHILD -> members.children.add(entityId);
            case PROJECTILE, EFFECT, PART -> members.transients.add(entityId);
        }
    }

    public static void untrack(UUID entityId, BossEntityLinkState link) {
        Members members = ATTEMPTS.get(link.attemptId());
        if (members == null) return;
        members.remove(entityId);
        if (members.empty()) ATTEMPTS.remove(link.attemptId());
    }

    public static Set<UUID> members(UUID attemptId) {
        Members members = ATTEMPTS.get(attemptId);
        if (members == null) return Set.of();
        LinkedHashSet<UUID> result = new LinkedHashSet<>();
        if (members.primary != null) result.add(members.primary);
        result.addAll(members.adds);
        result.addAll(members.children);
        result.addAll(members.transients);
        return Set.copyOf(result);
    }

    public static void clearAttempt(UUID attemptId) { ATTEMPTS.remove(attemptId); }
    public static void clear() { ATTEMPTS.clear(); }

    private static final class Members {
        private UUID primary;
        private final Set<UUID> adds = new LinkedHashSet<>();
        private final Set<UUID> children = new LinkedHashSet<>();
        private final Set<UUID> transients = new LinkedHashSet<>();

        private void remove(UUID id) {
            if (id.equals(primary)) primary = null;
            adds.remove(id);
            children.remove(id);
            transients.remove(id);
        }
        private boolean empty() {
            return primary == null && adds.isEmpty() && children.isEmpty() && transients.isEmpty();
        }
    }

    private BossAttemptEntityIndex() {}
}
