package com.squaregames.user.user.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO pour la création d'un utilisateur.
 */
public record UserCreationRequest(
    @NotBlank String name,
    @NotBlank @Email String email
) {}
