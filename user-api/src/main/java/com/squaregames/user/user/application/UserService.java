package com.squaregames.user.user.application;

import com.squaregames.user.user.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Service de gestion des utilisateurs.
 */
public interface UserService {
    
    User createUser(String name, String email);
    
    Optional<User> getUserById(String id);
    
    List<User> getAllUsers();
    
    void deleteUser(String id);
    
    boolean isValidUser(String id);
}
