package com.squaregames.user.user.api;

import com.squaregames.user.user.api.dto.UserCreationRequest;
import com.squaregames.user.user.api.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Collection;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration pour l'API utilisateurs.
 * Démarre une vraie application Spring Boot — aucun mock sur la logique métier.
 * Ces tests servent de "golden master" pour user-api.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserControllerIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void shouldCreateUser() {
        // Given
        UserCreationRequest request = new UserCreationRequest("Alice", "alice@test.com");

        // When
        ResponseEntity<UserDto> response = restTemplate.postForEntity("/users", request, UserDto.class);

        // Then
        assertThat(response.getStatusCode())
                .as("La création d'un utilisateur doit retourner 201 CREATED")
                .isEqualTo(HttpStatus.CREATED);
        UserDto body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.id())
                .as("L'id doit être un UUID valide non null")
                .isNotNull();
        assertThat(body.name())
                .as("Le nom doit correspondre à celui envoyé")
                .isEqualTo("Alice");
        assertThat(body.email())
                .as("L'email doit correspondre à celui envoyé")
                .isEqualTo("alice@test.com");
        assertThat(body.createdAt())
                .as("La date de création doit être renseignée")
                .isNotNull();
    }

    @Test
    void shouldGetUserById() {
        // Given — on crée d'abord un utilisateur
        UserCreationRequest request = new UserCreationRequest("Bob", "bob@test.com");
        UserDto created = java.util.Objects.requireNonNull(
                restTemplate.postForEntity("/users", request, UserDto.class).getBody());

        // When
        ResponseEntity<UserDto> response = restTemplate.getForEntity(
                "/users/" + created.id(), UserDto.class);

        // Then
        assertThat(response.getStatusCode())
                .as("GET /users/{id} doit retourner 200 pour un utilisateur existant")
                .isEqualTo(HttpStatus.OK);
        UserDto body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.id())
                .as("L'id retourné doit correspondre à celui demandé")
                .isEqualTo(created.id());
        assertThat(body.email())
                .as("L'email retourné doit correspondre à celui créé")
                .isEqualTo("bob@test.com");
    }

    @Test
    void shouldReturn404ForUnknownUser() {
        // Given — UUID inexistant
        String unknownId = UUID.randomUUID().toString();

        // When
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/users/" + unknownId, String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("GET /users/{id} doit retourner 404 pour un utilisateur inconnu")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListAllUsers() {
        // Given — on crée deux utilisateurs
        restTemplate.postForEntity("/users", new UserCreationRequest("Charlie", "charlie@test.com"), UserDto.class);
        restTemplate.postForEntity("/users", new UserCreationRequest("Diana", "diana@test.com"), UserDto.class);

        // When
        ResponseEntity<Collection<UserDto>> response = restTemplate.exchange(
                "/users", HttpMethod.GET, null,
                new ParameterizedTypeReference<Collection<UserDto>>() {});

        // Then
        assertThat(response.getStatusCode())
                .as("GET /users doit retourner 200")
                .isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(response.getBody()))
                .as("La liste doit contenir au moins les utilisateurs créés")
                .isNotEmpty();
    }

    @Test
    void shouldDeleteUser() {
        // Given — on crée un utilisateur
        UserCreationRequest request = new UserCreationRequest("Eve", "eve@test.com");
        UserDto created = java.util.Objects.requireNonNull(
                restTemplate.postForEntity("/users", request, UserDto.class).getBody());

        // When — suppression
        restTemplate.delete("/users/" + created.id());

        // Then — il ne doit plus exister
        ResponseEntity<String> response = restTemplate.getForEntity(
                "/users/" + created.id(), String.class);
        assertThat(response.getStatusCode())
                .as("Après suppression, GET doit retourner 404")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldValidateExistingUser() {
        // Given — on crée un utilisateur
        UserCreationRequest request = new UserCreationRequest("Frank", "frank@test.com");
        UserDto created = java.util.Objects.requireNonNull(
                restTemplate.postForEntity("/users", request, UserDto.class).getBody());

        // When
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                "/users/" + created.id() + "/valid", Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("Un utilisateur existant doit être valide (true)")
                .isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidUser() {
        // Given — UUID inexistant
        String unknownId = UUID.randomUUID().toString();

        // When
        ResponseEntity<Boolean> response = restTemplate.getForEntity(
                "/users/" + unknownId + "/valid", Boolean.class);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .as("Un utilisateur inexistant doit retourner false")
                .isFalse();
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() {
        // Given — on crée un utilisateur avec un email
        restTemplate.postForEntity("/users",
                new UserCreationRequest("Grace", "grace@test.com"), UserDto.class);

        // When — on tente de créer un second utilisateur avec le même email
        ResponseEntity<String> response = restTemplate.postForEntity("/users",
                new UserCreationRequest("Grace2", "grace@test.com"), String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Un email déjà utilisé doit retourner 409 CONFLICT")
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturn400WhenNameIsBlank() {
        // Given — nom vide
        UserCreationRequest request = new UserCreationRequest("", "valid@test.com");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity("/users", request, String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Un nom vide doit retourner 400 BAD_REQUEST (validation @NotBlank)")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() {
        // Given — email malformé
        UserCreationRequest request = new UserCreationRequest("Henry", "pas-un-email");

        // When
        ResponseEntity<String> response = restTemplate.postForEntity("/users", request, String.class);

        // Then
        assertThat(response.getStatusCode())
                .as("Un email invalide doit retourner 400 BAD_REQUEST (validation @Email)")
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
