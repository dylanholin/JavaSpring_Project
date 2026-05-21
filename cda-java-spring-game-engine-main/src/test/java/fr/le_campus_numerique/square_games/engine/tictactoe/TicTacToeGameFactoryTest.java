package fr.le_campus_numerique.square_games.engine.tictactoe;

import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.InconsistentGameDefinitionException;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

class TicTacToeGameFactoryTest {

    private static final Random RANDOM = new Random();

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    public void nominal(int boardSize) {
        final var factory = new TicTacToeGameFactory();
        assertThat(factory.createGame(2, boardSize)).isNotNull().satisfies(game -> {
            assertThat(game.getFactoryId()).isEqualTo(factory.getGameFactoryId());
            assertThat(game.getBoardSize()).isEqualTo(boardSize);
            assertThat(game.getPlayerIds()).hasSize(2);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5})
    public void nominalWithPlayerId(int boardSize) {
        final var factory = new TicTacToeGameFactory();
        final var players = Set.of(UUID.randomUUID(), UUID.randomUUID());
        assertThat(factory.createGame(boardSize, players)).isNotNull().satisfies(map -> {
            assertThat(map.getFactoryId()).isEqualTo(factory.getGameFactoryId());
            assertThat(map.getBoardSize()).isEqualTo(boardSize);
            assertThat(map.getPlayerIds()).containsExactlyInAnyOrderElementsOf(players);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 2, 8})
    public void withInvalidBoardSize(int boardSize) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TicTacToeGameFactory().createGame(2, boardSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1, 3, 4})
    public void withInvalidPlayerCount(int playerCount) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TicTacToeGameFactory().createGame(playerCount, 5));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    public void restore(int maxMoveCount) throws InconsistentGameDefinitionException {
        final var game = createGame(maxMoveCount);
        assertThat(game).isNotNull();
        final var playerKeys = List.of("_A_", "_B_");
        final var playerIds = new LinkedHashMap<UUID, String>(2);
        game.getPlayerIds().forEach(id -> playerIds.put(id, playerKeys.get(playerIds.size())));
        final var board = new ArrayList<TokenPosition<String>>();
        game.getBoard().forEach((position, token) -> board.add(new TokenPosition<>(
                playerIds.get(token.getOwnerId().orElseThrow()),
                token.getName(),
                position.x(),
                position.y())));
        final var restored = new TicTacToeGameFactory().createGame(
                3,
                List.copyOf(playerIds.values()),
                board,
                List.of());
        assertThat(restored).isNotNull();
        assertThat(restored.getBoard())
                .isNotNull()
                .allSatisfy((position, token) -> {
                    assertThat(position).isNotNull();
                    assertThat(token).isNotNull();
                    assertThat(token.canMove()).isFalse();
                    assertThat(token.getPosition()).isEqualTo(position);
                });
        assertThat(restored.getRemainingTokens()).isNotNull();
        assertThat(restored.getRemovedTokens()).isNotNull().isEmpty();
        assertThat(restored.getBoard().size() + restored.getRemainingTokens().size()).isEqualTo(9);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9})
    public void restoreWithSamePlayerIds(int maxMoveCount) throws InconsistentGameDefinitionException {
        final var game = createGame(maxMoveCount);
        final var playerIds = List.copyOf(game.getPlayerIds());
        assertThat(game).isNotNull();
        final var board = new ArrayList<TokenPosition<UUID>>();
        game.getBoard().forEach((position, token) -> board.add(new TokenPosition<>(
                token.getOwnerId().orElseThrow(),
                token.getName(),
                position.x(),
                position.y())));
        final var restored = new TicTacToeGameFactory().createGameWithIds(
                game.getId(),
                3,
                playerIds,
                board,
                List.of());
        assertThat(restored).isNotNull();
        assertThat(restored.getId()).isEqualTo(game.getId());
        assertThat(restored.getPlayerIds()).containsExactlyElementsOf(playerIds);
        assertThat(restored.getBoard())
                .isNotNull()
                .allSatisfy((position, token) -> {
                    assertThat(position).isNotNull();
                    assertThat(token).isNotNull();
                    assertThat(token.canMove()).isFalse();
                    assertThat(token.getPosition()).isEqualTo(position);
                });
        assertThat(restored.getRemainingTokens()).isNotNull();
        assertThat(restored.getRemovedTokens()).isNotNull().isEmpty();
        assertThat(restored.getBoard().size() + restored.getRemainingTokens().size()).isEqualTo(9);
    }

    private static @NotNull TicTacToeGame createGame(@Min(0) @Max(9) int maxMoveCount) {
        final var game = new TicTacToeGameFactory().createGame(2, 3);
        for (int i = 0; (i < maxMoveCount) && game.getStatus() != GameStatus.TERMINATED; i++) {
            final var token = game.getRemainingTokens().stream().filter(Token::canMove).findFirst().orElse(null);
            assertThat(token).isNotNull();
            final var positions = List.copyOf(token.getAllowedMoves());
            assertDoesNotThrow(() -> token.moveTo(positions.get(RANDOM.nextInt(positions.size()))));
        }
        return game;
    }

}
