package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.application.GameDao;
import fr.le_campus_numerique.square_games.engine.Game;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Implémentation en mémoire du DAO.
 * Utilise une HashMap pour stocker les parties.
 * Les données sont perdues au redémarrage de l'application.
 */
@Repository
public class InMemoryGameDao implements GameDao {

    private final Map<UUID, Game> games = new HashMap<>();

    @Override
    public Collection<Game> findAll() {
        return games.values();
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        return Optional.ofNullable(games.get(gameId));
    }

    @Override
    public Game upsert(Game game) {
        games.put(game.getId(), game);
        return game;
    }

    @Override
    public void delete(UUID gameId) {
        games.remove(gameId);
    }
}
