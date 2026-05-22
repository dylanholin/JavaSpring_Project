package com.squaregames.api.game.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour les entités Game.
 * Fournit automatiquement les opérations CRUD de base.
 */
@Repository
public interface GameEntityRepository extends JpaRepository<GameEntity, String> {
}
