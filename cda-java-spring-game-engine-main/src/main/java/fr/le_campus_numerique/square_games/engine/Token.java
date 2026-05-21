package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.*;

/**
 * Represents an element of a game. Actual name may vary from game to game (e.g. piece, disk, token, stone...)
 * <p>
 * {@link #moveTo(CellPosition) Moving} a token to a new position is currently the only way to alter the state of a {@link Game}.
 * </p>
 *
 * @see #moveTo(CellPosition)
 */
public interface Token {

    /**
     * Gets the identifier of the player owning this token, or {@link Optional#empty() empty}
     * if this token has no owner.
     *
     * @return an {@link Optional} with the owner identifier, or {@link Optional#empty() empty}.
     */
    @NotNull Optional<UUID> getOwnerId();

    /**
     * Gets the name of this token.
     *
     * <p>
     * Depending on the game, tokens may not have uniquely names.
     * However, tokens with the same name are considered as interchangeable.
     * In Connect Four for example, all tokens are equivalent and should thus have the same name.
     * In Chess however, all Pawns should share a name, both Castles should share another name, both Knights
     * should share yet another name, but the King, the Queen and both Bishops should have unique names.
     * </p>
     *
     * @return the name of this token; it must not be {@code null} or blank
     */
    @NotBlank String getName();

    /**
     * Gets the current position of this token. Only tokens placed on the board can have a position.
     *
     * @return the current position of this token, or {@code null} if this token is not on the board.
     * @see Game#getBoard()
     */
    CellPosition getPosition();

    /**
     * Returns a flag indicating if this token can be moved.
     * It is {@code true} if and only if there is at least one {@link #getAllowedMoves() allowed moves} for this token.
     *
     * @return {@code true} if this token can be moved, {@code false} otherwise
     * @see #getAllowedMoves()
     */
    default boolean canMove() {
        return !this.getAllowedMoves().isEmpty();
    }

    /**
     * Gets all positions this token can be moved to.
     * <p>
     * How the set of positions is computed depends entirely on the game and its rules.
     * For example, in Tic-tac-toe, only {@link Game#getRemainingTokens()} remaining} tokens
     * can be placed on the board, and they cannot be moved afterward.
     * In Chess however, tokens have different ways to move, and the allowed moves also depend
     * on the position of the other tokens.
     * </p>
     * <p>
     * Although in most games, a token can only be moved if it belongs to the current player,
     * this is not required by the {@code Token} interface.
     * If moving a token not owned by the current player is allowed, it is valid for an implementation
     * to have a non-empty set of moves for any token it needs to, in order to implement the game rules.
     * </p>
     *
     * @return a set of all positions this token can be moved to;
     * it cannot be {@code null}, but it can be empty if this token cannot move
     * @see #moveTo(CellPosition)
     */
    @NotNull Set<CellPosition> getAllowedMoves();

    /**
     * Moves this token to the specified position.
     * The move is valid if and only if {@link #getAllowedMoves() allowed moves} contains the specified position.
     *
     * @param position the position to move this token to; it must not be {@code null}
     * @throws NullPointerException     {@code position} parameter is {@code null}
     * @throws InvalidPositionException this token cannot be moved to the specified {@code position}
     */
    void moveTo(@NotNull CellPosition position) throws InvalidPositionException;

}
