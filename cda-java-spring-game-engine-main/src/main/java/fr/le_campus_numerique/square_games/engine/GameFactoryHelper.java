package fr.le_campus_numerique.square_games.engine;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.function.IntPredicate;
import java.util.function.Predicate;

public final class GameFactoryHelper {

    private GameFactoryHelper() {
    }

    public static <K> @NotEmpty Map<K, Set<CellPosition>> groupBoardTokensByPlayer(
            @NotNull Collection<TokenPosition<K>> boardTokens,
            @NotEmpty List<K> players,
            @NotNull Predicate<CellPosition> positionChecker) throws InconsistentGameDefinitionException {
        Objects.requireNonNull(boardTokens);
        Objects.requireNonNull(players);
        Objects.requireNonNull(positionChecker);
        final var result = new LinkedHashMap<K, Set<CellPosition>>();
        players.forEach(k -> result.put(k, new LinkedHashSet<>()));
        final var occupied = new HashSet<CellPosition>();
        for (var t : boardTokens) {
            final var p = new CellPosition(t.x(), t.y());
            if (!positionChecker.test(p))
                throw new InconsistentGameDefinitionException("invalid coordinates for element in boardTokens | " + t);
            if (!occupied.add(p))
                throw new InconsistentGameDefinitionException("positions occupied by several elements in boardTokens | " + p);
            Optional.ofNullable(result.get(t.owner()))
                    .orElseThrow(() -> new InconsistentGameDefinitionException("invalid owner for element in boardTokens | " + t))
                    .add(p);
        }
        result.replaceAll((k, v) -> Collections.unmodifiableSet(v));
        return Collections.unmodifiableMap(result);
    }


    public static @NotNull Predicate<CellPosition> positionChecker(@NotNull IntPredicate x, @NotNull IntPredicate y) {
        Objects.requireNonNull(x);
        Objects.requireNonNull(y);
        return p -> x.test(p.x()) && y.test(p.y());
    }

}
