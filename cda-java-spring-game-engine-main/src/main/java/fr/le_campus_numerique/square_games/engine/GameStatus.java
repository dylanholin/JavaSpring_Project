package fr.le_campus_numerique.square_games.engine;

public enum GameStatus {
    /**
     * The game is in a setup phase.
     * A game such as Battleship starts with a setup phase during which players place their tokens manually
     * on the board before actually playing.
     */
    SETUP,
    /**
     * The game is ongoing, at least one token can be moved.
     */
    ONGOING,
    /**
     * The game is over.
     * The identifier of the {@link Game#getCurrentPlayerId() current player} indicates the winner.
     * If the game is a draw, the current player identifier is empty.
     */
    TERMINATED
}
