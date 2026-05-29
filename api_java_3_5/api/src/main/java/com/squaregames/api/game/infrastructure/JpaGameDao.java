package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.application.GameDao;
import com.squaregames.api.game.domain.GameEntity;
import com.squaregames.api.game.domain.GameEntityRepository;
import com.squaregames.api.game.domain.GameTokenEntity;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.InconsistentGameDefinitionException;
import fr.le_campus_numerique.square_games.engine.Token;
import fr.le_campus_numerique.square_games.engine.TokenPosition;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.util.*;

/**
 * Implémentation JPA du DAO utilisant Spring Data.
 * S'appuie sur GameEntityRepository pour la persistance.
 *
 * Reconstruit l'état complet d'une partie (playerIds + positions des tokens)
 * via {@link GameFactory#createGameWithIds} du moteur square-games-engine.
 */
@Primary
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
     * Reconstruit l'état complet (playerIds + positions des tokens).
     */
    private Game convertToGame(GameEntity entity) {
        GameFactory factory = factories.get(entity.factoryId);
        if (factory == null) {
            return null;
        }

        List<UUID> playerIds = new ArrayList<>();
        if (entity.playerIds != null && !entity.playerIds.isEmpty()) {
            for (String pid : entity.playerIds.split(",")) {
                playerIds.add(UUID.fromString(pid.trim()));
            }
        } else {
            for (int i = 0; i < entity.playerCount; i++) {
                playerIds.add(UUID.randomUUID());
            }
        }

        List<TokenPosition<UUID>> onBoardTokens = new ArrayList<>();
        List<TokenPosition<UUID>> removedTokens = new ArrayList<>();

        for (GameTokenEntity tokenEntity : entity.tokens) {
            UUID ownerId = tokenEntity.ownerId != null ? UUID.fromString(tokenEntity.ownerId) : null;
            int x = tokenEntity.xPosition != null ? tokenEntity.xPosition : -1;
            int y = tokenEntity.yPosition != null ? tokenEntity.yPosition : -1;
            TokenPosition<UUID> tp = new TokenPosition<>(ownerId, tokenEntity.tokenName, x, y);
            if (tokenEntity.isRemoved) {
                removedTokens.add(tp);
            } else if (tokenEntity.isOnBoard) {
                onBoardTokens.add(tp);
            }
        }

        try {
            return factory.createGameWithIds(
                UUID.fromString(entity.id),
                entity.boardSize,
                playerIds,
                onBoardTokens,
                removedTokens
            );
        } catch (InconsistentGameDefinitionException e) {
            return factory.createGame(entity.playerCount, entity.boardSize);
        }
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
        entity.playerIds = game.getPlayerIds().stream()
                .map(Object::toString)
                .collect(java.util.stream.Collectors.joining(","));

        // Conversion des tokens
        entity.tokens.clear();
        for (Token token : game.getBoard().values()) {
            GameTokenEntity tokenEntity = new GameTokenEntity();
            tokenEntity.game = entity;
            tokenEntity.tokenName = token.getName();
            tokenEntity.ownerId = token.getOwnerId().map(Object::toString).orElse(null);
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
            tokenEntity.ownerId = token.getOwnerId().map(Object::toString).orElse(null);
            tokenEntity.isOnBoard = false;
            tokenEntity.isRemoved = false;
            entity.tokens.add(tokenEntity);
        }

        // Tokens retirés
        for (Token token : game.getRemovedTokens()) {
            GameTokenEntity tokenEntity = new GameTokenEntity();
            tokenEntity.game = entity;
            tokenEntity.tokenName = token.getName();
            tokenEntity.ownerId = token.getOwnerId().map(Object::toString).orElse(null);
            tokenEntity.isOnBoard = false;
            tokenEntity.isRemoved = true;
            entity.tokens.add(tokenEntity);
        }

        return entity;
    }

    @Override
    public Collection<Game> findByPlayerId(String playerId) {
        List<Game> games = new ArrayList<>();
        for (GameEntity entity : repository.findAll()) {
            if (entity.playerIds != null && entity.playerIds.contains(playerId)) {
                Game game = convertToGame(entity);
                if (game != null) {
                    games.add(game);
                }
            }
        }
        return games;
    }
}
