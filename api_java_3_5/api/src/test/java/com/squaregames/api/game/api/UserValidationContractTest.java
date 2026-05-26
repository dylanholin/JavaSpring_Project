package com.squaregames.api.game.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.squaregames.api.game.api.dto.GameCreationParams;
import com.squaregames.api.game.api.dto.GameDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de contrat entre api et user-api.
 *
 * Vérifie que les réponses de user-api sont correctement interprétées par api :
 * - user-api retourne true  → api autorise (200)
 * - user-api retourne false → api refuse (403)
 * - user-api est inaccessible → api refuse (403)
 *
 * WireMock simule user-api de façon déterministe — aucun démarrage réel de user-api requis.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock
class UserValidationContractTest {

    @InjectWireMock
    private WireMockServer wireMock;

    @Autowired
    private TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("user.service.url", () -> "http://localhost:${wiremock.server.port}");
    }

    private static final String VALID_USER_ID = UUID.randomUUID().toString();
    private static final String UNKNOWN_USER_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        wireMock.resetAll();
    }

    @Test
    void shouldAllowGameCreationWhenUserIsValid() {
        // Given — user-api confirme que l'utilisateur existe
        wireMock.stubFor(get(urlEqualTo("/users/" + VALID_USER_ID + "/valid"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("true")));

        // When
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", VALID_USER_ID);
        ResponseEntity<GameDto> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), GameDto.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Quand user-api retourne true, api doit autoriser la création (200)")
                .isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(response.getBody()).currentPlayerId())
                .as("Le currentPlayerId doit être l'utilisateur validé")
                .isEqualTo(UUID.fromString(VALID_USER_ID));

        // Vérifie que api a bien appelé user-api
        wireMock.verify(getRequestedFor(urlEqualTo("/users/" + VALID_USER_ID + "/valid")));
    }

    @Test
    void shouldReturn403WhenUserApiReturnsFalse() {
        // Given — user-api dit que l'utilisateur n'existe pas
        wireMock.stubFor(get(urlEqualTo("/users/" + UNKNOWN_USER_ID + "/valid"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("false")));

        // When
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", UNKNOWN_USER_ID);
        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Quand user-api retourne false, api doit refuser avec 403")
                .isEqualTo(HttpStatus.FORBIDDEN);

        wireMock.verify(getRequestedFor(urlEqualTo("/users/" + UNKNOWN_USER_ID + "/valid")));
    }

    @Test
    void shouldReturn403WhenUserApiIsDown() {
        // Given — user-api ne répond pas (aucun stub = connexion refusée)
        String userId = UUID.randomUUID().toString();

        // When
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", userId);

        // On stub une erreur réseau (503)
        wireMock.stubFor(get(urlEqualTo("/users/" + userId + "/valid"))
                .willReturn(aResponse().withStatus(503)));

        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Quand user-api est indisponible (503), api doit refuser avec 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldReturn403WhenUserApiReturns404() {
        // Given — user-api retourne 404 (endpoint inconnu ou service mal configuré)
        String userId = UUID.randomUUID().toString();
        wireMock.stubFor(get(urlEqualTo("/users/" + userId + "/valid"))
                .willReturn(aResponse().withStatus(404)));

        // When
        GameCreationParams params = new GameCreationParams("tictactoe", 2, 3);
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-UserId", userId);
        ResponseEntity<String> response = restTemplate.exchange(
                "/games", HttpMethod.POST,
                new HttpEntity<>(params, headers), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Quand user-api retourne 404, api doit refuser avec 403")
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
