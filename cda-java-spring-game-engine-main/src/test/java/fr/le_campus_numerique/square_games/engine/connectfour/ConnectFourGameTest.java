package fr.le_campus_numerique.square_games.engine.connectfour;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class ConnectFourGameTest {

    private ConnectFourGame game;

    @BeforeEach
    public void createGame() {
        final var redPlayer = UUID.randomUUID();
        final var yellowPlayer = UUID.randomUUID();
        this.game = new ConnectFourGame(redPlayer, yellowPlayer);
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getPlayerIds()).containsExactlyInAnyOrder(redPlayer, yellowPlayer);
        assertThat(game.getBoardSize()).isEqualTo(7);
        assertThat(game.getBoard()).isNotNull().isEmpty();
        assertThat(game.getRemovedTokens()).isNotNull().isEmpty();
        assertThat(game.getRemainingTokens()).hasSize(42);
    }

    private void checkGame(
            GameStatus expectedStatus,
            IntPredicate boardTokenCountExpectation,
            Predicate<UUID> playerIdExpectation) {
        if (expectedStatus != null)
            assertThat(game.getStatus()).isEqualTo(expectedStatus);
        assertThat(game.getCurrentPlayerId()).isNotNull();
        assertThat(game.getPlayerIds()).hasSize(2);
        if (game.getStatus() == GameStatus.ONGOING)
            assertThat(game.getPlayerIds()).first().isEqualTo(game.getCurrentPlayerId());
        if (playerIdExpectation != null)
            assertThat(playerIdExpectation.test(game.getCurrentPlayerId())).isTrue();
        if (boardTokenCountExpectation != null) {
            final var positionByColumn = game.getBoard().keySet().stream().collect(Collectors.groupingBy(
                    CellPosition::x,
                    Collectors.toList()));
            final var columnHeights = positionByColumn.entrySet().stream().collect(Collectors.toMap(
                    Function.identity(),
                    e -> new TreeSet<>(e.getValue().stream().map(CellPosition::y).toList())));
            final var columnHeightSum = columnHeights.values().stream().mapToInt(TreeSet::size).sum();
            assertThat(columnHeightSum).isEqualTo(game.getBoard().size());
            assertThat(boardTokenCountExpectation.test(columnHeightSum)).isTrue();
        }
        final var tokenByOwnerId = game.getBoard().values().stream().collect(Collectors.groupingBy(
                t -> t.getOwnerId().orElseThrow(),
                Collectors.toList()));
        assertThat(tokenByOwnerId.size()).isLessThanOrEqualTo(2);
        if (tokenByOwnerId.size() == 1) {
            final var single = tokenByOwnerId.values().stream().findFirst();
            assertThat(single).hasValueSatisfying(l -> assertThat(l).hasSize(1));
        } else if (!tokenByOwnerId.isEmpty()) {
            final var tokenCounts = tokenByOwnerId.values().stream().mapToInt(List::size).toArray();
            assertThat(tokenCounts).hasSize(2);
            assertThat(tokenCounts[0]).isCloseTo(tokenCounts[1], Offset.offset(1));
            assertThat(tokenByOwnerId.keySet()).containsExactlyInAnyOrderElementsOf(game.getPlayerIds());
            assertThat(game.getPlayerIds().stream().map(tokenByOwnerId::get).mapToInt(List::size)).satisfies(list -> {
                var current = (Integer) null;
                for (final var n : list) {
                    if (current != null)
                        assertThat(n).isGreaterThanOrEqualTo(current);
                    current = n;
                }
            });
        }
    }

    private void checkTerminatedGame(CellPosition... expectedWinningLine) {
        assertThat(game.getStatus()).isEqualTo(GameStatus.TERMINATED);
        assertThat(game.getRemainingTokens()).allSatisfy(t -> assertThat(t.getAllowedMoves()).isNotNull().isEmpty());
        if (expectedWinningLine != null) {
            final var board = this.game.getBoard();
            final var winningTokens = Arrays.stream(expectedWinningLine).map(board::get).toList();
            assertThat(winningTokens).allMatch(Objects::nonNull);
            assertThat(game.getWinningLine()).containsExactlyElementsOf(winningTokens);
        }
    }

    private @NotNull Optional<Token> anyTokenForCurrentPlayer() {
        final var currentPlayerId = game.getCurrentPlayerId();
        assertThat(currentPlayerId).isNotNull();
        return this.anyTokenFor(currentPlayerId::equals);
    }

    private @NotNull Optional<Token> anyTokenFor(@NotNull Predicate<UUID> ownerIdPredicate) {
        assert ownerIdPredicate != null;
        return game.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(ownerIdPredicate).isPresent())
                .findAny();
    }

    private void playSequence(@NotNull Stream<CellPosition> positions) {
        assert positions != null;
        positions.forEach(p -> assertThat(this.anyTokenForCurrentPlayer()).hasValueSatisfying(t ->
                this.moveToken(t, () -> p)));
    }

    private void moveToken(@NotNull Token token, @NotNull Supplier<CellPosition> position) {
        assert token != null;
        assert position != null;
        assertThat(token).isNotNull();
        final var p = position.get();
        assertThat(p).isNotNull().matches(token.getAllowedMoves()::contains);
        try {
            token.moveTo(p);
        }
        catch (InvalidPositionException e) {
            fail("move should be accepted", e);
        }
    }

    private static CellPosition column(@PositiveOrZero int index) {
        assert index >= 0;
        return new CellPosition(index, -1);
    }

    @Test
    public void winColumn() {
        final var firstPlayerId = game.getCurrentPlayerId();
        assertThat(firstPlayerId).isNotNull();
        IntStream.range(0, 4).forEach(i -> {
            checkGame(GameStatus.ONGOING, n -> n == 2 * i, firstPlayerId::equals);
            assertThat(this.anyTokenFor(firstPlayerId::equals))
                    .hasValueSatisfying(token -> this.moveToken(token, () -> column(0)));
            if (i < 3) {
                checkGame(GameStatus.ONGOING, n -> n == 2 * i + 1, Predicate.not(firstPlayerId::equals));
                assertThat(this.anyTokenFor(Predicate.not(firstPlayerId::equals)))
                        .hasValueSatisfying(token -> this.moveToken(token, () -> column(1)));
            }
        });
        checkGame(GameStatus.TERMINATED, n -> n == 7, firstPlayerId::equals);
        checkTerminatedGame(
                new CellPosition(0, 0),
                new CellPosition(0, 1),
                new CellPosition(0, 2),
                new CellPosition(0, 3));
    }

    @Test
    public void winRow() {
        final var firstPlayerId = game.getCurrentPlayerId();
        assertThat(firstPlayerId).isNotNull();
        IntStream.of(0, 1, 3, 4, 5, 6).forEach(i -> {
            checkGame(GameStatus.ONGOING, null, firstPlayerId::equals);
            assertThat(this.anyTokenFor(firstPlayerId::equals))
                    .hasValueSatisfying(token -> this.moveToken(token, () -> column(i)));
            if (i < 6) {
                checkGame(GameStatus.ONGOING, null, Predicate.not(firstPlayerId::equals));
                assertThat(this.anyTokenFor(Predicate.not(firstPlayerId::equals)))
                        .hasValueSatisfying(token -> this.moveToken(token, () -> column(i)));
            }
        });
        checkGame(GameStatus.TERMINATED, n -> n == 11, firstPlayerId::equals);
        checkTerminatedGame(
                new CellPosition(3, 0),
                new CellPosition(4, 0),
                new CellPosition(5, 0),
                new CellPosition(6, 0));
    }

    @Test
    public void winUpwardDiagonal() {
        this.playSequence(Stream.of(
                column(1), column(2),
                column(2), column(3),
                column(4), column(3),
                column(3), column(4),
                column(4), column(5)));
        checkGame(GameStatus.ONGOING, n -> n == 10, null);
        this.playSequence(Stream.of(column(4)));
        checkGame(GameStatus.TERMINATED, n -> n == 11, null);
        checkTerminatedGame(
                new CellPosition(1, 0),
                new CellPosition(2, 1),
                new CellPosition(3, 2),
                new CellPosition(4, 3));
    }

}
