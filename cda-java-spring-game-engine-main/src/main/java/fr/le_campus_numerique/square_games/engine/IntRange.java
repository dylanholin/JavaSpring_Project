package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public record IntRange(@PositiveOrZero int min, @PositiveOrZero int max) {

    public IntRange(@Positive int value) {
        this(value, value);
    }

    public IntRange {
        if ((min < 0) || (max < 0) || (min > max))
            throw new IllegalArgumentException("max must be greater than or equal to min, and both must be greater than or equal to 0");
    }

    public boolean contains(int value) {
        return (this.min <= value) && (value <= this.max);
    }

    public @Positive int size() {
        return this.max - this.min + 1;
    }

    public @NotNull IntStream stream() {
        return IntStream.rangeClosed(this.min, this.max);
    }

    public static @NotNull IntPredicate within(int min, int max) {
        return (min > max) ? n -> false : n -> (min <= n) && (n <= max);
    }

}
