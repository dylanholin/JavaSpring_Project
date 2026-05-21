package fr.le_campus_numerique.square_games.engine.connectfour;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConnectFourGame implements Game {

    static final int COLUMN_COUNT = 7;
    static final int ROW_COUNT = COLUMN_COUNT - 1;
    static final int WIN_GOAL = 4;

    private static final List<Reserved> RESERVED = IntStream.range(0, COLUMN_COUNT)
            .mapToObj(Reserved::new)
            .toList();

    private final UUID id;
    private final UUID redPlayerId;
    private final UUID yellowPlayerId;

    private final List<List<Token>> board = IntStream.range(0, COLUMN_COUNT)
            .mapToObj(ConnectFourGame::initColumn)
            .toList();
    private final Set<PlayerToken> remainingTokens = new LinkedHashSet<>(COLUMN_COUNT * ROW_COUNT);

    private boolean isYellowTurn;
    private List<Token> winningLine;
    private Set<CellPosition> availablePositions;

    public ConnectFourGame(@NotNull UUID redPlayerId, @NotNull UUID yellowPlayerId) {
        Objects.requireNonNull(redPlayerId);
        Objects.requireNonNull(yellowPlayerId);
        if (redPlayerId.equals(yellowPlayerId)) {
            throw new IllegalArgumentException("Player ids must be different");
        }
        this.id = UUID.randomUUID();
        this.redPlayerId = redPlayerId;
        this.yellowPlayerId = yellowPlayerId;
        this.isYellowTurn = false;
        this.createRemainingTokens(0);
    }

    ConnectFourGame(
            UUID id,
            List<@NotNull UUID> playerIds,
            @NotNull List<@NotNull List<Integer>> tokenOwnerIndexesByColumn) {
        assert tokenOwnerIndexesByColumn != null;
        assert tokenOwnerIndexesByColumn.stream().allMatch(l -> (l != null) && (l.size() <= ROW_COUNT));
        final var counts = countByOwnerIndexes(tokenOwnerIndexesByColumn);
        assert (counts != null) && (counts.length == 2) && IntRange.within(0, 1).test(counts[1] - counts[0]);
        assert (playerIds == null) || ((playerIds.size() == 2) || playerIds.stream().allMatch(Objects::nonNull));
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.isYellowTurn = counts[0] != counts[1];
        this.redPlayerId = (playerIds == null) ? UUID.randomUUID() : playerIds.get(this.isYellowTurn ? 1 : 0);
        this.yellowPlayerId = (playerIds == null) ? UUID.randomUUID() : playerIds.get(this.isYellowTurn ? 0 : 1);
        IntStream.range(0, tokenOwnerIndexesByColumn.size()).forEach(columnIndex -> {
            final var tokens = board.get(columnIndex);
            tokenOwnerIndexesByColumn.get(columnIndex).stream()
                    .map(ownerIndex -> (ownerIndex == 0) ? this.redPlayerId : this.yellowPlayerId)
                    .map(PlayerToken::new)
                    .forEach(t -> {
                        t.position = new CellPosition(columnIndex, tokens.size());
                        tokens.add(t);
                    });
        });
        this.createRemainingTokens(Arrays.stream(counts).sum());
    }

    private void createRemainingTokens(@PositiveOrZero int boardTokenCount) {
        assert boardTokenCount >= 0;
        final var cellCount = COLUMN_COUNT * ROW_COUNT;
        IntStream.range(boardTokenCount, cellCount)
                .mapToObj(n -> ((n % 2) == 0) ? this.redPlayerId : this.yellowPlayerId)
                .map(PlayerToken::new)
                .forEach(this.remainingTokens::add);
    }

    private static int[] countByOwnerIndexes(@NotNull List<@NotNull List<Integer>> tokenOwnerIndexesByColumn) {
        // we currently support 2 players only (add third item for 'out of bound' indexes - it should be 0)
        final var totalByPlayer = new int[3];
        tokenOwnerIndexesByColumn.stream()
                .flatMap(List::stream)
                .mapToInt(n -> (n < 0 || n > 1) ? 2 : n)
                .forEach(n -> totalByPlayer[n] += 1);
        return (totalByPlayer[2] == 0)
                ? new int[]{totalByPlayer[0], totalByPlayer[1]}
                : null;
    }


    public @Size(min = 4, max = 4) List<@NotNull Token> getWinningLine() {
        return this.winningLine;
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getFactoryId() {
        return ConnectFourGameFactory.ID;
    }

    @Override
    public Set<UUID> getPlayerIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(this.listOfPlayerIds()));
    }

    @Override
    public GameStatus getStatus() {
        return ((this.winningLine != null) || this.remainingTokens.isEmpty())
                ? GameStatus.TERMINATED
                : GameStatus.ONGOING;
    }

    @Override
    public UUID getCurrentPlayerId() {
        if (this.winningLine != null) {
            assert !this.winningLine.isEmpty();
            return this.winningLine.get(0).getOwnerId().orElseThrow();
        }
        return this.remainingTokens.isEmpty()
                ? null
                : this.isYellowTurn ? this.yellowPlayerId : this.redPlayerId;
    }

    @Override
    public int getBoardSize() {
        return this.board.size();
    }

    @Override
    public Map<CellPosition, Token> getBoard() {
        return this.board.stream().flatMap(Collection::stream)
                .filter(t -> t.getOwnerId().isPresent())
                .collect(Collectors.toUnmodifiableMap(Token::getPosition, Function.identity()));
    }

    @Override
    public Collection<Token> getRemainingTokens() {
        return Collections.unmodifiableSet(this.remainingTokens);
    }

    @Override
    public Collection<Token> getRemovedTokens() {
        return Set.of();
    }


    private @NotNull @Size(min = 2, max = 2) Collection<UUID> listOfPlayerIds() {
        return this.isYellowTurn
                ? List.of(this.yellowPlayerId, this.redPlayerId)
                : List.of(this.redPlayerId, this.yellowPlayerId);
    }

    private static @NotEmpty List<Token> initColumn(@PositiveOrZero int index) {
        final var result = new ArrayList<Token>();
        result.add(RESERVED.get(index));
        return result;
    }

    private List<Token> getColumn(int index) {
        return ((index >= 0) && (index < this.board.size())) ? this.board.get(index) : null;
    }

    private Token getTokenAt(@NotNull CellPosition position) {
        assert position != null;
        final var column = this.getColumn(position.x());
        return ((column != null) && (column.size() > position.y())) ? column.get(position.y()) : null;
    }

    private @NotNull CellPosition moveTokenTo(@NotNull PlayerToken token, @NotNull CellPosition position) throws InvalidPositionException {
        assert (token != null) && (position != null);
        assert this.winningLine == null;
        if (!this.remainingTokens.contains(token))
            throw new InvalidPositionException("token cannot be moved");
        final var column = this.getColumn(position.x());
        if ((position.y() >= 0) || (column == null) || (column.size() > ROW_COUNT))
            throw new InvalidPositionException("invalid position for token");
        this.remainingTokens.remove(token);
        final var actualPosition = new CellPosition(position.x(), column.size() - 1);
        column.add(token);
        this.availablePositions = null;
        this.updateWinningLine();
        this.isYellowTurn = !this.isYellowTurn;
        return actualPosition;
    }

    private void updateWinningLine() {
        assert this.winningLine == null;
        this.winningLine = Stream.concat(
                        Stream.concat(
                                IntStream.range(0, this.board.size()).mapToObj(this::checkColumnForWinGoal),
                                IntStream.range(1, ROW_COUNT + 1).mapToObj(this::checkRowForWinGoal)),
                        Stream.concat(
                                this.diagonalIndexes().mapToObj(this::checkUpwardDiagonalForWinGoal),
                                this.diagonalIndexes().mapToObj(this::checkDownwardDiagonalForWinGoal)))
                .filter(Objects::nonNull)
                .findFirst().orElse(null);
        assert (this.winningLine == null) || (this.winningLine.size() == WIN_GOAL);
    }

    private @NotNull IntStream diagonalIndexes() {
        return IntStream.range(WIN_GOAL - ROW_COUNT, this.board.size() - WIN_GOAL);
    }

    private List<Token> checkColumnForWinGoal(@PositiveOrZero int index) {
        assert index >= 0;
        return this.checkTokensForWinGoal(IntStream
                .range(1, ROW_COUNT + 1)
                .mapToObj(n -> new CellPosition(index, n))
                .map(this::getTokenAt)
                .takeWhile(Objects::nonNull));
    }

    private List<Token> checkRowForWinGoal(@PositiveOrZero int index) {
        assert index >= 0;
        return this.checkPositionsForWinGoal(IntStream
                .range(0, this.board.size())
                .mapToObj(n -> new CellPosition(n, index)));
    }

    private List<Token> checkUpwardDiagonalForWinGoal(int index) {
        return this.checkPositionsForWinGoal(IntStream
                .range(0, Math.min(COLUMN_COUNT, ROW_COUNT + 1))
                .mapToObj(n -> new CellPosition(n + index, n))
                .dropWhile(p -> p.x() < 0));
    }

    private List<Token> checkDownwardDiagonalForWinGoal(int index) {
        return this.checkPositionsForWinGoal(IntStream
                .range(0, Math.min(COLUMN_COUNT, ROW_COUNT + 1))
                .mapToObj(n -> new CellPosition(n + index, ROW_COUNT + 1 - n))
                .dropWhile(p -> p.x() < 0));
    }

    private List<Token> checkPositionsForWinGoal(@NotNull Stream<CellPosition> positions) {
        return this.checkTokensForWinGoal(positions.map(this::getTokenAt));
    }

    private List<Token> checkTokensForWinGoal(@NotNull Stream<Token> tokens) {
        final var counter = new WinningLineCounter();
        final var found = tokens
                .dropWhile(Predicate.not(counter::add))
                .findFirst();
        return found.isPresent() ? counter.line : null;
    }

    private @NotNull Set<CellPosition> getAvailablePositions() {
        if (this.winningLine != null)
            return Set.of();
        if (this.availablePositions == null) {
            this.availablePositions = IntStream.range(0, this.board.size())
                    .filter(i -> this.board.get(i).size() <= ROW_COUNT)
                    .mapToObj(i -> new CellPosition(i, -1))
                    .collect(Collectors.toSet());
        }
        return this.availablePositions;
    }

    private static final class WinningLineCounter {

        private final List<Token> line = new ArrayList<>(WIN_GOAL);
        private UUID currentId;

        public boolean add(Token token) {
            final var ownerId = Optional.ofNullable(token).flatMap(Token::getOwnerId).orElse(null);
            if ((ownerId == null) || !ownerId.equals(this.currentId)) {
                this.line.clear();
                this.currentId = ownerId;
            }
            this.line.add(token);
            return this.line.size() >= WIN_GOAL;
        }

    }

    private final class PlayerToken implements Token {

        private final @NotNull UUID ownerId;
        private CellPosition position;

        PlayerToken(@NotNull UUID ownerId) {
            assert ownerId != null;
            this.ownerId = ownerId;
        }

        @Override
        public Optional<UUID> getOwnerId() {
            return Optional.of(this.ownerId);
        }

        @Override
        public String getName() {
            return this.ownerId.equals(redPlayerId) ? "R" : "Y";
        }

        @Override
        public CellPosition getPosition() {
            return this.position;
        }

        @Override
        public boolean canMove() {
            return (this.position == null) && (winningLine == null) && this.ownerId.equals(getCurrentPlayerId());
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            return ((this.position == null) && this.ownerId.equals(getCurrentPlayerId()))
                    ? getAvailablePositions()
                    : Set.of();
        }

        @Override
        public void moveTo(CellPosition position) throws InvalidPositionException {
            Objects.requireNonNull(position);
            this.position = moveTokenTo(this, position);
        }

        @Override
        public String toString() {
            return "[ " + this.getName() + " ]";
        }

    }

    private record Reserved(@PositiveOrZero int column) implements Token {

        Reserved {
            assert (column >= 0) && (column <= COLUMN_COUNT);
        }

        @Override
        public Optional<UUID> getOwnerId() {
            return Optional.empty();
        }

        @Override
        public String getName() {
            return "-";
        }

        @Override
        public CellPosition getPosition() {
            return new CellPosition(this.column, -1);
        }

        @Override
        public boolean canMove() {
            return false;
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            return Set.of();
        }

        @Override
        public void moveTo(@NotNull CellPosition position) throws InvalidPositionException {
            Objects.requireNonNull(position);
            throw new InvalidPositionException("token cannot move");
        }

        @Override
        public String toString() {
            return "| " + this.column + " |";
        }
    }

}
