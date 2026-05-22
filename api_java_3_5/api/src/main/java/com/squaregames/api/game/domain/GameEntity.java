package com.squaregames.api.game.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entité JPA représentant une partie en cours.
 * Stocke les métadonnées du jeu pour la persistance (JPA).
 */
@Entity
@Table(name = "games")
public class GameEntity {

    @Id
    @Column(length = 36)
    public String id;

    @Column(name = "factory_id", nullable = false, length = 50)
    public String factoryId;

    @Column(name = "board_size", nullable = false)
    public int boardSize;

    @Column(name = "player_count", nullable = false)
    public int playerCount;

    @Column(nullable = false, length = 20)
    public String status;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "updated_at")
    public Instant updatedAt;

    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    public List<GameTokenEntity> tokens = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
