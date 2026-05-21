package fr.le_campus_numerique.square_games.engine.taquin;

import fr.le_campus_numerique.square_games.engine.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

class TaquinGameTest {

    private TaquinGame game;

    private List<Token> tokensThatCanMove() {
        return Optional.ofNullable(this.game)
                .map(Game::getBoard)
                .map(Map::values)
                .map(tokens -> tokens.stream().filter(Token::canMove).toList())
                .orElseGet(List::of);
    }

    @BeforeEach
    public void createGame() {
        this.game = new TaquinGame(4);
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
        assertThat(game.getPlayerIds()).hasSize(1);
        assertThat(game.getBoardSize()).isEqualTo(4);
        assertThat(game.getBoard()).hasSize(15);
        assertThat(game.getRemovedTokens()).isNotNull().isEmpty();
        assertThat(game.getRemainingTokens()).isNotNull().isEmpty();
    }

    @Test
    public void nominal() throws InvalidPositionException {
        final var bottomRight = new CellPosition(4, 4);
        final var tokens = this.tokensThatCanMove();
        assertThat(tokens).hasSizeGreaterThanOrEqualTo(2);
        final var token = tokens.get(0);
        assertThat(token.getAllowedMoves()).isNotEmpty();
        final var destination = token.getAllowedMoves().stream()
                .filter(Predicate.not(bottomRight::equals))
                .findAny()
                .orElseThrow();
        assertThat(game.getBoard()).doesNotContainKey(destination);
        token.moveTo(destination);
        assertThat(game.getStatus()).isEqualTo(GameStatus.ONGOING);
    }

}
