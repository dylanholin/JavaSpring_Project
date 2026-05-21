package com.squaregames.api.game.api.dto;

public record GameCreationParams(
        String gameType,
        int playerCount,
        int boardSize
) {}
