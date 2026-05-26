package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.application.UserValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Implémentation de UserValidator qui appelle l'API user-api via RestClient.
 */
@Component
public class RestUserValidator implements UserValidator {

    private final RestClient restClient;
    private final String userServiceUrl;

    public RestUserValidator(RestClient.Builder restClientBuilder,
                             @Value("${user.service.url}") String userServiceUrl) {
        this.restClient = restClientBuilder.build();
        this.userServiceUrl = userServiceUrl;
    }

    @Override
    public void validate(String userId) {
        try {
            Boolean valid = restClient.get()
                    .uri(userServiceUrl + "/users/{id}/valid", userId)
                    .retrieve()
                    .body(Boolean.class);
            if (!Boolean.TRUE.equals(valid)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur inconnu : " + userId);
            }
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Service utilisateurs inaccessible. Vérifiez que user-api est démarré.");
        }
    }
}
