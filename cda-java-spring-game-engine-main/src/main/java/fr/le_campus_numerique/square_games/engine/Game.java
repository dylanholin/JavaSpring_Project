package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.*;
import java.util.*;

/**
 * Represents the state of an active game.
 * <p>
 * The only way to alter the state of a game is to move one of its {@link Token}.
 * A game involves one or more players, each identified with an {@link UUID} only.
 */
public interface Game {

    /**
     * Returns the identifier of this game.
     *
     * @return The identifier of this game; it cannot be {@code null}.
     */
    @NotNull UUID getId();

    /**
     * Returns the string identifier of the {@link GameFactory} that created this game.
     *
     * @return The identifier of the {@link GameFactory} used to create this game; it cannot be {@code null} or blank.
     * @see GameFactory#getGameFactoryId()
     */
    @NotBlank String getFactoryId();

    /**
     * Returns the identifiers of the players taking part in this game.
     * The collection must not change during the lifetime of this game.
     * <p>
     * <p>The order of the elements in the returned set is meaningful: it follows the sequence of player turns</p>
     * A player is identified by a {@link UUID} only and there is no additional model for a player.
     * Relying only on a {@code UUID} is enough to:
     * <ul>
     *     <li>expose a list players</li>
     *     <li>indicate the current player</li>
     *     <li>link a {@link Token} to its owner</li>
     * </ul>
     * </p>
     *
     * @return a set containing the identifiers of each player; it cannot be {@code null} or empty
     * @see #getCurrentPlayerId()
     * @see Token#getOwnerId()
     */
    @NotEmpty Set<UUID> getPlayerIds();

    /**
     * Gets the current status of this game (setup, ongoing or terminated).
     *
     * @return a {@link GameStatus} value representing the current status of this game; it cannot be {@code null}
     */
    @NotNull GameStatus getStatus();

    /**
     * Gets the identifier of the currently active player.
     * If the status of this game is {@link GameStatus#TERMINATED}, returns the identifier of the winner.
     * <p>
     * It is a possible that there is no active player, for example if this game terminated with a draw.
     * </p>
     *
     * @return the identifier of the active player, or {@code null} if there are no active player.
     */
    UUID getCurrentPlayerId();

    /**
     * Gets the size of the side of the (square) board used in this game.
     *
     * @return a value greater than 1 representing the size of the board side
     */
    @Min(2) int getBoardSize();

    /**
     * Gets all the {@link Token} currently on the board, keyed by their position on the board.
     * <p>
     * The board is a {@code Map} keyed by {@link CellPosition}, so it is not possible to have several tokens
     * at the same positions. Also, board cells containing no token are not returned in the {@code Map}
     * returned by this property.
     * </p>
     *
     * @return a {@code Map} containing all tokens currently placed on the board, keyed by their position;
     * it cannot be {@code null}
     */
    @NotNull Map<CellPosition, Token> getBoard();

    /**
     * Gets the collection of tokens that are yet-to-be placed on the board by a player.
     *
     * <p>
     * Depending on the game being played, tokens considered as 'remaining' can for example be:
     *     <ul>
     *         <li>tokens during a manual setup phase such as in the Battleship game</li>
     *         <li>predefined stock a tokens such as in Connect Four</li>
     *     </ul>
     * </p>
     *
     * @return a collection of {@link Token}s considered as candidates to be moved to the board;
     * it can be empty but must not be {@code null}
     */
    @NotNull Collection<Token> getRemainingTokens();


    /**
     * Gets the collection of tokens that were previously removed during gameplay.
     *
     * <p>
     * In some games such as Chess, it is possible to capture other players' tokens, in which case the token
     * is removed from the board. In other games such as Reversi, a captured token stays on the board but
     * changes ownership. Other games have no concept of capture.
     * </p>
     *
     * @return a collection of {@link Token}s which were removed from the board;
     * it can be empty but must not be {@code null}
     */
    @NotNull Collection<Token> getRemovedTokens();

}
