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
        GameDto body = java.util.Objects.requireNonNull(response.getBody());
        createdGameId = body.id();
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
        assertThat(response.getStatusCode())
                .as("La création de partie doit retourner 200")
                .isEqualTo(HttpStatus.OK);
        GameDto body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.id()).as("L'id de la partie doit être un UUID non null").isNotNull();
        assertThat(body.gameType()).as("Le type de jeu doit être tictactoe").isEqualTo("tictactoe");
        assertThat(body.playerCount()).as("Le nombre de joueurs doit être 2").isEqualTo(2);
        assertThat(body.boardSize()).as("La taille du plateau doit être 3").isEqualTo(3);
        assertThat(body.status()).as("Le statut initial doit être ONGOING").isEqualTo("ONGOING");
        assertThat(body.currentPlayerId())
                .as("Le premier joueur (currentPlayerId) doit être l'utilisateur qui a créé la partie (X-UserId)")
                .isEqualTo(UUID.fromString(TEST_USER_ID));
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
        assertThat(response.getStatusCode())
                .as("La liste des parties doit retourner 200")
                .isEqualTo(HttpStatus.OK);
        Collection<GameDto> games = java.util.Objects.requireNonNull(response.getBody());
        assertThat(games)
                .as("La liste doit contenir au moins la partie créée dans setUp")
                .isNotEmpty();
        assertThat(games.stream().map(GameDto::id).toList())
                .as("La partie créée dans setUp doit être présente dans la liste")
                .contains(createdGameId);
    }

    @Test
    void shouldGetGameById() {
        // When
        ResponseEntity<GameDto> response = restTemplate.getForEntity(
                "/games/" + createdGameId, GameDto.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        GameDto gameBody = java.util.Objects.requireNonNull(response.getBody());
        assertThat(gameBody.id()).isEqualTo(createdGameId);
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
        Collection<TokenMovesDto> movesBody = java.util.Objects.requireNonNull(response.getBody());
        assertThat(movesBody).isNotEmpty();

        // Vérifie qu'il y a des tokens avec des mouvements possibles
        List<TokenMovesDto> tokens = movesBody.stream().toList();
        assertThat(tokens).anyMatch(token -> !token.allowedMoves().isEmpty());
    }

    @Test
    void shouldPlayMove() {
        // Le currentPlayerId doit être TEST_USER_ID (créateur de la partie)
        GameDto gameState = java.util.Objects.requireNonNull(
                restTemplate.getForEntity("/games/" + createdGameId, GameDto.class).getBody());
        assertThat(gameState.currentPlayerId())
                .as("Le premier joueur doit être l'utilisateur qui a créé la partie")
                .isEqualTo(UUID.fromString(TEST_USER_ID));
        String currentPlayerId = gameState.currentPlayerId().toString();

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
        assertThat(response.getStatusCode())
                .as("Jouer un coup valide doit retourner 200")
                .isEqualTo(HttpStatus.OK);
        GameDto moveBody = java.util.Objects.requireNonNull(response.getBody());
        assertThat(moveBody.id()).as("L'id de la partie ne doit pas changer").isEqualTo(createdGameId);
        assertThat(moveBody.status()).as("La partie doit rester ONGOING ou passer à FINISHED").isIn("ONGOING", "FINISHED");
        assertThat(moveBody.currentPlayerId())
                .as("Après un coup, c'est au tour de l'autre joueur")
                .isNotEqualTo(UUID.fromString(TEST_USER_ID));
    }

    @Test
    void shouldReturn400ForInvalidMove() {
        // Récupère le currentPlayerId depuis l'état du jeu
        ResponseEntity<GameDto> gameState = restTemplate.getForEntity(
                "/games/" + createdGameId, GameDto.class);
        String currentPlayerId = java.util.Objects.requireNonNull(gameState.getBody()).currentPlayerId().toString();

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
    void shouldReturn403WhenWrongPlayerTriesToPlay() {
        // Un userId différent du currentPlayerId doit recevoir 403
        String wrongUserId = UUID.randomUUID().toString();
        MoveRequest move = new MoveRequest("X", 0, 0);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-UserId", wrongUserId);
        HttpEntity<MoveRequest> request = new HttpEntity<>(move, headers);

        ResponseEntity<String> response = restTemplate.exchange(
                "/games/" + createdGameId + "/moves",
                HttpMethod.POST,
                request,
                String.class);

        assertThat(response.getStatusCode())
                .as("Un joueur qui n'est pas currentPlayerId doit recevoir 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody())
                .as("Le message d'erreur doit indiquer la cause")
                .contains("403");
    }

    @Test
    void shouldReturn400WhenXUserIdHeaderMissingOnCreate() {
        // Sans header X-UserId, Spring doit retourner 400
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpEntity<GameCreationParams> entity = new HttpEntity<>(params);

        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST, entity, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenXUserIdHeaderMissingOnPlayMove() {
        // Sans header X-UserId, Spring doit retourner 400
        MoveRequest move = new MoveRequest("X", 0, 0);
        HttpEntity<MoveRequest> entity = new HttpEntity<>(move);

        ResponseEntity<String> response = restTemplate.exchange(
                "/games/" + createdGameId + "/moves",
                HttpMethod.POST,
                entity,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    /**
     * Joue une partie TicTacToe complète jusqu'à TERMINATED avec le vrai moteur.
     * Aucun mock sur la logique du jeu — validation indépendante de l'implémentation.
     *
     * Séquence gagnante pour le joueur A (token "X") sur plateau 3x3 :
     *   X:(0,0) → O:(0,1) → X:(1,0) → O:(1,1) → X:(2,0)
     * → X aligne la ligne y=0 (cases (0,0),(1,0),(2,0)) et gagne.
     * CellPosition(x,y) : x = colonne, y = ligne.
     */
    @Test
    void shouldCompleteFullTicTacToeGame() {
        // --- Création de la partie ---
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", TEST_USER_ID);
        GameDto game = java.util.Objects.requireNonNull(
                restTemplate.exchange("/games", HttpMethod.POST,
                        new HttpEntity<>(params, headers), GameDto.class).getBody());

        UUID gameId = game.id();
        assertThat(game.currentPlayerId())
                .as("Le créateur (TEST_USER_ID) doit être le premier joueur")
                .isEqualTo(UUID.fromString(TEST_USER_ID));
        assertThat(game.status()).isEqualTo("ONGOING");

        // On lit le nom du token disponible pour chaque joueur via /moves
        // (évite de coder en dur "X"/"0" qui dépend de l'ordre interne du moteur)
        String tokenA = getFirstAvailableToken(gameId);

        // --- Coup 1 : joueur A joue son token en (0,0) ---
        GameDto after1 = playMove(gameId, TEST_USER_ID, tokenA, 0, 0);
        assertThat(after1.status()).as("Après le coup 1, la partie doit être ONGOING").isEqualTo("ONGOING");
        String playerB = after1.currentPlayerId().toString();
        assertThat(playerB)
                .as("Après le coup du joueur A, c'est au joueur B")
                .isNotEqualTo(TEST_USER_ID);

        String tokenB = getFirstAvailableToken(gameId);

        // --- Coup 2 : joueur B joue son token en (0,1) ---
        GameDto after2 = playMove(gameId, playerB, tokenB, 0, 1);
        assertThat(after2.status()).as("Après le coup 2, la partie doit être ONGOING").isEqualTo("ONGOING");
        assertThat(after2.currentPlayerId().toString())
                .as("Après le coup du joueur B, c'est au joueur A")
                .isEqualTo(TEST_USER_ID);

        // --- Coup 3 : joueur A joue son token en (1,0) ---
        GameDto after3 = playMove(gameId, TEST_USER_ID, getFirstAvailableToken(gameId), 1, 0);
        assertThat(after3.status()).as("Après le coup 3, la partie doit être ONGOING").isEqualTo("ONGOING");

        // --- Coup 4 : joueur B joue son token en (1,1) ---
        GameDto after4 = playMove(gameId, playerB, getFirstAvailableToken(gameId), 1, 1);
        assertThat(after4.status()).as("Après le coup 4, la partie doit être ONGOING").isEqualTo("ONGOING");

        // --- Coup 5 : joueur A joue son token en (2,0) → aligne ligne y=0 → victoire ---
        GameDto after5 = playMove(gameId, TEST_USER_ID, getFirstAvailableToken(gameId), 2, 0);
        assertThat(after5.status())
                .as("Après le 5ème coup (alignement ligne y=0), la partie doit être TERMINATED")
                .isEqualTo("TERMINATED");
        assertThat(after5.currentPlayerId())
                .as("Quand TERMINATED, currentPlayerId == ID du gagnant (joueur A = TEST_USER_ID)")
                .isEqualTo(UUID.fromString(TEST_USER_ID));

        // --- Vérification de l'état final via GET ---
        GameDto finalState = java.util.Objects.requireNonNull(
                restTemplate.getForEntity("/games/" + gameId, GameDto.class).getBody());
        assertThat(finalState.status())
                .as("L'état final en base doit être TERMINATED")
                .isEqualTo("TERMINATED");
        assertThat(finalState.currentPlayerId())
                .as("L'état final en base doit indiquer TEST_USER_ID comme gagnant")
                .isEqualTo(UUID.fromString(TEST_USER_ID));
    }

    /** Méthode utilitaire : retourne le nom du premier token ayant des coups disponibles. */
    private String getFirstAvailableToken(UUID gameId) {
        Collection<TokenMovesDto> moves = java.util.Objects.requireNonNull(
                restTemplate.exchange(
                        "/games/" + gameId + "/moves",
                        HttpMethod.GET, null,
                        new ParameterizedTypeReference<Collection<TokenMovesDto>>() {}).getBody());
        return moves.stream()
                .filter(t -> !t.allowedMoves().isEmpty())
                .map(TokenMovesDto::tokenName)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Aucun token disponible pour la partie " + gameId));
    }

    /** Méthode utilitaire : joue un coup et retourne le GameDto résultant. */
    private GameDto playMove(UUID gameId, String userId, String tokenName, int row, int col) {
        MoveRequest move = new MoveRequest(tokenName, row, col);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-UserId", userId);
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games/" + gameId + "/moves",
                HttpMethod.POST,
                new HttpEntity<>(move, headers),
                GameDto.class);
        assertThat(response.getStatusCode())
                .as("Le coup (%s en %d,%d) par %s doit retourner 200".formatted(tokenName, row, col, userId))
                .isEqualTo(HttpStatus.OK);
        return java.util.Objects.requireNonNull(response.getBody());
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
        assertThat(java.util.Objects.requireNonNull(responseCF.getBody()).gameType()).isEqualTo("connect4");

        // Test Taquin — l'ID moteur est "15 puzzle"
        GameCreationParams taquin = new GameCreationParams("15 puzzle", 1, 4);
        ResponseEntity<GameDto> responseTaquin = restTemplate.exchange(
                "/games", HttpMethod.POST, new HttpEntity<>(taquin, headers), GameDto.class);
        assertThat(responseTaquin.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(responseTaquin.getBody()).gameType()).isEqualTo("15 puzzle");
    }
}
