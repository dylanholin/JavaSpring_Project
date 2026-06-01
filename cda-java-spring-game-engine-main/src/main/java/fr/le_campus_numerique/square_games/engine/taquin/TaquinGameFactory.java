package fr.le_campus_numerique.square_games.engine.taquin;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;

public class TaquinGameFactory implements GameFactory {

    static final String ID = "15 puzzle";

    @Override
    public String getGameFactoryId() {
        return ID;
    }

    @Override
    public IntRange getPlayerCountRange() {
        return new IntRange(1);
    }

    @Override
    public IntRange getBoardSizeRange(int playerCount) {
        if (playerCount != 1)
            throw new IllegalArgumentException();
        return new IntRange(3, 8);
    }

    @Override
    public TaquinGame createGame(@Min(1) @Max(1) int playerCount, @Min(3) @Max(8) int boardSize) {
        if (playerCount != 1)
            throw new IllegalArgumentException("playerCount must be equal to 1");
        return this.createGame(boardSize, Set.of(UUID.randomUUID()));
    }

    @Override
    public TaquinGame createGame(@Min(3) @Max(8) int boardSize, @NotNull @Size(min = 1, max = 1) Set<UUID> playerIds) {
        if ((boardSize < 3) && (boardSize > 8))
            throw new IllegalArgumentException("boardSize must be equal to 4");
        if (Objects.requireNonNull(playerIds).size() != 1) {
            throw new IllegalArgumentException("playerIds must contain exactly one element");
        }
        return new TaquinGame(boardSize, playerIds.stream().findFirst().orElseThrow());
    }

    @Override
    public <K> TaquinGame createGame(
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(null, boardSize, players, boardTokens, removedTokens, null);
    }

    @Override
    public TaquinGame createGameWithIds(
            UUID gameId,
            int boardSize,
            List<UUID> players,
            Collection<TokenPosition<UUID>> boardTokens,
            Collection<TokenPosition<UUID>> removedTokens) throws InconsistentGameDefinitionException {
        return this.createGame(gameId, boardSize, players, boardTokens, removedTokens, Function.identity());
    }


    private <K> TaquinGame createGame(
            UUID id,
            int boardSize,
            List<K> players,
            Collection<TokenPosition<K>> boardTokens,
            Collection<TokenPosition<K>> removedTokens,
            Function<K, UUID> playerIdProvider) throws InconsistentGameDefinitionException {
        if (boardSize != 4)
            throw new InconsistentGameDefinitionException("boardSize must be equal to 4");
        if (Objects.requireNonNull(players).size() != 1)
            throw new InconsistentGameDefinitionException("players must have exactly one element");
        if (Objects.requireNonNull(boardTokens).size() != 15)
            throw new InconsistentGameDefinitionException("boardTokens should contain exactly 15 elements");
        if (!Objects.requireNonNull(removedTokens).isEmpty())
            throw new InconsistentGameDefinitionException("removedTokens should be empty");
        final var playerKey = players.get(0);
        final var positions = new CellPosition[15];
        for (final var token : boardTokens) {
            final var index = checkTokenAndGetIndex(playerKey, 4, token);
            if (positions[index] != null)
                throw new InconsistentGameDefinitionException("several tokens have the same position");
            positions[index] = new CellPosition(token.x(), token.y());
        }
        return new TaquinGame(
                id,
                (playerIdProvider == null) ? null : playerIdProvider.apply(players.get(0)),
                List.of(positions));
    }

    private static <K> int checkTokenAndGetIndex(K expectedOwner, int boardSize, @NotNull TokenPosition<K> token) throws InconsistentGameDefinitionException {
        final var validity = (IntPredicate) new IntRange(0, 4)::contains;
        if (!Objects.equals(expectedOwner, token.owner()))
            throw new InconsistentGameDefinitionException("token owner is invalid | " + token);
        if (!validity.test(token.x()) || !validity.test(token.y()))
            throw new InconsistentGameDefinitionException("unexpected token position | " + token);
        try {
            final var value = Integer.parseInt(token.tokenName());
            if (IntRange.within(1, boardSize * boardSize - 1).test(value))
                return value - 1;
        }
        catch (NumberFormatException ignored) {
        }
        throw new InconsistentGameDefinitionException("unexpected token name | " + token);
    }

}
