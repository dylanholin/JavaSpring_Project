package com.squaregames.api.game.application;

import fr.le_campus_numerique.square_games.engine.Game;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour les GamePlugin (TicTacToe, ConnectFour, Taquin).
 * Vérifie que chaque plugin retourne le bon gameType, crée une partie valide,
 * et délègue correctement les noms localisés au MessageSource.
 */
@ExtendWith(MockitoExtension.class)
class GamePluginTest {

    @Mock
    private MessageSource messageSource;

    private TicTacToePlugin ticTacToePlugin;
    private ConnectFourPlugin connectFourPlugin;
    private TaquinPlugin taquinPlugin;

    @BeforeEach
    @SuppressWarnings("null")
    void setUp() {
        ticTacToePlugin = new TicTacToePlugin(messageSource);
        connectFourPlugin = new ConnectFourPlugin(messageSource);
        taquinPlugin = new TaquinPlugin(messageSource);

        // Injecter les @Value manuellement (pas de Spring dans les tests unitaires)
        ReflectionTestUtils.setField(ticTacToePlugin, "defaultPlayerCount", 2);
        ReflectionTestUtils.setField(ticTacToePlugin, "defaultBoardSize", 3);
        ReflectionTestUtils.setField(connectFourPlugin, "defaultPlayerCount", 2);
        ReflectionTestUtils.setField(connectFourPlugin, "defaultBoardSize", 7);
        ReflectionTestUtils.setField(taquinPlugin, "defaultPlayerCount", 1);
        ReflectionTestUtils.setField(taquinPlugin, "defaultBoardSize", 3);
    }

    // === TicTacToe ===

    @Test
    void ticTacToe_shouldReturnCorrectGameType() {
        assertThat(ticTacToePlugin.getGameType()).isEqualTo("tictactoe");
    }

    @Test
    void ticTacToe_shouldCreateValidGame() {
        Game game = ticTacToePlugin.createGame();

        assertThat(game).as("createGame doit retourner une partie non null").isNotNull();
        assertThat(game.getFactoryId()).as("Le factoryId doit être tictactoe").isEqualTo("tictactoe");
        assertThat(game.getBoardSize()).as("Le plateau doit faire 3x3").isEqualTo(3);
        assertThat(game.getPlayerIds()).as("TicTacToe doit avoir 2 joueurs").hasSize(2);
        assertThat(game.getStatus().name()).as("Une nouvelle partie doit être ONGOING").isEqualTo("ONGOING");
    }

    @Test
    @SuppressWarnings("null")
    void ticTacToe_shouldReturnLocalizedName() {
        when(messageSource.getMessage(eq("game.tictactoe.name"), any(), any(Locale.class)))
                .thenReturn("Morpion");

        String name = ticTacToePlugin.getName(Locale.FRENCH);
        assertThat(name).isEqualTo("Morpion");
    }

    @Test
    void ticTacToe_shouldProvideFactory() {
        assertThat(ticTacToePlugin.getFactory()).as("getFactory doit retourner une factory non null").isNotNull();
        assertThat(ticTacToePlugin.getFactory().getGameFactoryId()).isEqualTo("tictactoe");
    }

    // === ConnectFour ===

    @Test
    void connectFour_shouldReturnCorrectGameType() {
        assertThat(connectFourPlugin.getGameType()).isEqualTo("connect4");
    }

    @Test
    void connectFour_shouldCreateValidGame() {
        Game game = connectFourPlugin.createGame();

        assertThat(game).as("createGame doit retourner une partie non null").isNotNull();
        assertThat(game.getFactoryId()).as("Le factoryId doit être connect4").isEqualTo("connect4");
        assertThat(game.getBoardSize()).as("Le plateau doit faire 7x7").isEqualTo(7);
        assertThat(game.getPlayerIds()).as("ConnectFour doit avoir 2 joueurs").hasSize(2);
        assertThat(game.getStatus().name()).isEqualTo("ONGOING");
    }

    @Test
    @SuppressWarnings("null")
    void connectFour_shouldReturnLocalizedName() {
        when(messageSource.getMessage(eq("game.connectfour.name"), any(), any(Locale.class)))
                .thenReturn("Puissance 4");

        String name = connectFourPlugin.getName(Locale.FRENCH);
        assertThat(name).isEqualTo("Puissance 4");
    }

    @Test
    void connectFour_shouldProvideFactory() {
        assertThat(connectFourPlugin.getFactory()).as("getFactory doit retourner une factory non null").isNotNull();
        assertThat(connectFourPlugin.getFactory().getGameFactoryId()).isEqualTo("connect4");
    }

    // === Taquin ===

    @Test
    void taquin_shouldReturnCorrectGameType() {
        assertThat(taquinPlugin.getGameType()).isEqualTo("15 puzzle");
    }

    @Test
    void taquin_shouldCreateValidGame() {
        Game game = taquinPlugin.createGame();

        assertThat(game).as("createGame doit retourner une partie non null").isNotNull();
        assertThat(game.getFactoryId()).as("Le factoryId doit être 15 puzzle").isEqualTo("15 puzzle");
        assertThat(game.getBoardSize()).as("Le plateau doit faire 3x3").isEqualTo(3);
        assertThat(game.getPlayerIds()).as("Taquin doit avoir 1 joueur").hasSize(1);
        assertThat(game.getStatus().name()).isEqualTo("ONGOING");
    }

    @Test
    @SuppressWarnings("null")
    void taquin_shouldReturnLocalizedName() {
        when(messageSource.getMessage(eq("game.taquin.name"), any(), any(Locale.class)))
                .thenReturn("Taquin");

        String name = taquinPlugin.getName(Locale.FRENCH);
        assertThat(name).isEqualTo("Taquin");
    }

    @Test
    void taquin_shouldProvideFactory() {
        assertThat(taquinPlugin.getFactory()).as("getFactory doit retourner une factory non null").isNotNull();
        assertThat(taquinPlugin.getFactory().getGameFactoryId()).isEqualTo("15 puzzle");
    }
}
