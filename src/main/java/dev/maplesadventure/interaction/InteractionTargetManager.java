package dev.maplesadventure.interaction;

import dev.maplesadventure.config.InteractionConfig;
import dev.maplesadventure.interaction.provider.InteractionTargetProvider;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

public final class InteractionTargetManager {
    private static final InteractionTargetManager INSTANCE = new InteractionTargetManager();

    private final InteractionRegistry registry = InteractionRegistry.getInstance();
    private final InteractionTargetValidator validator = new InteractionTargetValidator(registry);
    private final InteractionTargetScanner scanner = new InteractionTargetScanner(registry, validator);
    private List<InteractionCandidate> candidates = Collections.emptyList();
    private @Nullable InteractionCandidate currentCandidate;
    private @Nullable ClientLevel observedLevel;
    private @Nullable LocalPlayer observedPlayer;
    private int ticksUntilScan;
    private String lastValidation = "idle";

    public static InteractionTargetManager getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (!InteractionConfig.INTERACTION_ENABLED.get() || player == null || level == null || !player.isAlive()) {
            clear();
            return;
        }
        ensureWorld(level, player);

        if (currentCandidate != null && !isCandidateStillDiscoverable(level, player, currentCandidate)) {
            currentCandidate = null;
            ticksUntilScan = 0;
        }
        if (ticksUntilScan > 0) {
            ticksUntilScan--;
            return;
        }
        refresh(level, player);
        ticksUntilScan = Math.max(0, InteractionConfig.SCAN_INTERVAL.get() - 1);
    }

    /** Forces the same bounded scan used by the scheduled tick path. */
    public void refreshCandidatesNow() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (!InteractionConfig.INTERACTION_ENABLED.get() || player == null || level == null || !player.isAlive()) {
            clear();
            return;
        }
        ensureWorld(level, player);
        refresh(level, player);
        ticksUntilScan = Math.max(0, InteractionConfig.SCAN_INTERVAL.get() - 1);
    }

    /** Revalidates the cached target at key-down time and scans immediately when it is absent or stale. */
    public PreparedInteraction prepareInteraction() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        ClientLevel level = minecraft.level;
        if (!InteractionConfig.INTERACTION_ENABLED.get() || player == null || level == null || !player.isAlive()) {
            return PreparedInteraction.failure(InteractionTargetValidator.FailureReason.TARGET_REMOVED);
        }
        ensureWorld(level, player);

        InteractionCandidate staleCandidate = currentCandidate;
        InteractionTargetValidator.ValidationResult staleValidation = validateCandidate(
                level,
                player,
                staleCandidate,
                InteractionTargetValidator.ValidationMode.EXECUTE
        );
        if (staleCandidate != null && staleValidation.valid()) {
            lastValidation = "execution_revalidated";
            return new PreparedInteraction(staleCandidate, staleCandidate.provider(), staleValidation);
        }

        refresh(level, player);
        ticksUntilScan = Math.max(0, InteractionConfig.SCAN_INTERVAL.get() - 1);
        InteractionCandidate refreshedCandidate = currentCandidate;
        InteractionTargetValidator.ValidationResult refreshedValidation = validateCandidate(
                level,
                player,
                refreshedCandidate,
                InteractionTargetValidator.ValidationMode.EXECUTE
        );
        if (refreshedCandidate != null && refreshedValidation.valid()) {
            lastValidation = "execution_refreshed";
            return new PreparedInteraction(refreshedCandidate, refreshedCandidate.provider(), refreshedValidation);
        }

        InteractionTargetValidator.ValidationResult failure = refreshedCandidate != null
                ? refreshedValidation
                : staleValidation;
        lastValidation = failure.failureReason().name();
        return new PreparedInteraction(
                refreshedCandidate != null ? refreshedCandidate : staleCandidate,
                refreshedCandidate != null ? refreshedCandidate.provider() : staleCandidate == null ? null : staleCandidate.provider(),
                failure
        );
    }

    public void cycleTarget() {
        if (candidates.size() < 2 || currentCandidate == null) {
            return;
        }
        int currentIndex = indexOf(currentCandidate.target());
        currentCandidate = candidates.get((currentIndex + 1) % candidates.size());
        lastValidation = "selected_by_player";
    }

    public @Nullable InteractionCandidate getCurrentCandidate() {
        return currentCandidate;
    }

    public List<InteractionCandidate> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    public int getCandidateCount() {
        return candidates.size();
    }

    public String getLastValidation() {
        return lastValidation;
    }

    public void setLastValidation(String lastValidation) {
        this.lastValidation = lastValidation;
    }

    public void clear() {
        scanner.clear();
        candidates = Collections.emptyList();
        currentCandidate = null;
        observedLevel = null;
        observedPlayer = null;
        ticksUntilScan = 0;
        lastValidation = "cleared";
    }

    private void ensureWorld(ClientLevel level, LocalPlayer player) {
        if (observedLevel != level || observedPlayer != player) {
            clear();
            observedLevel = level;
            observedPlayer = player;
        }
    }

    private void refresh(ClientLevel level, LocalPlayer player) {
        InteractionTarget previous = currentCandidate == null ? null : currentCandidate.target();
        candidates = scanner.scan(level, player);
        if (candidates.isEmpty()) {
            currentCandidate = null;
            lastValidation = "no_candidates";
            return;
        }

        int previousIndex = previous == null ? -1 : indexOf(previous);
        if (previousIndex >= 0 && InteractionConfig.TARGET_STICKINESS.get()) {
            currentCandidate = candidates.get(previousIndex);
            lastValidation = "valid_sticky_target";
        } else {
            currentCandidate = candidates.get(0);
            lastValidation = previousIndex >= 0 ? "rescored" : "selected_best_candidate";
        }
    }

    private int indexOf(InteractionTarget target) {
        for (int index = 0; index < candidates.size(); index++) {
            if (candidates.get(index).sameTarget(target)) {
                return index;
            }
        }
        return -1;
    }

    private boolean isCandidateStillDiscoverable(
            ClientLevel level,
            LocalPlayer player,
            InteractionCandidate candidate
    ) {
        InteractionTargetProvider provider = registry.findProvider(level, player, candidate.target());
        InteractionTargetValidator.ValidationResult validation = validator.validate(
                level,
                player,
                candidate.target(),
                provider,
                InteractionTargetValidator.ValidationMode.RELEASE
        );
        lastValidation = validation.valid() ? "valid_release_target" : validation.failureReason().name();
        return validation.valid();
    }

    private InteractionTargetValidator.ValidationResult validateCandidate(
            ClientLevel level,
            LocalPlayer player,
            @Nullable InteractionCandidate candidate,
            InteractionTargetValidator.ValidationMode mode
    ) {
        if (candidate == null) {
            return InteractionTargetValidator.ValidationResult.failure(InteractionTargetValidator.FailureReason.NO_PROVIDER);
        }
        InteractionTargetProvider provider = registry.findProvider(level, player, candidate.target());
        return validator.validate(level, player, candidate.target(), provider, mode);
    }

    public record PreparedInteraction(
            @Nullable InteractionCandidate candidate,
            @Nullable InteractionTargetProvider provider,
            InteractionTargetValidator.ValidationResult validation
    ) {
        static PreparedInteraction failure(InteractionTargetValidator.FailureReason reason) {
            return new PreparedInteraction(null, null, InteractionTargetValidator.ValidationResult.failure(reason));
        }
    }

    private InteractionTargetManager() {
    }
}
