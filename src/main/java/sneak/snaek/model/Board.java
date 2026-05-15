package sneak.snaek.model;

import java.util.List;

public record Board(int height, int width, List<Coord> food, List<BattleSnake> snakes, List<Coord> hazards) {
}
