package com.squaregames.api.game.application;

/**
 * Composant chargé de valider qu'un userId existe dans le service utilisateurs.
 * Séparé du service pour permettre le mock dans les tests.
 */
public interface UserValidator {

    /**
     * Vérifie que l'utilisateur existe.
     * @throws org.springframework.web.server.ResponseStatusException HTTP 403 si inconnu ou service inaccessible
     */
    void validate(String userId);
}
