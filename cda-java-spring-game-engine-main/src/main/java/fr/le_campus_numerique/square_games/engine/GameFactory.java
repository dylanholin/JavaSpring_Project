package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Defines method to create instances of {@link Game} of a specific game type (e.g. Tic-tac-toe, Chess,...)
 */
public interface GameFactory {

    /**
     * Gets the identifier of the game this factory creates.
     *
     * @return an identifier such as "tictactoe" that uniquely identifies a game type; it cannot be {@code null} or blank
     */
    @NotBlank
    String getGameFactoryId();

    /**
     * Gets the range of player count allowed by this factory.
     * (Most games allow only a fixed number of players)
     *
     * @return a {@link IntRange range} of int values representing the allowed numbers of players; it cannot be {@code null}
     */
    @NotNull
    IntRange getPlayerCountRange();

    /**
     * Gets the range of board sizes allowed by this factory, for the specified player count.
     *
     * @param playerCount the number of players to get the board sizes for;
     *                    it must be a {@link #getPlayerCountRange() valid value}, or this method throws
     *                    an {@link IllegalArgumentException}
     * @return a {@link IntRange range} of int values representing the allowed board sizes
     * for the specified number of players; it cannot be {@code null}
     * @throws IllegalArgumentException if {@code playerCount} is invalid
     * @see #getPlayerCountRange()
     */
    @NotNull
    IntRange getBoardSizeRange(@Positive int playerCount);


    /**
     * Creates a new game with the specified player count and board size.
     *
     * @param playerCount the number of players taking part in the new game; it must within the allowed range
     * @param boardSize   the size of the (square) board side; it must be within the allowed range for the specified player count
     * @return the newly created {@link Game}, in its initial state
     * @throws IllegalArgumentException either {@code playerCount} or {@code boardSize} is not within its allowed range
     * @see #getPlayerCountRange()
     * @see #getBoardSizeRange(int)
     * @see #createGame(int, Set)
     * @see #createGameWithIds(UUID, int, List, Collection, Collection)
     */
    @NotNull
    Game createGame(int playerCount, int boardSize);

    /**
     * Creates a new game with the specified player identifiers and board size.
     *
     * @param playerIds the identifiers of the players taking part in the new game; it must not be null
     * @param boardSize the size of the (square) board side; it must be within the allowed range for the specified player count
     * @return the newly created {@link Game}, in its initial state
     * @throws IllegalArgumentException either the number of elements in {@code playerIds} or {@code boardSize} is not within its allowed range
     * @see #getPlayerCountRange()
     * @see #getBoardSizeRange(int)
     * @see #createGame(int, int)
     * @see #createGameWithIds(UUID, int, List, Collection, Collection)
     */
    @NotNull
    Game createGame(int boardSize, @NotNull Set<UUID> playerIds);


    /**
     * Creates a new game with the specified board size and tokens
     *
     * @param boardSize     the size of the (square) board side; it must be within the allowed range for the specified player count
     * @param players       the list of player identifiers, in the same order as  {@link Game#getPlayerIds()}; it must not be {@code null} or empty
     * @param boardTokens   the collection of tokens initially placed on the board, and their position on the board; it must not be {@code null}
     * @param removedTokens the collection of tokens considered as removed (the position-related properties are ignored); it must not be {@code null}
     * @param <K>           the type used to define player identifiers, it must have proper {@link Object#hashCode()} and
     *                      {#{@link Object#equals(Object)}} implementation; it does not affect the resulting {@link Game}
     * @return the newly created {@link Game}, in the state specified by the provided token positions
     * @throws InconsistentGameDefinitionException at least one parameter value is invalid,
     *                                             or a set of values are not consistent with each other
     * @see #createGame(int, int)
     */
    @Deprecated
    @NotNull
    <K> Game createGame(
            int boardSize,
            @NotEmpty List<K> players,
            @NotNull Collection<TokenPosition<K>> boardTokens,
            @NotNull Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException;

    /**
     * Creates a new game with the specified board size, tokens and player ids
     *
     * @param gameId        the identifier for the new game; it can be {@code null}, in which case a random id is used
     * @param boardSize     the size of the (square) board side; it must be within the allowed range for the specified player count
     * @param players       the list of player identifiers, in the same order as  {@link Game#getPlayerIds()}; it must not be {@code null} or empty
     * @param boardTokens   the collection of tokens initially placed on the board, and their position on the board; it must not be {@code null}
     * @param removedTokens the collection of tokens considered as removed (the position-related properties are ignored); it must not be {@code null}
     * @return the newly created {@link Game}, in the state specified by the provided token positions and player identifiers
     * @throws InconsistentGameDefinitionException at least one parameter value is invalid,
     *                                             or a set of values are not consistent with each other
     * @see #createGame(int, int)
     */
    @NotNull
    Game createGameWithIds(
            UUID gameId,
            int boardSize,
            @NotEmpty List<UUID> players,
            @NotNull Collection<TokenPosition<UUID>> boardTokens,
            @NotNull Collection<TokenPosition<UUID>> removedTokens) throws InconsistentGameDefinitionException;

}
