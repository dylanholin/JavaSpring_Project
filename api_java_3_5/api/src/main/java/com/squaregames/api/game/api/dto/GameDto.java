package com.squaregames.api.game.api.dto;

import java.util.UUID;

public record GameDto(
        UUID id,
        String gameType,
        int playerCount,
        int boardSize,
        String status
) {}
