package com.squaregames.api.game.application;

import com.squaregames.api.game.api.dto.*;
import fr.le_campus_numerique.square_games.engine.CellPosition;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.InvalidPositionException;
import fr.le_campus_numerique.square_games.engine.Token;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Stream;

@Service
public class GameServiceImpl implements GameService {

    private final GameDao gameDao;
    private final List<GamePlugin> plugins;

    public GameServiceImpl(GameDao gameDao, List<GamePlugin> plugins) {
        this.gameDao = gameDao;
        this.plugins = plugins;
    }

    @Override
    public GameDto createGame(GameCreationParams params, String userId) {
        GamePlugin plugin = plugins.stream()
                .filter(p -> p.getGameType().equals(params.gameType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de jeu inconnu : " + params.gameType()));

        UUID userUuid = UUID.fromString(userId);
        Set<UUID> playerIds = new LinkedHashSet<>();
        playerIds.add(userUuid);
        for (int i = 1; i < params.playerCount(); i++) {
            playerIds.add(UUID.randomUUID());
        }
        Game game = plugin.getFactory().createGame(params.boardSize(), playerIds);
        gameDao.upsert(game);
        return toDto(game);
    }

    @Override
    public Collection<GameDto> listGames(String userId) {
        return gameDao.findByPlayerId(userId).stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public GameDto getGame(UUID gameId) {
        Game game = gameDao.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable : " + gameId));
        try {
            return toDto(game);
        } catch (Exception e) {
            System.err.println("GameServiceImpl.getGame toDto ERROR: " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la conversion du jeu", e);
        }
    }

    @Override
    public List<TokenMovesDto> getPossibleMoves(UUID gameId) {
        try {
            Game game = findGame(gameId);
            return Stream.concat(
                    game.getBoard().values().stream(),
                    game.getRemainingTokens().stream()
            ).map(token -> {
                CellPosition pos = token.getPosition();
                List<PositionDto> moves = token.getAllowedMoves().stream()
                        .map(p -> new PositionDto(p.x(), p.y()))
                        .toList();
                return new TokenMovesDto(
                        token.getName(),
                        pos != null ? pos.x() : -1,
                        pos != null ? pos.y() : -1,
                        moves
                );
            }).toList();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Throwable e) {
            System.err.println("getPossibleMoves ERROR: " + e.getClass().getName() + " " + e.getMessage());
            e.printStackTrace();
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur calcul coups", e);
        }
    }

    @Override
    public GameDto playMove(UUID gameId, MoveRequest move, String userId) {
        Game game = findGame(gameId);

        // Vérifie que c'est bien le tour du joueur qui envoie la requête
        UUID currentPlayerId = game.getCurrentPlayerId();
        if (currentPlayerId != null && !currentPlayerId.toString().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce n'est pas votre tour. Joueur courant : " + currentPlayerId);
        }

        Token token = game.getRemainingTokens().stream()
                .filter(t -> t.getName().equals(move.tokenName()))
                .findFirst()
                .or(() -> game.getBoard().values().stream()
                        .filter(t -> t.getName().equals(move.tokenName()))
                        .findFirst())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token introuvable : " + move.tokenName()));

        try {
            token.moveTo(new CellPosition(move.row(), move.col()));
        } catch (InvalidPositionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Coup invalide : " + e.getMessage());
        }
        gameDao.upsert(game);
        return toDto(game);
    }

    private Game findGame(UUID gameId) {
        return gameDao.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable : " + gameId));
    }

    private GameDto toDto(Game game) {
        return new GameDto(
                game.getId(),
                game.getFactoryId(),
                game.getPlayerIds().size(),
                game.getBoardSize(),
                game.getStatus().name(),
                game.getCurrentPlayerId()
        );
    }
}
