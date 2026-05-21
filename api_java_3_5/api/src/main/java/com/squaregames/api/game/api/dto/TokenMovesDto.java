package com.squaregames.api.game.api.dto;

import java.util.List;

public record TokenMovesDto(
        String tokenName,
        int row,
        int col,
        List<PositionDto> allowedMoves
) {}
