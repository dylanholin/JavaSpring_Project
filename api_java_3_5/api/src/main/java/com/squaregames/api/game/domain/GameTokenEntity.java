package com.squaregames.api.game.domain;

import jakarta.persistence.*;

/**
 * Entité JPA représentant un token dans une partie.
 */
@Entity
@Table(name = "game_tokens")
public class GameTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    public GameEntity game;

    @Column(name = "token_name", nullable = false, length = 10)
    public String tokenName;

    @Column(name = "owner_id", length = 36)
    public String ownerId;

    @Column(name = "x_position")
    public Integer xPosition;

    @Column(name = "y_position")
    public Integer yPosition;

    @Column(name = "is_on_board")
    public boolean isOnBoard;

    @Column(name = "is_removed")
    public boolean isRemoved;
}
