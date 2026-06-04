package com.squaregames.user.user.application;

import com.squaregames.user.user.domain.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

/**
 * Implémentation du service de gestion des utilisateurs.
 */
@Service
public class UserServiceImpl implements UserService {

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserDao userDao, PasswordEncoder passwordEncoder) {
        this.userDao = userDao;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(String name, String email, String password, String role) {
        if (userDao.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already exists: " + email);
        }
        String hashedPassword = passwordEncoder.encode(password);
        User user = new User(name, email, hashedPassword, role);
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

    @Override
    public Optional<User> findByEmail(String email) {
        return userDao.findByEmail(email);
    }
}
