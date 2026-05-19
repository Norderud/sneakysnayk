package sneak.snaek.engine;

import sneak.snaek.model.GameState;
import sneak.snaek.model.Ruleset;

/**
 * Detects the {@link GameMode} for a freshly-started game from the
 * {@code /start} payload. Detection runs <em>once per game</em> and the
 * result is stored alongside the engine in
 * {@code FierceBattleSnakeApplication}.
 *
 * <p>The mapping is intentionally permissive — a missing or unknown
 * ruleset name falls back to a snake-count-based heuristic and finally
 * to {@link GameMode#STANDARD}, so we never refuse to play.
 */
public final class ModeDetector {

    private ModeDetector() {}

    public static GameMode detect(GameState state) {
        if (state == null || state.board() == null) return GameMode.UNKNOWN;

        int snakes = state.board().snakes() == null ? 0 : state.board().snakes().size();
        Ruleset ruleset = state.game() != null ? state.game().ruleset() : null;
        String name = ruleset != null && ruleset.name() != null
                ? ruleset.name().toLowerCase()
                : "";

        return switch (name) {
            case "constrictor"          -> GameMode.CONSTRICTOR;
            case "wrapped-constrictor"  -> GameMode.WRAPPED_CONSTRICTOR;
            case "wrapped"              -> GameMode.WRAPPED;
            case "royale"               -> GameMode.ROYALE;
            case "solo"                 -> GameMode.SOLO;
            case "standard", ""         -> classifyStandard(snakes);
            default                     -> GameMode.UNKNOWN;
        };
    }

    private static GameMode classifyStandard(int snakes) {
        if (snakes <= 1) return GameMode.SOLO;
        if (snakes == 2) return GameMode.DUEL;
        return GameMode.STANDARD;
    }
}
