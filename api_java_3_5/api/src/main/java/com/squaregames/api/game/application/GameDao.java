package com.squaregames.api.game.application;

import fr.le_campus_numerique.square_games.engine.Game;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object pour la persistance des parties.
 * Interface définissant les opérations CRUD sur les jeux,
 * indépendante de la technologie de stockage (mémoire, JDBC, JPA).
 */
public interface GameDao {

    /**
     * Récupère toutes les parties.
     */
    Collection<Game> findAll();

    /**
     * Récupère une partie par son identifiant.
     */
    Optional<Game> findById(UUID gameId);

    /**
     * Sauvegarde ou met à jour une partie.
     */
    Game upsert(Game game);

    /**
     * Supprime une partie.
     */
    void delete(UUID gameId);
}
