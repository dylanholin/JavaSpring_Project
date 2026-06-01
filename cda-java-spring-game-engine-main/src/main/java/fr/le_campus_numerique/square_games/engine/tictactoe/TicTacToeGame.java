package fr.le_campus_numerique.square_games.engine.tictactoe;

import fr.le_campus_numerique.square_games.engine.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TicTacToeGame implements Game {

    private final UUID id;
    private final int boardSize;
    private final UUID playerA;
    private final UUID playerB;

    private final Map<CellPosition, Token> board = new HashMap<>(9);

    private final Deque<TicTacToken> unplacedTokens = new ArrayDeque<>();

    private UUID winnerId;

    public TicTacToeGame(@Min(3) int boardSize, @NotNull UUID playerA, @NotNull UUID playerB) {
        Objects.requireNonNull(playerA);
        Objects.requireNonNull(playerB);
        if (playerA.equals(playerB)) {
            throw new IllegalArgumentException("Players A & B must be different");
        }
        this.id = UUID.randomUUID();
        this.boardSize = boardSize;
        this.playerA = playerA;
        this.playerB = playerB;
        this.createUnplacedTokens();
    }

    TicTacToeGame(
            UUID id,
            @Min(3) int boardSize,
            List<@NotNull UUID> playerIds,
            @NotNull Set<CellPosition> currentPlayerTokens,
            @NotNull Set<CellPosition> otherPlayerTokens) {
        assert boardSize >= 3;
        assert (playerIds == null) || ((playerIds.size() == 2) && playerIds.stream().allMatch(Objects::nonNull));
        assert (currentPlayerTokens != null) && (otherPlayerTokens != null);
        assert Math.abs(currentPlayerTokens.size() - otherPlayerTokens.size()) < 2;
        assert currentPlayerTokens.size() <= otherPlayerTokens.size();
        assert currentPlayerTokens.stream().noneMatch(otherPlayerTokens::contains);
        final var currentIsA = currentPlayerTokens.size() == otherPlayerTokens.size();
        this.id = (id == null) ? UUID.randomUUID() : id;
        this.boardSize = boardSize;
        this.playerA = (playerIds == null) ? UUID.randomUUID() : playerIds.get(currentIsA ? 0 : 1);
        this.playerB = (playerIds == null) ? UUID.randomUUID() : playerIds.get(currentIsA ? 1 : 0);
        currentPlayerTokens.forEach(p -> this.board.put(p, new TicTacToken(currentIsA ? this.playerA : this.playerB, p)));
        otherPlayerTokens.forEach(p -> this.board.put(p, new TicTacToken(currentIsA ? this.playerB : this.playerA, p)));
        this.createUnplacedTokens();
        this.updateWinnerId();
    }

    private void createUnplacedTokens() {
        IntStream.range(this.board.size(), this.boardSize * this.boardSize)
                .mapToObj(i -> (i % 2 == 0) ? this.playerA : this.playerB)
                .map(TicTacToken::new)
                .forEach(this.unplacedTokens::addLast);
    }

    @Override
    public UUID getId() {
        return this.id;
    }

    @Override
    public String getFactoryId() {
        return TicTacToeGameFactory.ID;
    }

    @Override
    public Set<UUID> getPlayerIds() {
        final var result = new LinkedHashSet<UUID>(2);
        final var current = Optional.ofNullable(this.winnerId)
                .map(id -> id.equals(this.playerA) ? this.playerB : this.playerA)
                .orElseGet(() -> Optional.ofNullable(this.unplacedTokens.peekFirst())
                        .flatMap(Token::getOwnerId)
                        .orElse(this.playerB));
        result.add(current);
        result.add(current.equals(this.playerA) ? this.playerB : this.playerA);
        return Collections.unmodifiableSet(result);
    }

    @Override
    public GameStatus getStatus() {
        return ((this.winnerId != null) || this.unplacedTokens.isEmpty()) ? GameStatus.TERMINATED : GameStatus.ONGOING;
    }

    @Override
    public UUID getCurrentPlayerId() {
        return (this.winnerId == null)
                ? Optional.ofNullable(this.unplacedTokens.peekFirst()).flatMap(Token::getOwnerId).orElse(null)
                : this.winnerId;
    }

    @Override
    public int getBoardSize() {
        return this.boardSize;
    }

    @Override
    public Map<CellPosition, Token> getBoard() {
        return Collections.unmodifiableMap(this.board);
    }

    @Override
    public Collection<Token> getRemainingTokens() {
        return Collections.unmodifiableCollection(this.unplacedTokens);
    }

    @Override
    public Collection<Token> getRemovedTokens() {
        return List.of();
    }


    boolean canMoveNow(TicTacToken token) {
        return (this.winnerId == null) && (token == unplacedTokens.peekFirst());
    }

    @NotNull
    Set<CellPosition> getEmptyCells() {
        return IntStream.range(0, this.boardSize * this.boardSize)
                .mapToObj(n -> new CellPosition(n / this.boardSize, n % this.boardSize))
                .filter(Predicate.not(board::containsKey))
                .collect(Collectors.toSet());
    }

    void moveTokenTo(@NotNull TicTacToken token, @NotNull CellPosition position) throws InvalidPositionException {
        assert (token != null) && (position != null);
        if (!this.canMoveNow(token))
            throw new InvalidPositionException("token cannot be moved now");
        if ((position.x() < 0) || (position.x() >= this.boardSize) || (position.y() < 0) || (position.y() >= this.boardSize))
            throw new InvalidPositionException("invalid position on board");
        if (this.board.containsKey(position))
            throw new InvalidPositionException("another token already at the specified position");
        final var first = this.unplacedTokens.pollFirst();
        assert first == token;
        this.board.put(position, token);
        this.updateWinnerId();
    }

    private void updateWinnerId() {
        assert this.winnerId == null;
        if (this.board.size() < 5)
            return;
        this.winnerId = Stream.concat(
                        Stream.concat(
                                IntStream.range(0, this.boardSize).mapToObj(this::checkRow),
                                IntStream.range(0, this.boardSize).mapToObj(this::checkColumn)),
                        Stream.of(
                                this.checkAllOwnedBySamePlayer(IntStream.range(0, this.boardSize).mapToObj(n -> new CellPosition(n, n))),
                                this.checkAllOwnedBySamePlayer(IntStream.range(0, this.boardSize).mapToObj(n -> new CellPosition(n, this.boardSize - 1 - n)))))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

    }

    private UUID checkRow(@Min(0) int n) {
        assert n >= 0 && n < this.boardSize;
        return this.checkAllOwnedBySamePlayer(IntStream.range(0, this.boardSize).mapToObj(x -> new CellPosition(x, n)));
    }

    private UUID checkColumn(@Min(0) int n) {
        assert n >= 0 && n < this.boardSize;
        return this.checkAllOwnedBySamePlayer(IntStream.range(0, this.boardSize).mapToObj(y -> new CellPosition(n, y)));
    }

    private UUID checkAllOwnedBySamePlayer(@NotNull Stream<CellPosition> positions) {
        assert positions != null;
        final var ids = new HashSet<UUID>(this.boardSize);
        positions.map(this.board::get)
                .map(token -> (token == null) ? null : token.getOwnerId().orElse(null))
                .forEach(ids::add);
        return ((ids.size() == 1) && !ids.contains(null)) ? ids.stream().findFirst().orElseThrow() : null;
    }


    private final class TicTacToken implements Token {

        private final UUID ownerId;
        private CellPosition position;

        private TicTacToken(@NotNull UUID ownerId) {
            this(ownerId, null);
        }

        private TicTacToken(@NotNull UUID ownerId, CellPosition position) {
            assert ownerId != null;
            this.ownerId = ownerId;
            this.position = position;
        }

        @Override
        public Optional<UUID> getOwnerId() {
            return Optional.of(this.ownerId);
        }

        @Override
        public String getName() {
            return this.ownerId.equals(playerA) ? "X" : "0";
        }

        @Override
        public CellPosition getPosition() {
            return this.position;
        }

        @Override
        public Set<CellPosition> getAllowedMoves() {
            return canMoveNow(this) ? getEmptyCells() : Set.of();
        }

        @Override
        public boolean canMove() {
            return canMoveNow(this);
        }

        @Override
        public void moveTo(@NotNull CellPosition position) throws InvalidPositionException {
            Objects.requireNonNull(position);
            moveTokenTo(this, position);
            this.position = position;
        }

        @Override
        public String toString() {
            return this.ownerId.equals(playerA) ? "X" : " O";
        }
    }

}
