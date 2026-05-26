package com.squaregames.api.game.application;

import com.squaregames.api.game.api.dto.GameCreationParams;
import com.squaregames.api.game.api.dto.GameDto;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
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

    @Mock
    private UserValidator userValidator;

    private GameServiceImpl gameService;

    private static final String USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {
        lenient().when(gamePlugin.getGameType()).thenReturn("tictactoe");
        lenient().when(gamePlugin.getFactory()).thenReturn(gameFactory);
        lenient().doNothing().when(userValidator).validate(any());

        gameService = new GameServiceImpl(gameDao, List.of(gamePlugin), userValidator);
    }

    @Test
    void shouldCreateGame() {
        // Given
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        UUID gameId = UUID.randomUUID();

        UUID userUuid = UUID.fromString(USER_ID);
        when(gameFactory.createGame(anyInt(), ArgumentMatchers.<Set<UUID>>any())).thenReturn(game);
        when(game.getId()).thenReturn(gameId);
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(userUuid, UUID.randomUUID()));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);
        when(gameDao.upsert(any(Game.class))).thenReturn(game);

        // When
        GameDto result = gameService.createGame(params, USER_ID);

        // Then
        assertThat(result).as("Le DTO ne doit pas être null").isNotNull();
        assertThat(result.id()).as("L'id doit correspondre au jeu créé").isEqualTo(gameId);
        assertThat(result.gameType()).as("Le type de jeu doit être tictactoe").isEqualTo("tictactoe");
        assertThat(result.status()).as("Le statut initial doit être ONGOING").isEqualTo("ONGOING");
        // Vérifie que l'userId est le PREMIER joueur passé au moteur (LinkedHashSet garantit l'ordre)
        verify(gameFactory).createGame(eq(3), argThat((Set<UUID> ids) ->
                ids.iterator().next().equals(userUuid)));
        verify(gameDao).upsert(game);
    }

    @Test
    void shouldRejectInvalidUserIdFormat() {
        // Un userId non-UUID doit lever une exception avant même d'appeler le moteur
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);

        assertThatThrownBy(() -> gameService.createGame(params, "not-a-uuid"))
                .as("Un userId non-UUID doit lever IllegalArgumentException")
                .isInstanceOf(IllegalArgumentException.class);
        // Vérifie que le moteur n'a JAMAIS été appelé
        verifyNoInteractions(gameFactory);
    }

    @Test
    void shouldValidateUserOnCreateGame() {
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        UUID userUuid = UUID.fromString(USER_ID);
        when(gameFactory.createGame(anyInt(), ArgumentMatchers.<Set<UUID>>any())).thenReturn(game);
        when(game.getId()).thenReturn(UUID.randomUUID());
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(userUuid));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);
        when(gameDao.upsert(any())).thenReturn(game);

        gameService.createGame(params, USER_ID);

        // Vérifie que la validation utilisateur est bien appelée
        verify(userValidator).validate(USER_ID);
    }

    @Test
    void shouldValidateUserOnListGames() {
        when(gameDao.findByPlayerId(USER_ID)).thenReturn(List.of());

        gameService.listGames(USER_ID);

        verify(userValidator).validate(USER_ID);
    }

    @Test
    void shouldThrowExceptionForUnknownGameType() {
        // Given
        GameCreationParams params = new GameCreationParams("unknown", 2, 3);

        // Then
        assertThatThrownBy(() -> gameService.createGame(params, USER_ID))
                .as("Un type de jeu inconnu doit lever une ResponseStatusException 400")
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rex = (ResponseStatusException) ex;
                    assertThat(rex.getStatusCode().value())
                            .as("Le code HTTP doit être 400 BAD_REQUEST")
                            .isEqualTo(400);
                    assertThat(rex.getReason())
                            .as("Le message doit mentionner le type de jeu inconnu")
                            .contains("unknown");
                });
        // Vérifie que le moteur n'a pas été appelé
        verifyNoInteractions(gameFactory);
    }

    @Test
    void shouldListGames() {
        // Given
        when(gameDao.findByPlayerId(USER_ID)).thenReturn(List.of(game));
        when(game.getId()).thenReturn(UUID.randomUUID());
        when(game.getFactoryId()).thenReturn("tictactoe");
        when(game.getPlayerIds()).thenReturn(Set.of(UUID.randomUUID()));
        when(game.getBoardSize()).thenReturn(3);
        when(game.getStatus()).thenReturn(fr.le_campus_numerique.square_games.engine.GameStatus.ONGOING);

        // When
        Collection<GameDto> result = gameService.listGames(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        verify(gameDao).findByPlayerId(USER_ID);
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
                .as("Un gameId inconnu doit lever une ResponseStatusException 404")
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rex = (ResponseStatusException) ex;
                    assertThat(rex.getStatusCode().value())
                            .as("Le code HTTP doit être 404 NOT_FOUND")
                            .isEqualTo(404);
                });
    }
}
