package com.squaregames.api.game.api.dto;

public record MoveRequest(
        String tokenName,
        int row,
        int col
) {}
