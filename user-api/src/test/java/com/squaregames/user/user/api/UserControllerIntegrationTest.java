package com.squaregames.user.user.api;

import com.squaregames.user.auth.api.LoginRequest;
import com.squaregames.user.auth.api.LoginResponse;
import com.squaregames.user.user.api.dto.UserCreationRequest;
import com.squaregames.user.user.api.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.Collection;

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

    private UserDto createUser(String name, String email, String password, String role) {
        UserCreationRequest request = new UserCreationRequest(name, email, password, role);
        ResponseEntity<UserDto> response = restTemplate.postForEntity("/users", request, UserDto.class);
        assertThat(response.getStatusCode())
                .as("La création d'un utilisateur doit retourner 201 CREATED")
                .isEqualTo(HttpStatus.CREATED);
        return java.util.Objects.requireNonNull(response.getBody());
    }

    private String loginAndGetToken(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login", loginRequest, LoginResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return java.util.Objects.requireNonNull(response.getBody()).token();
    }

    private HttpHeaders authHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return headers;
    }

    @Test
    void shouldCreateUser() {
        UserCreationRequest request = new UserCreationRequest("Alice", "alice@test.com", "password123", null);

        ResponseEntity<UserDto> response = restTemplate.postForEntity("/users", request, UserDto.class);

        assertThat(response.getStatusCode())
                .as("La création d'un utilisateur doit retourner 201 CREATED")
                .isEqualTo(HttpStatus.CREATED);
        UserDto body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.id()).isNotNull();
        assertThat(body.name()).isEqualTo("Alice");
        assertThat(body.email()).isEqualTo("alice@test.com");
        assertThat(body.role()).isEqualTo("ROLE_USER");
        assertThat(body.createdAt()).isNotNull();
    }

    @Test
    void shouldLoginAndGetJwt() {
        createUser("LoginTest", "login@test.com", "secret123", null);

        LoginRequest loginRequest = new LoginRequest("login@test.com", "secret123");
        ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                "/auth/login", loginRequest, LoginResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        LoginResponse body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.token()).isNotNull().isNotEmpty();
        assertThat(body.email()).isEqualTo("login@test.com");
        assertThat(body.role()).isEqualTo("ROLE_USER");
    }

    @Test
    void shouldReturn401ForInvalidCredentials() {
        createUser("BadLogin", "badlogin@test.com", "correct123", null);

        LoginRequest loginRequest = new LoginRequest("badlogin@test.com", "wrongpassword");
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/auth/login", loginRequest, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldGetUserByIdWithJwt() {
        UserDto created = createUser("Bob", "bob@test.com", "pass123", null);
        String token = loginAndGetToken("bob@test.com", "pass123");

        ResponseEntity<UserDto> response = restTemplate.exchange(
                "/users/" + created.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), UserDto.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        UserDto body = java.util.Objects.requireNonNull(response.getBody());
        assertThat(body.id()).isEqualTo(created.id());
        assertThat(body.email()).isEqualTo("bob@test.com");
    }

    @Test
    void shouldReturn404ForUnknownUser() {
        String unknownId = java.util.UUID.randomUUID().toString();
        createUser("ForAuth", "forauth@test.com", "pass123", null);
        String token = loginAndGetToken("forauth@test.com", "pass123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/" + unknownId, HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListAllUsersAsAdmin() {
        createUser("AdminCharlie", "admin@test.com", "admin123", "ROLE_ADMIN");
        createUser("NormalDiana", "normal@test.com", "normal123", "ROLE_USER");
        String adminToken = loginAndGetToken("admin@test.com", "admin123");

        ResponseEntity<Collection<UserDto>> response = restTemplate.exchange(
                "/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)),
                new ParameterizedTypeReference<Collection<UserDto>>() {});

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(java.util.Objects.requireNonNull(response.getBody())).isNotEmpty();
    }

    @Test
    void shouldForbidListUsersForNonAdmin() {
        createUser("NormalEve", "normal2@test.com", "pass123", "ROLE_USER");
        String userToken = loginAndGetToken("normal2@test.com", "pass123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users", HttpMethod.GET,
                new HttpEntity<>(authHeaders(userToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldDeleteUserAsAdmin() {
        UserDto toDelete = createUser("DeleteMe", "delete@test.com", "pass123", "ROLE_USER");
        createUser("Admin2", "admin2@test.com", "admin123", "ROLE_ADMIN");
        String adminToken = loginAndGetToken("admin2@test.com", "admin123");

        ResponseEntity<Void> deleteResponse = restTemplate.exchange(
                "/users/" + toDelete.id(), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(adminToken)), Void.class);
        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<String> getResponse = restTemplate.exchange(
                "/users/" + toDelete.id(), HttpMethod.GET,
                new HttpEntity<>(authHeaders(adminToken)), String.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldForbidDeleteUserForNonAdmin() {
        UserDto user = createUser("NoDelete", "nodelete@test.com", "pass123", "ROLE_USER");
        String userToken = loginAndGetToken("nodelete@test.com", "pass123");

        ResponseEntity<String> response = restTemplate.exchange(
                "/users/" + user.id(), HttpMethod.DELETE,
                new HttpEntity<>(authHeaders(userToken)), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldValidateExistingUser() {
        UserDto created = createUser("Frank", "frank@test.com", "pass123", null);
        String token = loginAndGetToken("frank@test.com", "pass123");

        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/users/" + created.id() + "/valid", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Boolean.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidUser() {
        String unknownId = java.util.UUID.randomUUID().toString();
        createUser("ForAuth2", "forauth2@test.com", "pass123", null);
        String token = loginAndGetToken("forauth2@test.com", "pass123");

        ResponseEntity<Boolean> response = restTemplate.exchange(
                "/users/" + unknownId + "/valid", HttpMethod.GET,
                new HttpEntity<>(authHeaders(token)), Boolean.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isFalse();
    }

    @Test
    void shouldReturn409WhenEmailAlreadyExists() {
        restTemplate.postForEntity("/users",
                new UserCreationRequest("Grace", "grace@test.com", "pass123", null), UserDto.class);

        ResponseEntity<String> response = restTemplate.postForEntity("/users",
                new UserCreationRequest("Grace2", "grace@test.com", "pass456", null), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void shouldReturn400WhenNameIsBlank() {
        UserCreationRequest request = new UserCreationRequest("", "valid@test.com", "pass123", null);

        ResponseEntity<String> response = restTemplate.postForEntity("/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() {
        UserCreationRequest request = new UserCreationRequest("Henry", "pas-un-email", "pass123", null);

        ResponseEntity<String> response = restTemplate.postForEntity("/users", request, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
