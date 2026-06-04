package com.squaregames.api.game.api;

import com.squaregames.api.game.api.dto.*;
import com.squaregames.api.game.application.GameService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @PostMapping
    public GameDto createGame(@RequestBody GameCreationParams params) {
        return gameService.createGame(params, getCurrentUserId());
    }

    @GetMapping
    public Collection<GameDto> listGames() {
        return gameService.listGames(getCurrentUserId());
    }

    @GetMapping("/{gameId}")
    public GameDto getGame(@PathVariable UUID gameId) {
        return gameService.getGame(gameId);
    }

    @GetMapping("/{gameId}/moves")
    public List<TokenMovesDto> getPossibleMoves(@PathVariable UUID gameId) {
        return gameService.getPossibleMoves(gameId);
    }

    @PostMapping("/{gameId}/moves")
    public GameDto playMove(@PathVariable UUID gameId, @RequestBody MoveRequest move) {
        return gameService.playMove(gameId, move, getCurrentUserId());
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getName();
    }
}
