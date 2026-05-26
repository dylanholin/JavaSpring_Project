package com.squaregames.api.game.api;

import com.squaregames.api.game.api.dto.GameCreationParams;
import com.squaregames.api.game.api.dto.GameDto;
import com.squaregames.api.game.api.dto.MoveRequest;
import com.squaregames.api.game.api.dto.TokenMovesDto;
import com.squaregames.api.game.application.UserValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;

/**
 * Tests d'intégration pour l'API Game.
 * Ces tests servent de "golden master" : ils vérifient que le comportement
 * de l'API reste identique après les refactorings (DAO, JDBC, JPA).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private UserValidator userValidator;

    private UUID createdGameId;
    private static final String TEST_USER_ID = "00000000-0000-0000-0000-000000000001";

    @BeforeEach
    void setUp() {
        doNothing().when(userValidator).validate(anyString());

        // Créer une partie avant chaque test
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", TEST_USER_ID);
        HttpEntity<GameCreationParams> entity = new HttpEntity<>(params, headers);
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games", HttpMethod.POST, entity, GameDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        createdGameId = response.getBody().id();
    }

    @Test
    void shouldCreateGame() {
        // Given
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", TEST_USER_ID);
        HttpEntity<GameCreationParams> entity = new HttpEntity<>(params, headers);

        // When
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games", HttpMethod.POST, entity, GameDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isNotNull();
        assertThat(response.getBody().gameType()).isEqualTo("tictactoe");
        assertThat(response.getBody().playerCount()).isEqualTo(2);
        assertThat(response.getBody().boardSize()).isEqualTo(3);
        assertThat(response.getBody().status()).isEqualTo("ONGOING");
    }

    @Test
    void shouldListGames() {
        // When
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", TEST_USER_ID);
        ResponseEntity<Collection<GameDto>> response = restTemplate.exchange(
                "/games",
                HttpMethod.GET,
                new HttpEntity<>(headers),
                new ParameterizedTypeReference<>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    void shouldGetGameById() {
        // When
        ResponseEntity<GameDto> response = restTemplate.getForEntity(
                "/games/" + createdGameId, GameDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(createdGameId);
    }

    @Test
    void shouldReturn404ForUnknownGame() {
        // Given
        UUID unknownId = UUID.randomUUID();

        // When
        ResponseEntity<GameDto> response = restTemplate.getForEntity(
                "/games/" + unknownId, GameDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldGetPossibleMoves() {
        // When
        ResponseEntity<Collection<TokenMovesDto>> response = restTemplate.exchange(
                "/games/" + createdGameId + "/moves",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();

        // Vérifie qu'il y a des tokens avec des mouvements possibles
        List<TokenMovesDto> tokens = response.getBody().stream().toList();
        assertThat(tokens).anyMatch(token -> !token.allowedMoves().isEmpty());
    }

    @Test
    void shouldPlayMove() {
        // Récupère le currentPlayerId depuis l'état du jeu
        ResponseEntity<GameDto> gameState = restTemplate.getForEntity(
                "/games/" + createdGameId, GameDto.class);
        String currentPlayerId = gameState.getBody().currentPlayerId().toString();

        // Given
        MoveRequest move = new MoveRequest("X", 0, 0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-UserId", currentPlayerId);
        HttpEntity<MoveRequest> request = new HttpEntity<>(move, headers);

        // When
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games/" + createdGameId + "/moves",
                HttpMethod.POST,
                request,
                GameDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(createdGameId);
    }

    @Test
    void shouldReturn400ForInvalidMove() {
        // Récupère le currentPlayerId depuis l'état du jeu
        ResponseEntity<GameDto> gameState = restTemplate.getForEntity(
                "/games/" + createdGameId, GameDto.class);
        String currentPlayerId = gameState.getBody().currentPlayerId().toString();

        // Given - token inexistant
        MoveRequest move = new MoveRequest("INVALID_TOKEN", 0, 0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-UserId", currentPlayerId);
        HttpEntity<MoveRequest> request = new HttpEntity<>(move, headers);

        // When
        ResponseEntity<String> response = restTemplate.exchange(
                "/games/" + createdGameId + "/moves",
                HttpMethod.POST,
                request,
                String.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldCreateDifferentGameTypes() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", TEST_USER_ID);

        // Test ConnectFour — l'ID moteur est "connect4"
        GameCreationParams connectFour = new GameCreationParams("connect4", 2, 7);
        ResponseEntity<GameDto> responseCF = restTemplate.exchange(
                "/games", HttpMethod.POST, new HttpEntity<>(connectFour, headers), GameDto.class);
        assertThat(responseCF.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseCF.getBody().gameType()).isEqualTo("connect4");

        // Test Taquin — l'ID moteur est "15 puzzle"
        GameCreationParams taquin = new GameCreationParams("15 puzzle", 1, 4);
        ResponseEntity<GameDto> responseTaquin = restTemplate.exchange(
                "/games", HttpMethod.POST, new HttpEntity<>(taquin, headers), GameDto.class);
        assertThat(responseTaquin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseTaquin.getBody().gameType()).isEqualTo("15 puzzle");
    }
}
