package com.squaregames.api.game.infrastructure;

import fr.le_campus_numerique.square_games.engine.*;

import java.util.*;

/**
 * Wrapper autour d'un Game pour forcer l'ID et le currentPlayerId.
 * Utilisé quand le moteur ne reconstruit pas exactement l'état original.
 */
public class GameStateWrapper implements Game {
    private final Game delegate;
    private final UUID id;
    private final UUID currentPlayerId;

    public GameStateWrapper(Game delegate, UUID id, UUID currentPlayerId) {
        this.delegate = delegate;
        this.id = id;
        this.currentPlayerId = currentPlayerId;
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public String getFactoryId() { return delegate.getFactoryId(); }

    @Override
    public Set<UUID> getPlayerIds() { return delegate.getPlayerIds(); }

    @Override
    public GameStatus getStatus() { return delegate.getStatus(); }

    @Override
    public UUID getCurrentPlayerId() { return currentPlayerId; }

    @Override
    public int getBoardSize() { return delegate.getBoardSize(); }

    @Override
    public Map<CellPosition, Token> getBoard() { return delegate.getBoard(); }

    @Override
    public Collection<Token> getRemainingTokens() { return delegate.getRemainingTokens(); }

    @Override
    public Collection<Token> getRemovedTokens() { return delegate.getRemovedTokens(); }
}
