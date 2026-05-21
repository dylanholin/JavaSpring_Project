package fr.le_campus_numerique.square_games.engine.tictactoe;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.GameStatus;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicTacToeGameTest {

    private Optional<Token> currentToken(TicTacToeGame game) {
        return Optional.ofNullable(game)
                .map(TicTacToeGame::getRemainingTokens)
                .flatMap(t -> t.stream().findFirst());
    }

    private @NotNull Token moveCurrentTokenTo(TicTacToeGame game, int x, int y, UUID expectedPlayerId) throws InvalidPositionException {
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        if (expectedPlayerId != null)
            assertThat(game).isNotNull().extracting(TicTacToeGame::getCurrentPlayerId).isEqualTo(expectedPlayerId);
        final var token = currentToken(game).orElseThrow();
        assertThat(token.canMove()).isTrue();
        assertThat(token.getPosition()).isNull();
        token.moveTo(new CellPosition(x, y));
        assertThat(token.getPosition()).isEqualTo(new CellPosition(x, y));
        assertThat(token.canMove()).isFalse();
        return token;
    }

    public TicTacToeGame createGame(int boardSize) {
        final var playerA = UUID.randomUUID();
        final var playerB = UUID.randomUUID();
        final var game = new TicTacToeGame(boardSize, playerA, playerB);
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getPlayerIds()).containsExactlyInAnyOrder(playerA, playerB);
        assertThat(game.getBoard()).isNotNull().isEmpty();
        assertThat(game.getRemovedTokens()).isNotNull().isEmpty();
        assertThat(game.getRemainingTokens()).hasSize(boardSize * boardSize);
        return game;
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 8})
    public void nominal(int boardSize) throws InvalidPositionException {
        final var game = createGame(boardSize);
        assertThat(game.getRemainingTokens()).first().satisfies(token -> {
            assertThat(token.canMove()).isTrue();
            assertThat(token.getPosition()).isNull();
            assertThat(token.getAllowedMoves()).hasSize(boardSize * boardSize);
        });
        assertThat(game.getPlayerIds()).hasSize(2).first().isEqualTo(game.getCurrentPlayerId());
        final var firstPlayerId = moveCurrentTokenTo(game, 1, 1, null).getOwnerId().orElseThrow();
        assertThat(game.getBoard()).hasSize(1);
        assertThat(currentToken(game)).hasValueSatisfying(token ->
                assertThat(token.getAllowedMoves())
                        .hasSize(boardSize * boardSize - 1)
                        .doesNotContain(new CellPosition(1, 1)));
        assertThat(game.getPlayerIds()).hasSize(2).first().isEqualTo(game.getCurrentPlayerId());
        final var secondPlayerId = moveCurrentTokenTo(game, 1, 0, null).getOwnerId().orElseThrow();
        assertThat(game.getBoard()).hasSize(2);
        moveCurrentTokenTo(game, 0, boardSize - 1, firstPlayerId);
        assertThat(game.getBoard()).hasSize(3);
        assertThat(game.getPlayerIds()).hasSize(2).first().isEqualTo(game.getCurrentPlayerId());
        moveCurrentTokenTo(game, boardSize - 1, 0, secondPlayerId);
        assertThat(game.getBoard()).hasSize(4);
        assertThat(game.getPlayerIds()).hasSize(2).first().isEqualTo(game.getCurrentPlayerId());
        moveCurrentTokenTo(game, 0, 0, firstPlayerId);
        assertThat(game.getBoard()).hasSize(5);
        moveCurrentTokenTo(game, boardSize - 1, boardSize - 1, secondPlayerId);
        assertThat(game.getBoard()).hasSize(6);
        moveCurrentTokenTo(game, 0, 1, firstPlayerId);
        assertThat(game.getBoard()).hasSize(7);
        if (boardSize == 3) {
            assertThat(game.getStatus()).isEqualTo(GameStatus.TERMINATED);
            assertThat(game.getCurrentPlayerId()).isEqualTo(firstPlayerId);
            assertThat(game.getPlayerIds()).hasSize(2).first().isNotEqualTo(game.getCurrentPlayerId());
            return;
        }
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        moveCurrentTokenTo(game, boardSize - 1, boardSize - 2, secondPlayerId);
        moveCurrentTokenTo(game, 0, boardSize - 2, firstPlayerId);
        if (boardSize == 4) {
            assertThat(game.getStatus()).isEqualTo(GameStatus.TERMINATED);
            assertThat(game.getCurrentPlayerId()).isEqualTo(firstPlayerId);
            assertThat(game.getPlayerIds()).hasSize(2).first().isNotEqualTo(game.getCurrentPlayerId());
            return;
        }
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        moveCurrentTokenTo(game, boardSize - 1, boardSize - 3, secondPlayerId);
        moveCurrentTokenTo(game, 0, boardSize - 3, firstPlayerId);
        if (boardSize == 5) {
            assertThat(game.getStatus()).isEqualTo(GameStatus.TERMINATED);
            assertThat(game.getCurrentPlayerId()).isEqualTo(firstPlayerId);
            assertThat(game.getPlayerIds()).hasSize(2).first().isNotEqualTo(game.getCurrentPlayerId());
        }
    }

}
