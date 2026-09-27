package dev.maplesadventure.integration.soulscombathud;

import dev.maplesadventure.api.flask.FlaskKind;
import dev.maplesadventure.api.flask.FlaskSnapshot;

/** Pure selection/display rules, independent of the client's held item and inventory scans. */
public final class FlaskHudPolicy {
    public static boolean enabled(FlaskKind selectedKind, boolean mana, boolean blacklisted) {
        return selectedKind != null && !blacklisted && (selectedKind != FlaskKind.ASHEN || mana);
    }
    public static String label(FlaskSnapshot state, FlaskKind selectedKind) {
        return state.remaining(selectedKind) + " / " + state.allocated(selectedKind);
    }
    private FlaskHudPolicy() {}
}
