package com.squaregames.user.user.application;

import com.squaregames.user.user.domain.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des utilisateurs.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    public UserServiceImpl(UserDao userDao) {
        this.userDao = userDao;
    }

    @Override
    public User createUser(String name, String email) {
        if (userDao.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }
        User user = new User(name, email);
        return userDao.save(user);
    }

    @Override
    public Optional<User> getUserById(String id) {
        return userDao.findById(id);
    }

    @Override
    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    @Override
    public void deleteUser(String id) {
        userDao.deleteById(id);
    }

    @Override
    public boolean isValidUser(String id) {
        return userDao.existsById(id);
    }
}
