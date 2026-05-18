package sneak.snaek.sim;

import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.*;

import java.util.*;

/**
 * A simple simulator to project game state forward.
 * Note: This is a simplified version of Battlesnake rules.
 * It handles movement, basic collisions, and food consumption.
 */
public class Simulator {

    public static GameState step(GameState state, Map<String, Move> moves) {
        Board oldBoard = state.board();
        List<BattleSnake> nextSnakes = new ArrayList<>();
        Set<Coord> newFood = new HashSet<>(oldBoard.food());

        // 1. Determine new heads and preliminary body states
        Map<String, Coord> newHeads = new HashMap<>();
        for (BattleSnake snake : oldBoard.snakes()) {
            Move move = moves.get(snake.id());
            if (move == null) {
                // If no move provided, assume it's dead or moves forward if possible?
                // For simplicity, if no move is provided for an enemy, they are treated as dead in this step?
                // Actually, for 2-ply, we will likely only move one or two snakes.
                // Let's assume snakes that don't move stay put (which is actually death in Battlesnake if they hit themselves).
                // Better: if move is null, the snake is eliminated.
                continue;
            }
            newHeads.put(snake.id(), CoordUtils.neighbor(snake.head(), move));
        }

        // 2. Move snakes
        for (BattleSnake snake : oldBoard.snakes()) {
            Coord nextHead = newHeads.get(snake.id());
            if (nextHead == null) continue;

            boolean ate = oldBoard.food().contains(nextHead);
            List<Coord> newBody = new ArrayList<>();
            newBody.add(nextHead);
            
            // Add existing body parts
            // If ate, length increases, so we keep the tail.
            // If not ate, tail is removed.
            int segmentsToKeep = ate ? snake.body().size() : snake.body().size() - 1;
            for (int i = 0; i < segmentsToKeep; i++) {
                newBody.add(snake.body().get(i));
            }

            int newHealth = ate ? 100 : snake.health() - 1;
            // Handle hazard damage
            if (state.board().hazards() != null && state.board().hazards().contains(nextHead)) {
                int hazardDamage = 15; // Default standard
                if (state.game().ruleset() != null && state.game().ruleset().settings() != null) {
                    hazardDamage = state.game().ruleset().settings().hazardDamagePerTurn();
                }
                newHealth -= hazardDamage;
            }

            if (newHealth <= 0) continue; // Starved or hazard-killed

            BattleSnake nextSnake = new BattleSnake(
                    snake.id(), snake.name(), newHealth, newBody,
                    snake.latency(), nextHead, newBody.size(),
                    snake.shout(), snake.squad(), snake.customizations()
            );
            nextSnakes.add(nextSnake);
            
            if (ate) {
                newFood.remove(nextHead);
            }
        }

        // 3. Handle Collisions
        // OOB
        nextSnakes.removeIf(s -> s.head().x() < 0 || s.head().x() >= oldBoard.width() ||
                                s.head().y() < 0 || s.head().y() >= oldBoard.height());

        // Body collisions (hitting any segment except a head)
        List<BattleSnake> survivors = new ArrayList<>();
        for (BattleSnake s : nextSnakes) {
            boolean collided = false;
            for (BattleSnake other : nextSnakes) {
                List<Coord> otherBody = other.body();
                // Skip the head (index 0) of the other snake. 
                // Head-to-head collisions are handled in the next step.
                for (int i = 1; i < otherBody.size(); i++) {
                    if (s.head().equals(otherBody.get(i))) {
                        collided = true;
                        break;
                    }
                }
                if (collided) break;
            }
            if (!collided) survivors.add(s);
        }
        
        // Head-to-head collisions
        Set<String> toRemove = new HashSet<>();
        for (int i = 0; i < survivors.size(); i++) {
            for (int j = i + 1; j < survivors.size(); j++) {
                BattleSnake s1 = survivors.get(i);
                BattleSnake s2 = survivors.get(j);
                if (s1.head().equals(s2.head())) {
                    if (s1.length() <= s2.length()) toRemove.add(s1.id());
                    if (s2.length() <= s1.length()) toRemove.add(s2.id());
                }
            }
        }
        survivors.removeIf(s -> toRemove.contains(s.id()));

        Board nextBoard = new Board(oldBoard.height(), oldBoard.width(), 
                new ArrayList<>(newFood), survivors, oldBoard.hazards());
        
        BattleSnake nextYou = survivors.stream()
                .filter(s -> s.id().equals(state.you().id()))
                .findFirst()
                .orElse(null);

        return new GameState(state.game(), state.turn() + 1, nextBoard, nextYou);
    }
}
