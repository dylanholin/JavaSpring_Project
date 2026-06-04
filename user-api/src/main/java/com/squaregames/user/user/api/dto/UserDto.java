package com.squaregames.user.user.api.dto;

import java.time.Instant;

/**
 * DTO pour représenter un utilisateur en réponse.
 */
public record UserDto(
    String id,
    String name,
    String email,
    String role,
    Instant createdAt
) {}
