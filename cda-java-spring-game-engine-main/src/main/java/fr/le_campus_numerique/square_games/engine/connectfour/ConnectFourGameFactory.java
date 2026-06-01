package fr.le_campus_numerique.square_games.engine.connectfour;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

public class ConnectFourGameFactory implements GameFactory {

    static final String ID = "connect4";

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
        return new IntRange(7);
    }

    @Override
    public ConnectFourGame createGame(@Min(2) @Max(2) int playerCount, @Min(7) @Max(7) int boardSize) {
        if (playerCount != 2)
            throw new IllegalArgumentException("playerCount must be equal to 2");
        return this.createGame(boardSize, Set.of(UUID.randomUUID(), UUID.randomUUID()));
    }

    @Override
    public ConnectFourGame createGame(@Min(7) @Max(7) int boardSize, @NotNull @Size(min = 2, max = 2) Set<UUID> playerIds) {
        if (boardSize != 7)
            throw new IllegalArgumentException("boardSize must be equal to 7");
        if (Objects.requireNonNull(playerIds).size() != 2) {
            throw new IllegalArgumentException("playerIds must contain exactly two elements");
        }
        final var ids = playerIds.toArray(new UUID[2]);
        return new ConnectFourGame(ids[0], ids[1]);
    }

    @Override
    public <K> ConnectFourGame createGame(
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(null, boardSize, players, boardTokens, removedTokens, null);
    }

    @Override
    public Game createGameWithIds(
            UUID gameId,
            int boardSize,
            List<UUID> players,
            Collection<TokenPosition<UUID>> boardTokens,
            Collection<TokenPosition<UUID>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(gameId, boardSize, players, boardTokens, removedTokens, Function.identity());
    }

    private <K> ConnectFourGame createGame(
            UUID id,
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens,
            Function<K, UUID> playerIdProvider) throws InconsistentGameDefinitionException {
        final var tokens = checkAndGroupByPlayer(boardSize, players, boardTokens, removedTokens);
        assert (tokens != null) && (tokens.size() == 2);
        assert players.size() == 2;
        final var columns = IntStream.range(0, ConnectFourGame.COLUMN_COUNT)
                .mapToObj(n -> new TreeMap<Integer, Integer>())
                .toList();
        tokens.forEach((k, v) -> {
            final var playerIndex = players.indexOf(k);
            assert IntRange.within(0, 1).test(playerIndex);
            v.forEach(p -> {
                assert IntRange.within(0, ConnectFourGame.COLUMN_COUNT).test(p.x());
                columns.get(p.x()).put(p.y(), playerIndex);
            });
        });
        if (columns.stream().anyMatch(m -> !m.isEmpty() && (m.firstKey() < 0 || m.lastKey() >= m.size())))
            throw new InconsistentGameDefinitionException("inconsistent token positions in boardTokens");
        return new ConnectFourGame(
                id,
                (playerIdProvider == null) ? null : players.stream().map(playerIdProvider).toList(),
                columns.stream()
                        .map(Map::values)
                        .map(List::copyOf)
                        .toList());
    }

    private static <K> @NotNull @Size(min = 2, max = 2) Map<K, Set<CellPosition>> checkAndGroupByPlayer(
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        if (boardSize != 7)
            throw new InconsistentGameDefinitionException("boardSize must be equal to 7");
        if ((Objects.requireNonNull(players).size() != 2) || Objects.equals(players.get(0), players.get(1)))
            throw new InconsistentGameDefinitionException("players must have exactly 2 distinct elements");
        if (Objects.requireNonNull(boardTokens).size() > 42)
            throw new InconsistentGameDefinitionException("boardTokens should contain no more than 42 elements");
        if (!Objects.requireNonNull(removedTokens).isEmpty())
            throw new InconsistentGameDefinitionException("removedTokens should be empty");
        final var result = GameFactoryHelper.groupBoardTokensByPlayer(
                boardTokens,
                players,
                GameFactoryHelper.positionChecker(
                        IntRange.within(0, ConnectFourGame.COLUMN_COUNT),
                        IntRange.within(0, ConnectFourGame.ROW_COUNT)));
        final var tokenCounts = result.values().stream().mapToInt(Collection::size).toArray();
        assert tokenCounts.length == 2;
        if (Math.abs(tokenCounts[0] - tokenCounts[1]) > 1)
            throw new InconsistentGameDefinitionException("invalid distribution of placed tokens between players");
        return result;
    }

}
