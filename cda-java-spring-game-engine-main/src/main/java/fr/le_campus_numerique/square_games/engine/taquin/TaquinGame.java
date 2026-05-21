package fr.le_campus_numerique.square_games.engine.taquin;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.*;

import java.util.*;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class TaquinGame implements Game {

    private static final Random RANDOM = new Random();

    private final @NotNull UUID id;
    private final @NotNull UUID playerId;
    private final @Min(3) int boardSize;
    private final @NotEmpty List<Tile> tiles;

    public TaquinGame(@Min(3) @Max(8) int boardSize) {
        this(boardSize, UUID.randomUUID());
    }

    public TaquinGame(@Min(3) @Max(8) int boardSize, @NotNull UUID playerId) {
        if ((boardSize < 3) || (boardSize > 8))
            throw new IllegalArgumentException("board size must be between 3 and 8");
        ;
        this.id = UUID.randomUUID();
        this.playerId = Objects.requireNonNull(playerId);
        this.boardSize = boardSize;
        this.tiles = new LinkedList<>();
        IntStream.range(1, this.boardSize * this.boardSize).mapToObj(Tile::new).forEach(this.tiles::add);
        this.tiles.add(null);
        while (this.getStatus() == GameStatus.TERMINATED) {
            this.shuffle(RANDOM.nextInt(10, 50));
        }
    }

    TaquinGame(UUID id, UUID playerId, @NotEmpty List<@NotNull CellPosition> tokenPositions) {
        assert (tokenPositions != null) && !tokenPositions.isEmpty();
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.playerId = (playerId == null) ? UUID.randomUUID() : playerId;
        this.boardSize = Math.toIntExact(Math.round(Math.sqrt(1 + tokenPositions.size())));
        assert 1 + tokenPositions.size() == this.boardSize * this.boardSize;
        this.tiles = new LinkedList<>();
        final var positionToToken = new HashMap<CellPosition, Tile>(this.boardSize * this.boardSize);
        final var validity = (IntPredicate) new IntRange(0, this.boardSize)::contains;
        IntStream.range(0, tokenPositions.size()).forEach(idx -> {
            final var p = tokenPositions.get(idx);
            assert validity.test(p.x()) && validity.test(p.y());
            positionToToken.put(p, new Tile(1 + idx));
        });
        assert positionToToken.size() == tokenPositions.size();
        IntStream.range(0, this.boardSize).forEach(x -> IntStream.range(0, this.boardSize)
                .mapToObj(y -> new CellPosition(x, y))
                .map(positionToToken::get)
                .forEach(this.tiles::add));
    }

    public void shuffle(@Positive int count) {
        if (count <= 0)
            throw new IllegalArgumentException("count must be greater than 0");
        var unoccupiedPosition = this.unoccupiedPosition();
        for (var i = count; i > 0; i--) {
            final var neighbors = this.neighborsOf(unoccupiedPosition);
            final var next = neighbors.get(RANDOM.nextInt(neighbors.size()));
            this.slideTile(next, unoccupiedPosition);
            unoccupiedPosition = next;
        }
        System.out.println("shuffled (" + count + " moves)");
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getFactoryId() {
        return TaquinGameFactory.ID;
    }

    @Override
    public Set<UUID> getPlayerIds() {
        return Set.of(this.playerId);
    }

    @Override
    public GameStatus getStatus() {
        return (this.tiles.get(this.tiles.size() - 1) == null) ? GameStatus.TERMINATED : GameStatus.ONGOING;
    }

    @Override
    public UUID getCurrentPlayerId() {
        return this.playerId;
    }

    @Override
    public int getBoardSize() {
        return this.boardSize;
    }

    @Override
    public Map<CellPosition, Token> getBoard() {
        final var result = new LinkedHashMap<CellPosition, Token>(this.tiles.size() - 1);
        for (var i = 0; i < this.tiles.size(); i++) {
            final var tile = this.tiles.get(i);
            if (tile != null)
                result.put(this.indexToPosition(i), tile);
        }
        return Collections.unmodifiableMap(result);
    }

    @Override
    public Collection<Token> getRemainingTokens() {
        return Set.of();
    }

    @Override
    public Collection<Token> getRemovedTokens() {
        return Set.of();
    }

    private void slideTile(@NotNull CellPosition currentPosition, @NotNull CellPosition destination) {
        assert (currentPosition != null) && (destination != null);
        final var currentIndex = this.indexOf(currentPosition);
        final var unoccupiedIndex = this.indexOf(destination);
        assert (currentIndex >= 0) && (currentIndex < this.tiles.size()) && (this.tiles.get(currentIndex) != null);
        assert (unoccupiedIndex >= 0) && (unoccupiedIndex < this.tiles.size()) && (this.tiles.get(unoccupiedIndex) == null);
        this.tiles.set(unoccupiedIndex, this.tiles.get(currentIndex));
        this.tiles.set(currentIndex, null);
    }

    private void moveTileTo(@NotNull Tile tile, @NotNull CellPosition position) throws InvalidPositionException {
        assert (tile != null) && (position != null);
        final var currentPosition = this.positionOf(tile.value);
        assert currentPosition != null;
        if ((position.x() < 0) || (position.y() < 0) || (position.x() >= this.boardSize) || (position.y() >= this.boardSize))
            throw new InvalidPositionException("invalid position");
        if (!areNeighbors(currentPosition, position))
            throw new InvalidPositionException("invalid position for token");
        final var idx = this.indexOf(position);
        assert (idx >= 0) && (idx < this.tiles.size());
        if (this.tiles.get(idx) != null)
            throw new InvalidPositionException("destination position is not available");
        this.slideTile(currentPosition, position);
    }

    private int indexOf(@NotNull CellPosition position) {
        assert position != null;
        return position.y() * this.boardSize + position.x();
    }

    private int indexOf(int value) {
        for (var i = 0; i < this.tiles.size(); i++) {
            if (Optional.ofNullable(this.tiles.get(i)).filter(t -> t.value == value).isPresent())
                return i;
        }
        return -1;
    }

    private @NotNull CellPosition unoccupiedPosition() {
        var unoccupiedIndex = this.tiles.lastIndexOf(null);
        assert unoccupiedIndex >= 0;
        return this.indexToPosition(unoccupiedIndex);
    }

    private CellPosition positionOf(int value) {
        final var idx = this.indexOf(value);
        return (idx < 0) ? null : this.indexToPosition(idx);
    }

    private CellPosition indexToPosition(@PositiveOrZero int index) {
        assert index >= 0;
        return new CellPosition(index % this.boardSize, index / this.boardSize);
    }

    private @NotEmpty List<CellPosition> neighborsOf(@NotNull CellPosition position) {
        assert position != null;
        final var result = new ArrayList<CellPosition>(4);
        if (position.x() > 0)
            result.add(new CellPosition(position.x() - 1, position.y()));
        if (position.y() > 0)
            result.add(new CellPosition(position.x(), position.y() - 1));
        if (position.x() < this.boardSize - 1)
            result.add(new CellPosition(position.x() + 1, position.y()));
        if (position.y() < this.boardSize - 1)
            result.add(new CellPosition(position.x(), position.y() + 1));
        return Collections.unmodifiableList(result);
    }

    private static boolean areNeighbors(@NotNull CellPosition a, @NotNull CellPosition b) {
        assert (a != null) && (b != null);
        return (Math.abs(a.x() - b.x()) + Math.abs(a.y()) - b.y()) == 1;
    }

    private final class Tile implements Token {

        private final int value;

        private Tile(@Positive int value) {
            assert (value > 0) && (value < (boardSize * boardSize));
            this.value = value;
        }

        @Override
        public Optional<UUID> getOwnerId() {
            return Optional.of(playerId);
        }

        @Override
        public String getName() {
            return String.valueOf(this.value);
        }

        @Override
        public CellPosition getPosition() {
            return positionOf(this.value);
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            final var current = positionOf(this.value);
            assert current != null;
            final var unoccupied = unoccupiedPosition();
            return areNeighbors(current, unoccupied) ? Set.of(unoccupied) : Set.of();
        }

        @Override
        public void moveTo(@NotNull CellPosition position) throws InvalidPositionException {
            Objects.requireNonNull(position);
            moveTileTo(this, position);
        }
    }

}
