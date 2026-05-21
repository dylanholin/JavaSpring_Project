package com.squaregames.api.game;

public record GameCreationParams(
        String gameType,
        int playerCount,
        int boardSize
) {}
