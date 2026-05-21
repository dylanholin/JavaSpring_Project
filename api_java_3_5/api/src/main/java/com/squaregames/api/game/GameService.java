package com.squaregames.api.game;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface GameService {
    GameDto createGame(GameCreationParams params);
    Collection<GameDto> listGames();
    GameDto getGame(UUID gameId);
    List<TokenMovesDto> getPossibleMoves(UUID gameId);
    GameDto playMove(UUID gameId, MoveRequest move);
}
