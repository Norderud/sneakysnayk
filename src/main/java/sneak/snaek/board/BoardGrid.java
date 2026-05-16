package sneak.snaek.board;

import sneak.snaek.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BoardGrid {

    private final int width;
    private final int height;
    private final boolean[][] blocked;   // [x][y] — snake bodies (tail-free)
    private final boolean[][] hazard;    // [x][y] — hazard zones
    private final Set<Coord> food;

    // -----------------------------------------------------------------------
    // Construction
    // -----------------------------------------------------------------------

    public BoardGrid(GameState state) {
        this(state.board());
    }

    public BoardGrid(Board board) {
        this.width   = board.width();
        this.height  = board.height();
        this.blocked = new boolean[width][height];
        this.hazard  = new boolean[width][height];
        this.food    = new HashSet<>(board.food()    != null ? board.food()    : List.of());

        if (board.hazards() != null) {
            for (Coord h : board.hazards()) {
                if (inBounds(h)) hazard[h.x()][h.y()] = true;
            }
        }
        for (BattleSnake snake : board.snakes()) {
            markSnakeBody(snake.body());
        }
    }

    
    public BoardGrid(int width, int height,
                     Set<Coord> blockedCells,
                     Set<Coord> hazardCells,
                     Set<Coord> food) {
        this.width   = width;
        this.height  = height;
        this.blocked = new boolean[width][height];
        this.hazard  = new boolean[width][height];
        this.food    = new HashSet<>(food);
        for (Coord c : blockedCells) if (inBounds(c)) blocked[c.x()][c.y()] = true;
        for (Coord c : hazardCells)  if (inBounds(c)) hazard[c.x()][c.y()]  = true;
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    
    private void markSnakeBody(List<Coord> body) {
        if (body == null || body.isEmpty()) return;
        int last = body.size() - 1;
        boolean tailVacates = last > 0 && !body.get(last - 1).equals(body.get(last));
        for (int i = 0; i <= last; i++) {
            if (tailVacates && i == last) continue;   // tail will be gone next turn
            Coord c = body.get(i);
            if (inBounds(c)) blocked[c.x()][c.y()] = true;
        }
    }

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    
    public boolean inBounds(Coord c) {
        return c.x() >= 0 && c.x() < width
            && c.y() >= 0 && c.y() < height;
    }

    
    public boolean isBlocked(Coord c) {
        return !inBounds(c) || blocked[c.x()][c.y()];
    }

    public boolean isBlocked(int x, int y) {
        return x < 0 || x >= width || y < 0 || y >= height || blocked[x][y];
    }

    
    public boolean isHazard(Coord c) {
        return inBounds(c) && hazard[c.x()][c.y()];
    }

    public boolean isHazard(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height && hazard[x][y];
    }

    
    public boolean hasFood(Coord c) {
        return food.contains(c);
    }

    public Set<Coord> getFood()  { return food; }
    public int        getWidth() { return width; }
    public int        getHeight(){ return height; }
}

