package com.squaregames.api.game.infrastructure;

import com.squaregames.api.game.application.GameDao;
import fr.le_campus_numerique.square_games.engine.Game;
import fr.le_campus_numerique.square_games.engine.GameFactory;
import fr.le_campus_numerique.square_games.engine.tictactoe.TicTacToeGameFactory;
import fr.le_campus_numerique.square_games.engine.connectfour.ConnectFourGameFactory;
import fr.le_campus_numerique.square_games.engine.taquin.TaquinGameFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Implémentation JDBC du DAO avec SQL explicite.
 * Utilise NamedParameterJdbcTemplate pour exécuter des requêtes SQL.
 *
 * ⚠️ Limitation : le moteur de jeu ne permet pas de reconstruire un Game
 * depuis une base de données relationnelle. Cette implémentation stocke
 * les métadonnées mais ne peut pas restaurer l'état complet d'une partie.
 * Pour une vraie persistance, utiliser JPA ou sérialisation JSON.
 */
@Repository
public class JdbcGameDao implements GameDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    // Factories pour recréer des jeux (limitation : reset l'état)
    private final Map<String, GameFactory> factories = new HashMap<>();

    public JdbcGameDao(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        // Initialiser les factories
        factories.put("tictactoe", new TicTacToeGameFactory());
        factories.put("connectfour", new ConnectFourGameFactory());
        factories.put("taquin", new TaquinGameFactory());
    }

    @Override
    public Collection<Game> findAll() {
        String sql = "SELECT id, factory_id, board_size, player_count, status FROM games";
        return jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToGame(rs));
    }

    @Override
    public Optional<Game> findById(UUID gameId) {
        String sql = "SELECT id, factory_id, board_size, player_count, status FROM games WHERE id = :id";
        SqlParameterSource params = new MapSqlParameterSource("id", gameId.toString());

        try {
            Game game = jdbcTemplate.queryForObject(sql, params, (rs, rowNum) -> mapRowToGame(rs));
            return Optional.ofNullable(game);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Game upsert(Game game) {
        // Vérifie si le jeu existe déjà
        String selectSql = "SELECT COUNT(*) FROM games WHERE id = :id";
        SqlParameterSource checkParams = new MapSqlParameterSource("id", game.getId().toString());
        Integer count = jdbcTemplate.queryForObject(selectSql, checkParams, Integer.class);

        if (count != null && count > 0) {
            // Update
            String updateSql = "UPDATE games SET factory_id = :factoryId, board_size = :boardSize, " +
                    "player_count = :playerCount, status = :status, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE id = :id";
            SqlParameterSource updateParams = new MapSqlParameterSource()
                    .addValue("id", game.getId().toString())
                    .addValue("factoryId", game.getFactoryId())
                    .addValue("boardSize", game.getBoardSize())
                    .addValue("playerCount", game.getPlayerIds().size())
                    .addValue("status", game.getStatus().name());
            jdbcTemplate.update(updateSql, updateParams);
        } else {
            // Insert
            String insertSql = "INSERT INTO games (id, factory_id, board_size, player_count, status) " +
                    "VALUES (:id, :factoryId, :boardSize, :playerCount, :status)";
            SqlParameterSource insertParams = new MapSqlParameterSource()
                    .addValue("id", game.getId().toString())
                    .addValue("factoryId", game.getFactoryId())
                    .addValue("boardSize", game.getBoardSize())
                    .addValue("playerCount", game.getPlayerIds().size())
                    .addValue("status", game.getStatus().name());
            jdbcTemplate.update(insertSql, insertParams);
        }

        return game;
    }

    @Override
    public void delete(UUID gameId) {
        // Supprime d'abord les tokens associés (cascade gérée par FK, mais pour être sûr)
        String deleteTokensSql = "DELETE FROM game_tokens WHERE game_id = :gameId";
        jdbcTemplate.update(deleteTokensSql, new MapSqlParameterSource("gameId", gameId.toString()));

        // Supprime le jeu
        String deleteSql = "DELETE FROM games WHERE id = :id";
        jdbcTemplate.update(deleteSql, new MapSqlParameterSource("id", gameId.toString()));
    }

    /**
     * Mappe une ligne ResultSet vers un objet Game.
     * ⚠️ Crée un NOUVEAU jeu avec la factory — l'état précédent est perdu.
     */
    private Game mapRowToGame(ResultSet rs) throws SQLException {
        String factoryId = rs.getString("factory_id");
        int boardSize = rs.getInt("board_size");
        int playerCount = rs.getInt("player_count");

        GameFactory factory = factories.get(factoryId);
        if (factory == null) {
            throw new IllegalStateException("Factory inconnue : " + factoryId);
        }

        // Crée un nouveau jeu (l'état précédent est perdu — limitation du moteur)
        return factory.createGame(playerCount, boardSize);
    }
}
