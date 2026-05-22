package com.squaregames.api.game.api;

import com.squaregames.api.game.api.dto.CatalogEntryDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour le catalogue des jeux.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GameCatalogControllerTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldReturnCatalogInFrench() {
        // When
        ResponseEntity<Collection<CatalogEntryDto>> response = restTemplate.exchange(
                "/games/catalog",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {},
                Locale.FRENCH);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();

        // Vérifie qu'il y a au moins 3 jeux
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(3);

        // Vérifie que chaque entrée a un gameType et un name non null
        assertThat(response.getBody())
                .allSatisfy(entry -> {
                    assertThat(entry.gameType()).isNotNull();
                    assertThat(entry.name()).isNotNull();
                });
    }

    @Test
    void shouldReturnCatalogInEnglish() {
        // Given - ajouter header Accept-Language: en
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("Accept-Language", "en");
        var entity = new org.springframework.http.HttpEntity<>(headers);

        // When
        ResponseEntity<Collection<CatalogEntryDto>> response = restTemplate.exchange(
                "/games/catalog",
                HttpMethod.GET,
                entity,
                new ParameterizedTypeReference<>() {});

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isNotEmpty();
    }
}
