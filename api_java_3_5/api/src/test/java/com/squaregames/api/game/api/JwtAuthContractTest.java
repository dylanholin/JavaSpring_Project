package com.squaregames.api.game.api;

import com.squaregames.api.common.security.JwtService;
import com.squaregames.api.game.api.dto.GameCreationParams;
import com.squaregames.api.game.api.dto.GameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de contrat pour l'authentification JWT.
 *
 * Vérifie que :
 * - Un JWT valide permet d'accéder aux endpoints
 * - Un JWT invalide est rejeté (401)
 * - L'absence de JWT est rejetée (401)
 * - L'userId extrait du JWT est correctement utilisé
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JwtAuthContractTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JwtService jwtService;

    private static final String VALID_USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldAllowGameCreationWithValidJwt() {
        // Given — un JWT valide
        String token = jwtService.generateToken(VALID_USER_ID, "test@test.com", List.of("ROLE_USER"));

        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), GameDto.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Un JWT valide doit autoriser la création (200)")
                .isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(response.getBody()).currentPlayerId())
                .as("Le currentPlayerId doit être l'utilisateur du JWT")
                .isEqualTo(UUID.fromString(VALID_USER_ID));
    }

    @Test
    void shouldReturn401WithInvalidJwt() {
        // Given — un JWT invalide
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid.token.here");
        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Un JWT invalide doit être rejeté (401)")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturn401WithNoAuth() {
        // Given — aucun header Authorization
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Sans Authorization, la requête doit être rejetée (401)")
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
