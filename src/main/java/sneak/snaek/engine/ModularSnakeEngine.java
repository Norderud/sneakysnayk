package sneak.snaek.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.engine.filter.CollisionFilter;
import sneak.snaek.engine.filter.HeadToHeadFilter;
import sneak.snaek.engine.filter.MoveFilter;
import sneak.snaek.engine.scorer.*;
import sneak.snaek.model.BattleSnake;
import sneak.snaek.model.Coord;
import sneak.snaek.model.GameState;
import sneak.snaek.model.Move;

import java.util.*;

/**
 * A modular snake engine that uses a pipeline of filters and scorers to decide the next move.
 */
public class ModularSnakeEngine {
    private static final Logger log = LoggerFactory.getLogger(ModularSnakeEngine.class);
    private static final Logger decisionLog = LoggerFactory.getLogger("decision");
    
    private final List<MoveFilter> filters = new ArrayList<>();
    private final List<Scorer> scorers = new ArrayList<>();
    private Personality personality = Personality.BULLY;

    public ModularSnakeEngine setPersonality(Personality personality) {
        this.personality = personality;
        return this;
    }
    
    public ModularSnakeEngine addFilter(MoveFilter filter) {
        filters.add(filter);
        return this;
    }
    
    public ModularSnakeEngine addScorer(Scorer scorer) {
        scorers.add(scorer);
        return this;
    }
    
    /**
     * Creates a modular engine with the default set of filters and scorers,
     * matching the legacy behavior.
     */
    public static ModularSnakeEngine createDefault() {
        return new ModularSnakeEngine()
                .addFilter(new CollisionFilter())
                .addFilter(new HeadToHeadFilter())
                .addScorer(new SurvivalScorer())
                .addScorer(new FoodScorer())
                .addScorer(new AggressionScorer())
                .addScorer(new TailScorer())
                .addScorer(new PositionScorer());
    }
    
    public Map<Move, Double> scoreMoves(GameState state) {
        TurnContext ctx = TurnContext.from(state, personality);
        Set<Move> moves = new HashSet<>(Arrays.asList(Move.values()));
        
        // 1. Apply filters
        for (MoveFilter filter : filters) {
            filter.filter(ctx, moves);
            if (moves.isEmpty()) break;
        }

        Map<Move, Double> scores = new HashMap<>();
        for (Move move : moves) {
            MoveContext mctx = ctx.createMoveContext(move);
            double totalScore = 0;
            for (Scorer scorer : scorers) {
                totalScore += scorer.score(mctx);
            }
            scores.put(move, totalScore);
        }
        return scores;
    }

    public Move move(GameState state) {
        TurnContext ctx = TurnContext.from(state, personality);
        
        Set<Move> moves = new HashSet<>(Arrays.asList(Move.values()));
        Map<Move, String> filtersApplied = new HashMap<>();

        // 1. Apply filters
        for (MoveFilter filter : filters) {
            Set<Move> before = new HashSet<>(moves);
            filter.filter(ctx, moves);
            for (Move m : before) {
                if (!moves.contains(m)) {
                    filtersApplied.putIfAbsent(m, filter.getClass().getSimpleName());
                }
            }
            if (moves.isEmpty()) {
                log.warn("Filter {} pruned all moves!", filter.getClass().getSimpleName());
                break;
            }
        }
        
        Move bestMove = Move.UP;
        double bestTotalScore = Double.NEGATIVE_INFINITY;
        Map<Move, Double> totalScores = new HashMap<>();
        Map<Move, Map<String, Double>> breakdowns = new HashMap<>();

        if (moves.isEmpty()) {
            log.warn("No safe moves! Returning UP as last resort.");
        } else {
            // 2. Apply scorers
            List<MoveScore> initialScores = new ArrayList<>();
            for (Move move : moves) {
                MoveContext mctx = ctx.createMoveContext(move);
                double totalScore = 0;
                Map<String, Double> breakdown = new LinkedHashMap<>();
                for (Scorer scorer : scorers) {
                    double s = scorer.score(mctx);
                    totalScore += s;
                    breakdown.put(scorer.name(), s);
                }
                totalScores.put(move, totalScore);
                breakdowns.put(move, breakdown);
                initialScores.add(new MoveScore(move, totalScore));
            }

            // 3. Optional 2-ply lookahead
            if (EngineConfig.enableLookahead && initialScores.size() > 1) {
                initialScores.sort(Comparator.comparingDouble(MoveScore::score).reversed());
                int candidatesToCheck = Math.min(initialScores.size(), EngineConfig.lookaheadCandidates);
                int timeLimit = (int) (state.game().timeout() * EngineConfig.timeBudgetRatio);

                for (int i = 0; i < candidatesToCheck; i++) {
                    if (ctx.elapsedMillis() > timeLimit) {
                        log.info("Time limit reached, stopping lookahead ({}ms > {}ms)", ctx.elapsedMillis(), timeLimit);
                        break;
                    }

                    MoveScore ms = initialScores.get(i);
                    Move ourMove = ms.move();
                    double lookaheadScore = evaluateLookahead(state, ourMove, ms.score(), timeLimit, ctx);
                    
                    // Update score with lookahead results.
                    // Scale back to 1-ply range to remain comparable with unchecked moves.
                    totalScores.put(ourMove, lookaheadScore);
                    breakdowns.get(ourMove).put("Lookahead", lookaheadScore - ms.score());
                }
            }

            // Find best move after lookahead
            for (Map.Entry<Move, Double> entry : totalScores.entrySet()) {
                if (entry.getValue() > bestTotalScore) {
                    bestTotalScore = entry.getValue();
                    bestMove = entry.getKey();
                }
            }
        }
        
        // Log decision snapshot
        StringBuilder dsb = new StringBuilder();
        dsb.append(String.format("gameId=%s name=%s personality=%s turn=%d health=%d head=%s ", 
                state.game().id(), state.you().name(), personality, state.turn(), state.you().health(), state.you().head()));
        
        for (Move m : Move.values()) {
            dsb.append(m.name()).append(":[");
            if (filtersApplied.containsKey(m)) {
                dsb.append("FILTERED_BY=").append(filtersApplied.get(m));
            } else if (totalScores.containsKey(m)) {
                dsb.append(String.format("total=%.1f ", totalScores.get(m)));
                Map<String, Double> b = breakdowns.get(m);
                b.forEach((name, val) -> dsb.append(String.format("%s=%.1f ", name, val.doubleValue())));
            } else {
                dsb.append("SKIP");
            }
            dsb.append("] ");
        }
        dsb.append("chosen=").append(bestMove).append(String.format(" (%dms)", ctx.elapsedMillis()));
        decisionLog.info(dsb.toString());

        log.info("[{}:{}] Turn {} | Head={} Health={} | Move={} ({}ms)",
                state.game().id(), state.you().name(), state.turn(), state.you().head(), state.you().health(),
                bestMove, ctx.elapsedMillis());
        
        return bestMove;
    }

    private double evaluateLookahead(GameState state, Move ourMove, double currentScore, int timeLimit, TurnContext ctx) {
        List<BattleSnake> enemies = ctx.enemies();
        
        if (enemies.isEmpty()) return currentScore;

        Map<String, Move> moves = new HashMap<>();
        moves.put(state.you().id(), ourMove);
        
        for (BattleSnake enemy : enemies) {
            Move enemyMove = predictEnemyMove(state, enemy, ctx);
            moves.put(enemy.id(), enemyMove);
        }

        GameState nextState = sneak.snaek.sim.Simulator.step(state, moves);
        if (nextState.you() == null) {
            return -1000000.0; // Death
        }

        Map<Move, Double> nextScores = scoreMoves(nextState);
        if (nextScores.isEmpty()) {
            return -500000.0; // Trap
        }
        
        double bestNextScore = nextScores.values().stream().max(Double::compare).orElse(0.0);
        
        // current + discounted future, normalized to 1-ply scale
        return (currentScore + 0.9 * bestNextScore) / 1.9;
    }

    private double totalScoreForMove(GameState state, Move move) {
        TurnContext ctx = TurnContext.from(state, personality);
        MoveContext mctx = ctx.createMoveContext(move);
        double total = 0;
        for (Scorer s : scorers) total += s.score(mctx);
        return total;
    }

    private double scoreOnePly(GameState state, Move move) {
        return totalScoreForMove(state, move);
    }

    private Move predictEnemyMove(GameState state, BattleSnake enemy, TurnContext ctx) {
        Set<Move> moves = new HashSet<>(Arrays.asList(Move.values()));
        Coord head = enemy.head();
        moves.removeIf(m -> ctx.grid().isBlocked(CoordUtils.neighbor(head, m)));
        
        if (moves.isEmpty()) return Move.UP;
        if (moves.size() == 1) return moves.iterator().next();

        // Heuristic: stay away from our head if we are longer (avoid H2H death)
        // and stay closer to center.
        Move best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        Coord center = new Coord(state.board().width() / 2, state.board().height() / 2);
        Coord ourHead = state.you().head();
        boolean weAreLonger = state.you().length() >= enemy.length();

        for (Move m : moves) {
            Coord n = CoordUtils.neighbor(head, m);
            double score = 0;
            
            // Distance to center (Chebyshev)
            int distCenter = Math.max(Math.abs(n.x() - center.x()), Math.abs(n.y() - center.y()));
            score -= distCenter * 10;
            
            // Avoid our head if we are dangerous
            if (weAreLonger) {
                int distUs = CoordUtils.manhattanDistance(n, ourHead);
                if (distUs <= 1) score -= 1000; // Very dangerous
                else if (distUs == 2) score -= 100; // Getting close
            }

            if (best == null || score > bestScore) {
                bestScore = score;
                best = m;
            }
        }
        return best;
    }

    private record MoveScore(Move move, double score) {}
}
