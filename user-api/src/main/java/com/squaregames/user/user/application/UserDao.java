package com.squaregames.user.user.application;

import com.squaregames.user.user.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Interface DAO pour la gestion des utilisateurs.
 * Sépare la logique métier de la technologie de persistance.
 */
public interface UserDao {

    User save(User user);

    Optional<User> findById(String id);

    List<User> findAll();

    void deleteById(String id);

    boolean existsById(String id);

    boolean existsByEmail(String email);
}
