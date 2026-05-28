package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.domain.GameEntity;
import com.squaregames.api.game.domain.GameEntityRepository;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests d'intégration JPA pour JpaGameDao.
 * Utilise @DataJpaTest : démarre uniquement la couche JPA avec H2 en mémoire.
 * Vérifie que JpaGameDao persiste correctement les données et filtre par joueur.
 */
@DataJpaTest
class JpaGameDaoTest {

    @Autowired
    private GameEntityRepository repository;

    private JpaGameDao jpaGameDao;

    private static final UUID PLAYER_A = UUID.fromString("aaaaaaaa-0000-0000-0000-000000000001");
    private static final UUID PLAYER_B = UUID.fromString("bbbbbbbb-0000-0000-0000-000000000002");

    @BeforeEach
    void setUp() {
        jpaGameDao = new JpaGameDao(repository);
        repository.deleteAll();
    }

    @Test
    @SuppressWarnings("null")
    void shouldPersistAndRetrieveGameById() {
        // Given
        Game game = createTicTacToeGame(PLAYER_A, PLAYER_B);

        // When
        jpaGameDao.upsert(game);
        Optional<Game> found = jpaGameDao.findById(game.getId());

        // Then
        assertThat(found)
                .as("La partie persistée doit être retrouvable par son id")
                .isPresent();
        // Note : le moteur recrée un jeu vierge à la lecture (limitation connue).
        // On vérifie le type de jeu via l'entité en base, pas via l'objet Game retourné.
        String gameIdStr = game.getId().toString();
        Optional<GameEntity> entity = repository.findById(gameIdStr);
        assertThat(entity).as("L'entité doit exister en base").isPresent();
        assertThat(entity.get().id)
                .as("L'id persisté en base doit correspondre à l'id original")
                .isEqualTo(game.getId().toString());
        assertThat(entity.get().factoryId)
                .as("Le type de jeu doit être conservé en base")
                .isEqualTo(game.getFactoryId());
    }

    @Test
    void shouldReturnEmptyWhenGameNotFound() {
        // Given
        UUID unknownId = UUID.randomUUID();

        // When
        Optional<Game> found = jpaGameDao.findById(unknownId);

        // Then
        assertThat(found)
                .as("Un id inconnu doit retourner Optional.empty()")
                .isEmpty();
    }

    @Test
    void shouldFindByPlayerIdReturnsOnlyGamesOfThatPlayer() {
        // Given : deux parties — une avec PLAYER_A, une avec PLAYER_B seul
        Game gameA = createTicTacToeGame(PLAYER_A, PLAYER_B);
        Game gameB = createTicTacToeGame(PLAYER_B, UUID.randomUUID());
        jpaGameDao.upsert(gameA);
        jpaGameDao.upsert(gameB);

        // When
        Collection<Game> gamesOfPlayerA = jpaGameDao.findByPlayerId(PLAYER_A.toString());

        // Then
        // Note : le moteur recrée un jeu vierge à la lecture — on vérifie le count.
        // La vérification de l'id exact se fait via shouldPersistPlayerIdsInDatabase.
        assertThat(gamesOfPlayerA)
                .as("findByPlayerId doit retourner uniquement la 1 partie de PLAYER_A (pas celle de PLAYER_B seul)")
                .hasSize(1);
    }

    @Test
    void shouldFindByPlayerIdReturnsEmptyWhenNoMatch() {
        // Given : une partie sans PLAYER_A
        Game game = createTicTacToeGame(PLAYER_B, UUID.randomUUID());
        jpaGameDao.upsert(game);

        // When
        Collection<Game> result = jpaGameDao.findByPlayerId(PLAYER_A.toString());

        // Then
        assertThat(result)
                .as("findByPlayerId doit retourner une liste vide si le joueur n'a aucune partie")
                .isEmpty();
    }

    @Test
    void shouldFindByPlayerIdReturnsBothGamesWhenPlayerParticipatesInMultiple() {
        // Given : PLAYER_A participe à deux parties différentes
        Game game1 = createTicTacToeGame(PLAYER_A, UUID.randomUUID());
        Game game2 = createTicTacToeGame(UUID.randomUUID(), PLAYER_A);
        Game gameOther = createTicTacToeGame(PLAYER_B, UUID.randomUUID());
        jpaGameDao.upsert(game1);
        jpaGameDao.upsert(game2);
        jpaGameDao.upsert(gameOther);

        // When
        Collection<Game> result = jpaGameDao.findByPlayerId(PLAYER_A.toString());

        // Then
        // Note : le moteur recrée un jeu vierge à la lecture — on vérifie le count.
        assertThat(result)
                .as("PLAYER_A participe à 2 parties sur 3 : les deux doivent être retournées, pas celle de PLAYER_B")
                .hasSize(2);
    }

    @Test
    void shouldDeleteGame() {
        // Given
        Game game = createTicTacToeGame(PLAYER_A, PLAYER_B);
        jpaGameDao.upsert(game);
        assertThat(jpaGameDao.findById(game.getId())).as("Précondition : la partie existe").isPresent();

        // When
        jpaGameDao.delete(game.getId());

        // Then
        assertThat(jpaGameDao.findById(game.getId()))
                .as("Après suppression, la partie ne doit plus être trouvée")
                .isEmpty();
    }

    @Test
    @SuppressWarnings("null")
    void shouldPersistPlayerIdsInDatabase() {
        // Given
        Game game = createTicTacToeGame(PLAYER_A, PLAYER_B);

        // When
        jpaGameDao.upsert(game);

        // Then — vérification directe en base via le repository
        String gameIdStr = game.getId().toString();
        Optional<GameEntity> entity = repository.findById(gameIdStr);
        assertThat(entity).as("L'entité doit exister en base").isPresent();
        String persistedPlayerIds = entity.get().playerIds;
        assertThat(persistedPlayerIds)
                .as("playerIds doit être persisté en base (format CSV des UUIDs)")
                .isNotNull()
                .contains(PLAYER_A.toString())
                .contains(PLAYER_B.toString());
    }

    /** Crée une partie TicTacToe avec les joueurs donnés via la vraie factory du moteur. */
    private Game createTicTacToeGame(UUID... playerIds) {
        Set<UUID> players = new LinkedHashSet<>();
        for (UUID id : playerIds) {
            players.add(id);
        }
        return new TicTacToeGameFactory().createGame(3, players);
    }
}
