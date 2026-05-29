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
        factories.put("connect4", new ConnectFourGameFactory());
        factories.put("15 puzzle", new TaquinGameFactory());
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
        return repository.findById(gameId.toString())
                .map(this::convertToGame)
                .filter(game -> game != null && java.util.Objects.equals(game.getId(), gameId));
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
            List<TokenPosition<UUID>> normalizedOnBoard = onBoardTokens;
            if ("connect4".equals(entity.factoryId)) {
                normalizedOnBoard = ConnectFourStateAdapter.normalizePositions(onBoardTokens);
                // Le moteur exige que le joueur avec le plus de tokens soit à l'index 1
                // (assert counts[1] - counts[0] ∈ [0,1])
                playerIds = reorderPlayersForConnectFour(playerIds, normalizedOnBoard);
            }
            return factory.createGameWithIds(
                UUID.fromString(entity.id),
                entity.boardSize,
                playerIds,
                normalizedOnBoard,
                removedTokens
            );
        } catch (InconsistentGameDefinitionException e) {
            Game fallback = factory.createGame(entity.playerCount, entity.boardSize);
            UUID currentPlayer = entity.currentPlayerId != null ? UUID.fromString(entity.currentPlayerId) : fallback.getCurrentPlayerId();
            return new GameStateWrapper(fallback, UUID.fromString(entity.id), currentPlayer);
        } catch (Throwable e) {
            System.err.println("JpaGameDao.convertToGame fallback for " + entity.factoryId
                    + " id=" + entity.id + " error=" + e.getClass().getName() + " " + e.getMessage());
            Game fallback = factory.createGame(entity.playerCount, entity.boardSize);
            UUID currentPlayer = entity.currentPlayerId != null ? UUID.fromString(entity.currentPlayerId) : fallback.getCurrentPlayerId();
            return new GameStateWrapper(fallback, UUID.fromString(entity.id), currentPlayer);
        }
    }

    /**
     * Réorganise playerIds pour ConnectFour : le joueur avec le plus de tokens onBoard
     * doit être à l'index 1 (car le moteur vérifie counts[1] - counts[0] ∈ [0,1]).
     */
    private List<UUID> reorderPlayersForConnectFour(List<UUID> playerIds, List<TokenPosition<UUID>> onBoard) {
        if (playerIds.size() != 2 || onBoard.isEmpty()) {
            return playerIds;
        }
        Map<UUID, Integer> counts = new HashMap<>();
        for (TokenPosition<UUID> tp : onBoard) {
            counts.merge(tp.owner(), 1, Integer::sum);
        }
        UUID p0 = playerIds.get(0);
        UUID p1 = playerIds.get(1);
        int count0 = counts.getOrDefault(p0, 0);
        int count1 = counts.getOrDefault(p1, 0);
        if (count0 > count1) {
            // p0 a plus de tokens : le mettre à l'index 1
            return List.of(p1, p0);
        }
        return playerIds;
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
        entity.currentPlayerId = game.getCurrentPlayerId() != null ? game.getCurrentPlayerId().toString() : null;
        // Conserver l'ordre original des joueurs (important pour l'alternance des tours)
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
