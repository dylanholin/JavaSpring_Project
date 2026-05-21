package com.squaregames.api.game;

public record MoveRequest(
        String tokenName,
        int row,
        int col
) {}
