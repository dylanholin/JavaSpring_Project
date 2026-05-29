package com.squaregames.api.game.application;

import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test unitaire pour isoler le problème ConnectFour moveTo.
 * Vérifie si le moteur accepte CellPosition(col, -1) directement.
 */
class ConnectFourMoveTest {

    @Test
    void shouldMoveTokenWithGravityMarker() throws InvalidPositionException {
        var factory = new ConnectFourGameFactory();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Game game = factory.createGame(7, Set.of(p1, p2));

        // Trouver un token du currentPlayer
        UUID currentPlayer = game.getCurrentPlayerId();
        Optional<Token> tokenOpt = game.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(currentPlayer::equals).isPresent())
                .findFirst();

        assertThat(tokenOpt).as("Un token du currentPlayer doit exister").isPresent();
        Token token = tokenOpt.get();

        System.out.println("Token name=" + token.getName() + " owner=" + token.getOwnerId());
        System.out.println("AllowedMoves=" + token.getAllowedMoves());
        System.out.println("CurrentPlayer=" + currentPlayer);

        // Jouer avec CellPosition(0, -1)
        CellPosition target = new CellPosition(0, -1);
        assertThat(token.getAllowedMoves()).contains(target);

        assertThat(token.getPosition()).as("Token non placé").isNull();
        token.moveTo(target);
        assertThat(token.getPosition()).as("Token placé").isNotNull();
    }

    @Test
    void shouldMoveAfterReconstruction() throws Exception {
        var factory = new ConnectFourGameFactory();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Game original = factory.createGame(7, Set.of(p1, p2));

        // Reconstruire via createGameWithIds (comme JpaGameDao)
        List<TokenPosition<UUID>> onBoard = new ArrayList<>();
        List<TokenPosition<UUID>> removed = new ArrayList<>();

        Game reconstructed = factory.createGameWithIds(
                original.getId(), 7, List.of(p1, p2), onBoard, removed);

        UUID currentPlayer = reconstructed.getCurrentPlayerId();
        Optional<Token> tokenOpt = reconstructed.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(currentPlayer::equals).isPresent())
                .findFirst();

        assertThat(tokenOpt).isPresent();
        Token token = tokenOpt.get();
        System.out.println("RECONSTRUCTED: token=" + token.getName() + " owner=" + token.getOwnerId() + " moves=" + token.getAllowedMoves());

        CellPosition target = new CellPosition(0, -1);
        assertThat(token.getAllowedMoves()).contains(target);
        token.moveTo(target);
        assertThat(token.getPosition()).isNotNull();
    }

    @Test
    void shouldReconstructAfterOneMove() throws Exception {
        var factory = new ConnectFourGameFactory();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Game original = factory.createGame(7, Set.of(p1, p2));

        UUID currentPlayer = original.getCurrentPlayerId();
        Optional<Token> tokenOpt = original.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(currentPlayer::equals).isPresent())
                .findFirst();
        Token token = tokenOpt.get();
        token.moveTo(new CellPosition(0, -1));

        // Simuler JpaGameDao : stocker y=0 (car on stocke y=0 pour ConnectFour)
        List<TokenPosition<UUID>> onBoard = new ArrayList<>();
        onBoard.add(new TokenPosition<>(currentPlayer, token.getName(), 0, 0));
        List<TokenPosition<UUID>> removed = new ArrayList<>();

        // Réorganiser comme JpaGameDao : le joueur avec le plus de tokens doit être à l'index 1
        List<UUID> players = List.of(p1, p2);
        java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
        for (TokenPosition<UUID> tp : onBoard) {
            counts.merge(tp.owner(), 1, Integer::sum);
        }
        if (counts.getOrDefault(p1, 0) > counts.getOrDefault(p2, 0)) {
            players = List.of(p2, p1);
        }

        Game reconstructed = factory.createGameWithIds(
                original.getId(), 7, players, onBoard, removed);

        System.out.println("RECONSTRUCTED AFTER MOVE: status=" + reconstructed.getStatus()
                + " currentPlayer=" + reconstructed.getCurrentPlayerId()
                + " board=" + reconstructed.getBoard().keySet());

        assertThat(reconstructed.getStatus()).isNotNull();
        assertThat(reconstructed.getCurrentPlayerId()).isNotNull();

        // Vérifier que tous les tokens ont des allowedMoves non-null
        for (Token t : reconstructed.getBoard().values()) {
            assertThat(t.getAllowedMoves()).isNotNull();
        }
        for (Token t : reconstructed.getRemainingTokens()) {
            assertThat(t.getAllowedMoves()).isNotNull();
        }
    }

    @Test
    void shouldReconstructAfterTwoMovesAndGetAllowedMoves() throws Exception {
        var factory = new ConnectFourGameFactory();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Game original = factory.createGame(7, Set.of(p1, p2));

        // Jouer 2 coups (A col 0, B col 1)
        UUID currentPlayer = original.getCurrentPlayerId();
        Optional<Token> tokenOpt = original.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(currentPlayer::equals).isPresent())
                .findFirst();
        Token tokenA = tokenOpt.get();
        tokenA.moveTo(new CellPosition(0, -1));

        UUID nextPlayer = original.getCurrentPlayerId();
        Optional<Token> tokenOptB = original.getRemainingTokens().stream()
                .filter(t -> t.getOwnerId().filter(nextPlayer::equals).isPresent())
                .findFirst();
        Token tokenB = tokenOptB.get();
        tokenB.moveTo(new CellPosition(1, -1));

        // Simuler JpaGameDao : stocker positions réelles
        List<TokenPosition<UUID>> onBoard = new ArrayList<>();
        for (Token t : original.getBoard().values()) {
            CellPosition pos = t.getPosition();
            onBoard.add(new TokenPosition<>(
                    t.getOwnerId().orElse(null), t.getName(), pos.x(), pos.y()));
        }
        List<TokenPosition<UUID>> removed = new ArrayList<>();

        // Normaliser comme ConnectFourStateAdapter
        List<TokenPosition<UUID>> normalized = com.squaregames.api.game.infrastructure.ConnectFourStateAdapter.normalizePositions(onBoard);
        System.out.println("Normalized positions: " + normalized);

        Game reconstructed = factory.createGameWithIds(
                original.getId(), 7, List.of(p1, p2), normalized, removed);

        // Tester getAllowedMoves sur tous les tokens
        for (Token t : reconstructed.getBoard().values()) {
            System.out.println("Board token " + t.getName() + " allowedMoves=" + t.getAllowedMoves());
            assertThat(t.getAllowedMoves()).isNotNull();
        }
        for (Token t : reconstructed.getRemainingTokens()) {
            System.out.println("Remaining token " + t.getName() + " allowedMoves=" + t.getAllowedMoves());
            assertThat(t.getAllowedMoves()).isNotNull();
        }
    }

    @Test
    void shouldReconstructWithExactLogParams() throws Exception {
        var factory = new ConnectFourGameFactory();
        UUID p1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID p2 = UUID.fromString("182afe79-855b-40d1-8133-17b07025bcce");
        UUID gameId = UUID.fromString("45ac997e-0516-46c7-b5d4-7413260057ee");
        List<TokenPosition<UUID>> onBoard = List.of(
                new TokenPosition<>(p1, "R", 0, 0)
        );
        List<TokenPosition<UUID>> removed = List.of();

        // Simuler reorderPlayersForConnectFour
        List<UUID> players = List.of(p1, p2);
        java.util.Map<UUID, Integer> counts = new java.util.HashMap<>();
        for (TokenPosition<UUID> tp : onBoard) {
            counts.merge(tp.owner(), 1, Integer::sum);
        }
        if (counts.getOrDefault(p1, 0) > counts.getOrDefault(p2, 0)) {
            players = List.of(p2, p1);
        }

        Game reconstructed = factory.createGameWithIds(gameId, 7, players, onBoard, removed);
        assertThat(reconstructed.getStatus()).isNotNull();
        assertThat(reconstructed.getCurrentPlayerId()).isNotNull();
    }
}
