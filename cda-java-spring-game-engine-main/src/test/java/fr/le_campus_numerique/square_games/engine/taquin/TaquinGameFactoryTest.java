package fr.le_campus_numerique.square_games.engine.taquin;

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

class TaquinGameFactoryTest {

    private static final Random RANDOM = new Random();

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8})
    public void nominalWithPlayerCount(int boardSize) {
        final var factory = new TaquinGameFactory();
        assertThat(factory.createGame(1, boardSize)).isNotNull().satisfies(game -> {
            assertThat(game.getFactoryId()).isEqualTo(factory.getGameFactoryId());
            assertThat(game.getBoardSize()).isEqualTo(boardSize);
            assertThat(game.getPlayerIds()).hasSize(1);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 6, 7, 8})
    public void nominalWithPlayerId(int boardSize) {
        final var factory = new TaquinGameFactory();
        final var playerId = UUID.randomUUID();
        assertThat(factory.createGame(boardSize, Set.of(playerId))).isNotNull().satisfies(map -> {
            assertThat(map.getFactoryId()).isEqualTo(factory.getGameFactoryId());
            assertThat(map.getBoardSize()).isEqualTo(boardSize);
            assertThat(map.getPlayerIds()).containsExactly(playerId);
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-2, 0, 2, 10})
    public void withInvalidBoardSize(int boardSize) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TaquinGameFactory().createGame(1, boardSize));
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 2, 3})
    public void withInvalidPlayerCount(int playerCount) {
        assertThrowsExactly(
                IllegalArgumentException.class,
                () -> new TaquinGameFactory().createGame(playerCount, 4));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 8, 20, 50, 100})
    public void restore(int maxMoveCount) throws InconsistentGameDefinitionException {
        final var game = createGame(maxMoveCount);
        assertThat(game).isNotNull();
        final var playerKey = "_" + ('a' + RANDOM.nextInt(26)) + "_";
        final var board = new ArrayList<TokenPosition<String>>();
        game.getBoard().forEach((position, token) -> board.add(new TokenPosition<>(
                playerKey,
                token.getName(),
                position.x(),
                position.y())));
        final var restored = new TaquinGameFactory().createGame(
                4,
                List.of(playerKey),
                board,
                List.of());
        assertThat(restored).isNotNull();
        assertThat(restored.getBoard())
                .isNotNull()
                .hasSize(15)
                .allSatisfy((position, token) -> {
                    assertThat(position).isNotNull();
                    assertThat(token).isNotNull();
                    assertThat(token.getPosition()).isEqualTo(position);
                });
        assertThat(restored.getRemainingTokens()).isNotNull().isEmpty();
        assertThat(restored.getRemovedTokens()).isNotNull().isEmpty();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 8, 20, 50, 100})
    public void restoreWithSamePlayerIds(int maxMoveCount) throws InconsistentGameDefinitionException {
        final var game = createGame(maxMoveCount);
        final var playerIds = List.copyOf(game.getPlayerIds());
        assertThat(game).isNotNull();
        final var board = new ArrayList<TokenPosition<UUID>>();
        game.getBoard().forEach((position, token) -> board.add(new TokenPosition<>(
                playerIds.get(0),
                token.getName(),
                position.x(),
                position.y())));
        final var restored = new TaquinGameFactory().createGameWithIds(
                game.getId(),
                4,
                playerIds,
                board,
                List.of());
        assertThat(restored).isNotNull();
        assertThat(restored.getId()).isEqualTo(game.getId());
        assertThat(restored.getPlayerIds()).containsExactlyElementsOf(playerIds);
        assertThat(restored.getBoard())
                .isNotNull()
                .hasSize(15)
                .allSatisfy((position, token) -> {
                    assertThat(position).isNotNull();
                    assertThat(token).isNotNull();
                    assertThat(token.getPosition()).isEqualTo(position);
                });
        assertThat(restored.getRemainingTokens()).isNotNull().isEmpty();
        assertThat(restored.getRemovedTokens()).isNotNull().isEmpty();
    }

    private static @NotNull TaquinGame createGame(@Min(0) @Max(100) int maxMoveCount) {
        final var game = new TaquinGameFactory().createGame(1, 4);
        for (int i = 0; (i < maxMoveCount) && game.getStatus() != GameStatus.TERMINATED; i++) {
            final var token = game.getBoard().values().stream().filter(Token::canMove).findFirst().orElse(null);
            assertThat(token).isNotNull();
            final var positions = List.copyOf(token.getAllowedMoves());
            assertThat(positions).isNotEmpty();
            assertDoesNotThrow(() -> token.moveTo(positions.get(RANDOM.nextInt(positions.size()))));
        }
        return game;
    }
}
