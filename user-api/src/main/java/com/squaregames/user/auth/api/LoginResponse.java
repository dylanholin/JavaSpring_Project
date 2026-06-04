package com.squaregames.user.auth.api;

public record LoginResponse(
    String token,
    String userId,
    String email,
    String role
) {}
