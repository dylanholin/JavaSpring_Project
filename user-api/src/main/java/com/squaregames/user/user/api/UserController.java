package com.squaregames.user.user.api;

import com.squaregames.user.user.api.dto.UserCreationRequest;
import com.squaregames.user.user.api.dto.UserDto;
import com.squaregames.user.user.application.UserService;
import com.squaregames.user.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(@Valid @RequestBody UserCreationRequest request) {
        User user = userService.createUser(request.name(), request.email(), request.password(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return userService.getUserById(id)
            .map(user -> ResponseEntity.ok(toDto(user)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserDto> getAllUsers() {
        return userService.getAllUsers().stream()
            .map(this::toDto)
            .toList();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/valid")
    public boolean isValidUser(@PathVariable String id) {
        return userService.isValidUser(id);
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getId(), user.getName(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
