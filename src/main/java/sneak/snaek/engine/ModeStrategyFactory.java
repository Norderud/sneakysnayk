package sneak.snaek.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sneak.snaek.engine.filter.CollisionFilter;
import sneak.snaek.engine.filter.HeadToHeadFilter;
import sneak.snaek.engine.scorer.*;

/**
 * Builds a {@link ModularSnakeEngine} tuned for a specific {@link GameMode}.
 *
 * <p>Sits one layer above {@link PersonalityEngineFactory}: a mode picks a
 * base {@link Personality} plus mode-specific scorer overrides and
 * {@link EngineConfig} tweaks. Wrapped variants fall back to their
 * non-wrapped counterpart with a warning (no toroidal BFS yet).
 */
public final class ModeStrategyFactory {

    private static final Logger log = LoggerFactory.getLogger(ModeStrategyFactory.class);

    private ModeStrategyFactory() {}

    public static ModularSnakeEngine engineFor(GameMode mode) {
        return switch (mode) {
            case DUEL                 -> buildDuel();
            case ROYALE               -> buildRoyale();
            case CONSTRICTOR          -> buildConstrictor();
            case WRAPPED              -> wrappedFallback(GameMode.WRAPPED, buildStandard());
            case WRAPPED_CONSTRICTOR  -> wrappedFallback(GameMode.WRAPPED_CONSTRICTOR, buildConstrictor());
            case SOLO                 -> buildSolo();
            case STANDARD, UNKNOWN    -> buildStandard();
        };
    }

    // -- per-mode builders ---------------------------------------------------

    private static ModularSnakeEngine buildStandard() {
        // STANDARD = current default BULLY behavior, validates the refactor.
        return PersonalityEngineFactory.create(Personality.BULLY)
                .setMode(GameMode.STANDARD)
                .setConfig(new EngineConfig());
    }

    private static ModularSnakeEngine buildSolo() {
        // No enemies → MIDAS (food focus) is the right shape. Lookahead
        // still useful for self-avoidance in tight pockets.
        return PersonalityEngineFactory.create(Personality.MIDAS)
                .setMode(GameMode.SOLO)
                .setConfig(new EngineConfig());
    }

    private static ModularSnakeEngine buildDuel() {
        // 1v1 length race. DUELIST personality (relaxed H2H, symmetric Voronoi)
        // plus paranoid lookahead — only one opponent, so enumerating all 3
        // enemy moves fits the time budget easily.
        // TODO(duel): once DUEL phase lands, raise Aggression/Stretch weights
        //             in PersonalityEngineFactory.DUELIST or compose directly here.
        EngineConfig cfg = new EngineConfig().with(c -> {
            c.lookaheadCandidates = 4;
            c.enemyMovesPerCandidate = 3;
        });
        return PersonalityEngineFactory.create(Personality.DUELIST)
                .setMode(GameMode.DUEL)
                .setConfig(cfg);
    }

    private static ModularSnakeEngine buildRoyale() {
        // BULLY base + ShrinkZoneScorer to retreat from upcoming hazard rings,
        // double-weighted hazard penalty (anticipate rather than react), and
        // halved aggression (opponents often die to hazard on their own).
        return new ModularSnakeEngine()
                .setPersonality(Personality.BULLY)
                .setMode(GameMode.ROYALE)
                .setConfig(new EngineConfig())
                .addFilter(new CollisionFilter())
                .addFilter(new HeadToHeadFilter())
                .addScorer(new SurvivalScorer())
                .addScorer(new FoodScorer())
                .addScorer(new AggressionScorer(0.5))
                .addScorer(new TailScorer())
                .addScorer(new PositionScorer())
                .addScorer(new ShrinkZoneScorer(2.0));   // double the anticipation pressure
    }

    private static ModularSnakeEngine buildConstrictor() {
        // No food spawns ⇒ FoodScorer omitted entirely.
        // Every move grows ⇒ CompactnessScorer dominates the "don't coil into
        // yourself" decision. Survival doubled because area control is the
        // entire game.
        EngineConfig cfg = new EngineConfig().with(c -> c.lookaheadCandidates = 6);
        return new ModularSnakeEngine()
                .setPersonality(Personality.TURTLE)
                .setMode(GameMode.CONSTRICTOR)
                .setConfig(cfg)
                .addFilter(new CollisionFilter())
                .addFilter(new HeadToHeadFilter())
                .addScorer(new SurvivalScorer(2.0))
                .addScorer(new AggressionScorer(1.0))
                .addScorer(new TailScorer(1.0))
                .addScorer(new PositionScorer())
                .addScorer(new CompactnessScorer());
        // NOTE: FoodScorer intentionally omitted — constrictor has no food.
    }

    private static ModularSnakeEngine wrappedFallback(GameMode mode, ModularSnakeEngine fallback) {
        log.warn("Mode {} detected — using fallback engine (no toroidal BFS yet). "
                + "TODO: wrap-aware BoardGrid.inBounds + Bfs + CoordUtils.neighbor.", mode);
        return fallback.setMode(mode);
    }
}
