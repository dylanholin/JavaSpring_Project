package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public record TokenPosition<E>(E owner, @NotNull String tokenName, int x, int y) {

    public TokenPosition {
        Objects.requireNonNull(tokenName);
    }

}
