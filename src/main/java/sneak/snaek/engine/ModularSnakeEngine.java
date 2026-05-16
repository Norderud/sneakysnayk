package sneak.snaek.engine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sneak.snaek.engine.filter.CollisionFilter;
import sneak.snaek.engine.filter.HeadToHeadFilter;
import sneak.snaek.engine.filter.MoveFilter;
import sneak.snaek.engine.scorer.*;
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
                .addScorer(new GatekeeperScorer())
                .addScorer(new TailScorer())
                .addScorer(new PositionScorer());
    }
    
    public Move move(GameState state) {
        TurnContext ctx = TurnContext.from(state);
        
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
                
                if (log.isDebugEnabled()) {
                    log.debug("  candidate {} -> total={}", move, String.format("%.1f", totalScore));
                }
                
                if (totalScore > bestTotalScore) {
                    bestTotalScore = totalScore;
                    bestMove = move;
                }
            }
        }
        
        // Log decision snapshot
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("gameId=%s name=%s turn=%d health=%d head=%s ", 
                state.game().id(), state.you().name(), state.turn(), state.you().health(), state.you().head()));
        
        for (Move m : Move.values()) {
            sb.append(m.name()).append(":[");
            if (filtersApplied.containsKey(m)) {
                sb.append("FILTERED_BY=").append(filtersApplied.get(m));
            } else if (totalScores.containsKey(m)) {
                sb.append(String.format("total=%.1f ", totalScores.get(m)));
                Map<String, Double> b = breakdowns.get(m);
                b.forEach((name, val) -> sb.append(String.format("%s=%.1f ", name, val.doubleValue())));
            } else {
                sb.append("SKIP");
            }
            sb.append("] ");
        }
        sb.append("chosen=").append(bestMove).append(String.format(" (%dms)", ctx.elapsedMillis()));
        decisionLog.info(sb.toString());

        log.info("[{}:{}] Turn {} | Head={} Health={} | Move={} ({}ms)",
                state.game().id(), state.you().name(), state.turn(), state.you().head(), state.you().health(),
                bestMove, ctx.elapsedMillis());
        
        return bestMove;
    }
}
