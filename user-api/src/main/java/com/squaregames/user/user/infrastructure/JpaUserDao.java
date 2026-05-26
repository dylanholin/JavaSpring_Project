package com.squaregames.user.user.infrastructure;

import com.squaregames.user.user.application.UserDao;
import com.squaregames.user.user.domain.User;
import com.squaregames.user.user.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation JPA du DAO utilisateur.
 * S'appuie sur UserRepository (Spring Data) pour la persistance.
 */
@Repository
public class JpaUserDao implements UserDao {

    private final UserRepository repository;

    public JpaUserDao(UserRepository repository) {
        this.repository = repository;
    }

    @Override
    @SuppressWarnings("null")
    public User save(User user) {
        return repository.save(user);
    }

    @Override
    @SuppressWarnings("null")
    public Optional<User> findById(String id) {
        return repository.findById(id);
    }

    @Override
    public List<User> findAll() {
        return repository.findAll();
    }

    @Override
    @SuppressWarnings("null")
    public void deleteById(String id) {
        repository.deleteById(id);
    }

    @Override
    @SuppressWarnings("null")
    public boolean existsById(String id) {
        return repository.existsById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return repository.existsByEmail(email);
    }
}
