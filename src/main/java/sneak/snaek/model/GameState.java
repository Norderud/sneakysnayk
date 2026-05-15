package sneak.snaek.model;

public record GameState(Game game, int turn, Board board, BattleSnake you) {
}
