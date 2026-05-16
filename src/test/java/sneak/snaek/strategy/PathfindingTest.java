package sneak.snaek.strategy;

import org.junit.jupiter.api.Test;
import sneak.snaek.board.BoardGrid;
import sneak.snaek.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PathfindingTest {

    @Test
    public void testAlgorithms() {
        // Simple 5x5 board
        // S . . . .
        // # # # . .
        // . . . . .
        // . . # # #
        // . . . . G
        
        Coord head = new Coord(0, 4); // Start at top left
        List<Coord> body = List.of(head, new Coord(0, 3), new Coord(0, 2));
        BattleSnake you = new BattleSnake("you", "you", 100, body, "0", head, 3, "", "", null);
        
        // Blockers at (0,3), (1,3), (2,3) - wait, body is at (0,3), (0,2)
        // Let's make a clear wall
        List<Coord> wall = List.of(new Coord(0, 2), new Coord(1, 2), new Coord(2, 2));
        BattleSnake enemy = new BattleSnake("enemy", "enemy", 100, wall, "0", new Coord(0, 2), 3, "", "", null);

        Board board = new Board(5, 5, List.of(), List.of(you, enemy), List.of());
        GameState state = new GameState(null, 1, board, you);
        BoardGrid grid = new BoardGrid(state);
        
        Coord goal = new Coord(4, 0); // Bottom right
        
        // 1. BFS
        int[][] distMap = Bfs.from(grid, List.of(head));
        int bfsDist = distMap[goal.x()][goal.y()];
        assertTrue(bfsDist < Bfs.UNREACHABLE, "BFS should find a path");
        
        // 2. A*
        List<Coord> aStarPath = AStar.findPath(grid, head, goal);
        assertFalse(aStarPath.isEmpty(), "A* should find a path");
        assertEquals(bfsDist, aStarPath.size(), "A* path length should match BFS distance");
        
        // 3. DFS
        List<Coord> dfsPath = Dfs.findAnyPath(grid, head, goal);
        assertFalse(dfsPath.isEmpty(), "DFS should find a path");
        assertTrue(dfsPath.size() >= aStarPath.size(), "DFS path length should be >= shortest path");
        
        System.out.println("BFS distance: " + bfsDist);
        System.out.println("A* path size: " + aStarPath.size());
        System.out.println("DFS path size: " + dfsPath.size());
    }
}
