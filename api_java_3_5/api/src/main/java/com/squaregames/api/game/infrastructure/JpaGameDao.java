package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.application.GameDao;
import com.squaregames.api.game.domain.GameEntity;
import com.squaregames.api.game.domain.GameEntityRepository;
import com.squaregames.api.game.domain.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Implémentation JPA du DAO utilisant Spring Data.
 * S'appuie sur GameEntityRepository pour la persistance.
 *
 * ⚠️ Limitation : le moteur de jeu ne permet pas de reconstruire un Game
 * complet depuis la base. Cette implémentation stocke les métadonnées.
 */
@Repository
public class JpaGameDao implements GameDao {

    private final GameEntityRepository repository;

    // Factories pour recréer des jeux
    private final Map<String, GameFactory> factories = new HashMap<>();

    public JpaGameDao(GameEntityRepository repository) {
        this.repository = repository;
        factories.put("tictactoe", new TicTacToeGameFactory());
        factories.put("connectfour", new ConnectFourGameFactory());
        factories.put("taquin", new TaquinGameFactory());
    }

    @Override
    public Collection<Game> findAll() {
        List<Game> games = new ArrayList<>();
        for (GameEntity entity : repository.findAll()) {
            Game game = convertToGame(entity);
            if (game != null) {
                games.add(game);
            }
        }
        return games;
    }

    @Override
    @SuppressWarnings("null")
    public Optional<Game> findById(UUID gameId) {
        Optional<GameEntity> entity = repository.findById(gameId.toString());
        return entity.map(this::convertToGame).filter(Objects::nonNull);
    }

    @Override
    @SuppressWarnings("null")
    public Game upsert(Game game) {
        GameEntity entity = convertToEntity(game);
        repository.save(entity);
        return game;
    }

    @Override
    @SuppressWarnings("null")
    public void delete(UUID gameId) {
        repository.deleteById(gameId.toString());
    }

    /**
     * Convertit une entité JPA vers un objet Game du moteur.
     * Crée un nouveau jeu avec la factory — l'état précédent est perdu.
     */
    private Game convertToGame(GameEntity entity) {
        GameFactory factory = factories.get(entity.factoryId);
        if (factory == null) {
            return null;
        }

        // Crée un nouveau jeu (l'état précédent est perdu — limitation du moteur)
        return factory.createGame(entity.playerCount, entity.boardSize);
    }

    /**
     * Convertit un objet Game du moteur vers une entité JPA.
     */
    private GameEntity convertToEntity(Game game) {
        GameEntity entity = new GameEntity();
        entity.id = game.getId().toString();
        entity.factoryId = game.getFactoryId();
        entity.boardSize = game.getBoardSize();
        entity.playerCount = game.getPlayerIds().size();
        entity.status = game.getStatus().name();

        // Conversion des tokens
        entity.tokens.clear();
        for (Token token : game.getBoard().values()) {
            GameTokenEntity tokenEntity = new GameTokenEntity();
            tokenEntity.game = entity;
            tokenEntity.tokenName = token.getName();
            tokenEntity.ownerId = token.getOwnerId() != null ? token.getOwnerId().toString() : null;
            tokenEntity.isOnBoard = true;
            tokenEntity.isRemoved = false;

            CellPosition pos = token.getPosition();
            if (pos != null) {
                tokenEntity.xPosition = pos.x();
                tokenEntity.yPosition = pos.y();
            }

            entity.tokens.add(tokenEntity);
        }

        // Tokens restants (non placés)
        for (Token token : game.getRemainingTokens()) {
            GameTokenEntity tokenEntity = new GameTokenEntity();
            tokenEntity.game = entity;
            tokenEntity.tokenName = token.getName();
            tokenEntity.ownerId = token.getOwnerId() != null ? token.getOwnerId().toString() : null;
            tokenEntity.isOnBoard = false;
            tokenEntity.isRemoved = false;
            entity.tokens.add(tokenEntity);
        }

        return entity;
    }

    @Override
    public Collection<Game> findByPlayerId(String playerId) {
        // ⚠️ Limitation : le moteur JPA ne stocke pas les playerIds dans les entités
        // Même comportement que findAll pour cette implémentation
        return findAll();
    }
}
