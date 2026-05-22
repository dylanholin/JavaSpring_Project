package com.squaregames.api.game.application;

import com.squaregames.api.game.api.dto.GameCreationParams;
import com.squaregames.api.game.api.dto.GameDto;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

/**
 * Tests unitaires pour GameServiceImpl.
 * Utilise Mockito pour isoler le service du DAO et des plugins.
 */
@ExtendWith(MockitoExtension.class)
class GameServiceImplTest {

    @Mock
    private GameDao gameDao;

    @Mock
    private GamePlugin gamePlugin;

    @Mock
    private GameFactory gameFactory;

    @Mock
    private Game game;

    private GameServiceImpl gameService;

    @BeforeEach
    void setUp() {
        lenient().when(gamePlugin.getGameType()).thenReturn("tictactoe");
        lenient().when(gamePlugin.getFactory()).thenReturn(gameFactory);

        gameService = new GameServiceImpl(gameDao, List.of(gamePlugin));
    }

    @Test
    void shouldCreateGame() {
        // Given
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        UUID gameId = UUID.randomUUID();

        when(gameFactory.createGame(2, 3)).thenReturn(game);
        when(game.getId()).thenReturn(gameId);
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(UUID.randomUUID(), UUID.randomUUID()));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);
        when(gameDao.upsert(any(Game.class))).thenReturn(game);

        // When
        GameDto result = gameService.createGame(params);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(gameId);
        assertThat(result.gameType()).isEqualTo("tictactoe");
        verify(gameDao).upsert(game);
    }

    @Test
    void shouldThrowExceptionForUnknownGameType() {
        // Given
        GameCreationParams params = new GameCreationParams("unknown", 2, 3);

        // Then
        assertThatThrownBy(() -> gameService.createGame(params))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rex = (ResponseStatusException) ex;
                    assertThat(rex.getStatusCode().value()).isEqualTo(400);
                });
    }

    @Test
    void shouldListGames() {
        // Given
        when(gameDao.findAll()).thenReturn(List.of(game));
        when(game.getId()).thenReturn(UUID.randomUUID());
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(UUID.randomUUID()));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);

        // When
        Collection<GameDto> result = gameService.listGames();

        // Then
        assertThat(result).hasSize(1);
        verify(gameDao).findAll();
    }

    @Test
    void shouldGetGameById() {
        // Given
        UUID gameId = UUID.randomUUID();
        when(gameDao.findById(gameId)).thenReturn(Optional.of(game));
        when(game.getId()).thenReturn(gameId);
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(UUID.randomUUID()));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);

        // When
        GameDto result = gameService.getGame(gameId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(gameId);
    }

    @Test
    void shouldThrowExceptionWhenGameNotFound() {
        // Given
        UUID unknownId = UUID.randomUUID();
        when(gameDao.findById(unknownId)).thenReturn(Optional.empty());

        // Then
        assertThatThrownBy(() -> gameService.getGame(unknownId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rex = (ResponseStatusException) ex;
                    assertThat(rex.getStatusCode().value()).isEqualTo(404);
                });
    }
}
