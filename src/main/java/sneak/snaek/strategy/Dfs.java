package sneak.snaek.strategy;

import sneak.snaek.board.BoardGrid;
import sneak.snaek.board.CoordUtils;
import sneak.snaek.model.Coord;

import java.util.*;

/**
 * Depth-first search utilities.
 * 
 * DFS is generally NOT used for shortest pathfinding in BattleSnake,
 * but it can be useful for finding ANY path to a target in a large
 * space or for exploring long, winding paths.
 */
public final class Dfs {

    private Dfs() {}

    /**
     * Finds a path from start to goal using DFS.
     * Note: This is NOT guaranteed to be the shortest path.
     */
    public static List<Coord> findAnyPath(BoardGrid grid, Coord start, Coord goal) {
        if (start.equals(goal)) return List.of();
        
        Set<Coord> visited = new HashSet<>();
        List<Coord> path = new ArrayList<>();
        
        if (search(grid, start, goal, visited, path)) {
            return path;
        }
        return List.of();
    }

    private static boolean search(BoardGrid grid, Coord current, Coord goal, 
                                  Set<Coord> visited, List<Coord> path) {
        if (current.equals(goal)) return true;
        
        visited.add(current);
        
        // Shuffle neighbors to explore different paths each time (optional)
        List<Coord> neighbors = new ArrayList<>(CoordUtils.neighbors(current));
        // Collections.shuffle(neighbors); 

        for (Coord n : neighbors) {
            if (!grid.inBounds(n) || grid.isBlocked(n) || visited.contains(n)) continue;
            
            path.add(n);
            if (search(grid, n, goal, visited, path)) return true;
            path.removeLast();
        }
        
        return false;
    }
}
