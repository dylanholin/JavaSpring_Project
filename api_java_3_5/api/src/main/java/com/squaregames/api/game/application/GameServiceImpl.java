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
    public GameDto createGame(GameCreationParams params) {
        GamePlugin plugin = plugins.stream()
                .filter(p -> p.getGameType().equals(params.gameType()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Type de jeu inconnu : " + params.gameType()));

        Game game = plugin.getFactory().createGame(params.playerCount(), params.boardSize());
        gameDao.upsert(game);
        return toDto(game);
    }

    @Override
    public Collection<GameDto> listGames() {
        return gameDao.findAll().stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public GameDto getGame(UUID gameId) {
        Game game = gameDao.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Partie introuvable : " + gameId));
        return toDto(game);
    }

    @Override
    public List<TokenMovesDto> getPossibleMoves(UUID gameId) {
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
    }

    @Override
    public GameDto playMove(UUID gameId, MoveRequest move) {
        Game game = findGame(gameId);
        Token token = Stream.concat(
                game.getBoard().values().stream(),
                game.getRemainingTokens().stream()
        ).filter(t -> t.getName().equals(move.tokenName()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token introuvable : " + move.tokenName()));

        try {
            token.moveTo(new CellPosition(move.row(), move.col()));
        } catch (InvalidPositionException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
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
                game.getStatus().name()
        );
    }
}
