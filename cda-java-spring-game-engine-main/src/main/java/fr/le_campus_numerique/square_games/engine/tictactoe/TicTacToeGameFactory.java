package fr.le_campus_numerique.square_games.engine.tictactoe;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;
import java.util.function.Function;

public class TicTacToeGameFactory implements GameFactory {

    static final String ID = "tictactoe";

    @Override
    public String getGameFactoryId() {
        return ID;
    }

    @Override
    public IntRange getPlayerCountRange() {
        return new IntRange(2);
    }

    @Override
    public IntRange getBoardSizeRange(int playerCount) {
        if (playerCount != 2)
            throw new IllegalArgumentException();
        return new IntRange(3, 5);
    }

    @Override
    public TicTacToeGame createGame(@Min(2) @Max(2) int playerCount, @Min(3) @Max(5) int boardSize) {
        if (playerCount != 2)
            throw new IllegalArgumentException("playerCount must be equal to 2");
        return this.createGame(boardSize, Set.of(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Override
    public TicTacToeGame createGame(@Min(3) @Max(5) int boardSize, @NotNull @Size(min = 2, max = 2) Set<UUID> playerIds) {
        if ((boardSize < 3) || (boardSize > 5))
            throw new IllegalArgumentException("boardSize must be between 3 and 5");
        if (Objects.requireNonNull(playerIds).size() != 2) {
            throw new IllegalArgumentException("playerIds must contain exactly two elements");
        }
        final var ids = playerIds.toArray(new UUID[2]);
        return new TicTacToeGame(boardSize, ids[0], ids[1]);
    }


    @Override
    public <K> TicTacToeGame createGame(
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(null, boardSize, players, boardTokens, removedTokens, null);
    }

    @Override
    public TicTacToeGame createGameWithIds(
            UUID gameId,
            int boardSize,
            List<UUID> players,
            Collection<TokenPosition<UUID>> boardTokens,
            Collection<TokenPosition<UUID>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(gameId, boardSize, players, boardTokens, removedTokens, Function.identity());
    }

    private <K> TicTacToeGame createGame(
            UUID id,
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens,
            Function<K, UUID> playerIdProvider) throws InconsistentGameDefinitionException {
        final var tokens = checkAndGroupByPlayer(boardSize, players, boardTokens, removedTokens);
        assert (tokens != null) && (tokens.size() == 2);
        return new TicTacToeGame(
                id,
                boardSize,
                (playerIdProvider == null) ? null : players.stream().map(playerIdProvider).toList(),
                tokens.get(players.get(0)),
                tokens.get(players.get(1)));
    }

    private static <K> @NotNull @Size(min = 2, max = 2) Map<K, Set<CellPosition>> checkAndGroupByPlayer(
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        if ((boardSize < 3) || (boardSize > 5))
            throw new InconsistentGameDefinitionException("boardSize must be between 3 and 5");
        if ((Objects.requireNonNull(players).size() != 2) || Objects.equals(players.get(0), players.get(1)))
            throw new InconsistentGameDefinitionException("players must have exactly 2 distinct elements");
        final var maxTokens = boardSize * boardSize;
        if (Objects.requireNonNull(boardTokens).size() > maxTokens)
            throw new InconsistentGameDefinitionException("boardTokens should contain no more than " + maxTokens + " elements");
        if (!Objects.requireNonNull(removedTokens).isEmpty())
            throw new InconsistentGameDefinitionException("removedTokens should be empty");
        final var result = GameFactoryHelper.groupBoardTokensByPlayer(
                boardTokens,
                players,
                GameFactoryHelper.positionChecker(IntRange.within(0, boardSize - 1), IntRange.within(0, boardSize - 1)));
        final var tokenCounts = result.values().stream().mapToInt(Collection::size).toArray();
        assert tokenCounts.length == 2;
        if (Math.abs(tokenCounts[0] - tokenCounts[1]) > 1)
            throw new InconsistentGameDefinitionException("invalid distribution of placed tokens between players");
        return result;
    }

}
